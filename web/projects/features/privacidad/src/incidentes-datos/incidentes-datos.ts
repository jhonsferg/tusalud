import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  ETIQUETA_ESTADO_INCIDENTE,
  ETIQUETA_SEVERIDAD_INCIDENTE,
  ETIQUETA_TIPO_INCIDENTE,
  EstadoIncidente,
  Incidente,
  NotificationService,
  ProteccionDatosApiService,
  SeveridadIncidente,
  TipoIncidente,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

const SEVERIDAD_TAG: Readonly<Record<SeveridadIncidente, 'success' | 'info' | 'warn' | 'danger'>> =
  {
    BAJA: 'success',
    MEDIA: 'info',
    ALTA: 'warn',
    CRITICA: 'danger',
  };

@Component({
  selector: 'app-incidentes-datos',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Checkbox,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Incidentes de datos personales"
      subtitle="Registro, contención y notificación dentro del plazo"
      icon="pi-exclamation-triangle"
    >
      <p-button label="Registrar incidente" icon="pi pi-plus" (onClick)="abrirNuevo()" />
    </app-page-header>

    <p-message severity="info" styleClass="w-full mb-3">
      El plazo para notificar a la autoridad se calcula desde la detección y está por confirmar con
      un asesor legal.
    </p-message>

    <section class="panel">
      <p-table
        [value]="incidentes()"
        [loading]="cargando()"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Incidente</th>
            <th>Severidad</th>
            <th>Estado</th>
            <th>Plazo para la autoridad</th>
            <th></th>
          </tr>
        </ng-template>
        <ng-template #body let-i>
          <tr>
            <td>
              <strong>{{ i.codigo }}</strong> · {{ tipos[i.tipo] }}
              <small class="block text-muted">{{ i.descripcion }}</small>
              <small class="block text-muted">{{ i.titularesAfectados }} titulares afectados</small>
            </td>
            <td>
              <p-tag [value]="severidades[i.severidad]" [severity]="tagSeveridad[i.severidad]" />
            </td>
            <td>{{ estados[i.estado] }}</td>
            <td [class.vencido]="i.vencido">
              @if (i.autoridadNotificadaEn) {
                Notificada el {{ i.autoridadNotificadaEn | date: 'd MMM y, HH:mm' }}
              } @else if (i.estado === 'CERRADO') {
                —
              } @else if (i.vencido) {
                Vencido
                <small class="block">hace {{ -i.horasRestantes }} horas</small>
              } @else {
                {{ i.notificarAutoridadAntesDe | date: 'd MMM y, HH:mm' }}
                <small class="block text-muted">quedan {{ i.horasRestantes }} horas</small>
              }
            </td>
            <td class="derecha">
              @if (i.estado !== 'CERRADO') {
                <p-button
                  label="Actualizar"
                  size="small"
                  [outlined]="true"
                  [ariaLabel]="'Actualizar el incidente ' + i.codigo"
                  (onClick)="abrirEdicion(i)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="5">
              <app-empty-state icon="pi-shield" title="Sin incidentes registrados" />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    <p-dialog
      header="Registrar un incidente"
      [(visible)]="nuevo"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '38rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); registrar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="inc-tipo">Tipo</label>
        <p-select
          inputId="inc-tipo"
          [options]="opcionesTipo"
          optionLabel="label"
          optionValue="value"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="inc-sev">Severidad</label>
        <p-select
          inputId="inc-sev"
          [options]="opcionesSeveridad"
          optionLabel="label"
          optionValue="value"
          [ngModel]="severidad()"
          (ngModelChange)="severidad.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="inc-desc">Qué ocurrió</label>
        <textarea
          pTextarea
          id="inc-desc"
          rows="3"
          class="w-full"
          maxlength="1000"
          [ngModel]="descripcion()"
          (ngModelChange)="descripcion.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="inc-datos">Datos afectados</label>
        <input
          pInputText
          id="inc-datos"
          class="w-full"
          maxlength="400"
          [ngModel]="datos()"
          (ngModelChange)="datos.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
        <label for="inc-titulares">Titulares afectados</label>
        <p-inputnumber
          inputId="inc-titulares"
          [ngModel]="titulares()"
          (ngModelChange)="titulares.set($event ?? 0)"
          [ngModelOptions]="{ standalone: true }"
          [min]="0"
        />
        <label for="inc-cont">Acciones de contención ya tomadas</label>
        <textarea
          pTextarea
          id="inc-cont"
          rows="2"
          class="w-full"
          maxlength="1000"
          [ngModel]="contencion()"
          (ngModelChange)="contencion.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="nuevo.set(false)"
        />
        <p-button
          label="Registrar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="registrar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="editando() ? 'Incidente ' + editando()!.codigo : ''"
      [visible]="editando() !== null"
      (visibleChange)="$event ? null : editando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '38rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); actualizar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="act-estado">Estado</label>
        <p-select
          inputId="act-estado"
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estadoNuevo()"
          (ngModelChange)="estadoNuevo.set($event)"
          [ngModelOptions]="{ standalone: true }"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="act-cont">Contención</label>
        <textarea
          pTextarea
          id="act-cont"
          rows="2"
          class="w-full"
          maxlength="1000"
          [ngModel]="contencion()"
          (ngModelChange)="contencion.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <label for="act-causa">Causa raíz</label>
        <textarea
          pTextarea
          id="act-causa"
          rows="2"
          class="w-full"
          maxlength="1000"
          [ngModel]="causa()"
          (ngModelChange)="causa.set($event)"
          [ngModelOptions]="{ standalone: true }"
        ></textarea>
        <div class="check">
          <p-checkbox
            inputId="act-aut"
            [binary]="true"
            [ngModel]="autoridad()"
            (ngModelChange)="autoridad.set($event)"
            [ngModelOptions]="{ standalone: true }"
            [disabled]="!!editando()?.autoridadNotificadaEn"
          />
          <label for="act-aut">Se notificó a la autoridad</label>
        </div>
        <div class="check">
          <p-checkbox
            inputId="act-tit"
            [binary]="true"
            [ngModel]="titularesNotificados()"
            (ngModelChange)="titularesNotificados.set($event)"
            [ngModelOptions]="{ standalone: true }"
            [disabled]="!!editando()?.titularesNotificadosEn"
          />
          <label for="act-tit">Se notificó a los titulares afectados</label>
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="editando.set(null)"
        />
        <p-button
          label="Guardar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="actualizar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .derecha {
      text-align: right;
    }

    .vencido {
      color: var(--p-red-600);
      font-weight: 600;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
      margin-top: 0.3rem;
    }

    .check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.4rem;
    }

    .check label {
      margin: 0;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class IncidentesDatos implements OnInit {
  private readonly api = inject(ProteccionDatosApiService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_INCIDENTE;
  protected readonly severidades: Record<string, string> = ETIQUETA_SEVERIDAD_INCIDENTE;
  protected readonly estados: Record<string, string> = ETIQUETA_ESTADO_INCIDENTE;
  protected readonly tagSeveridad: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_TAG;
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_INCIDENTE).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly opcionesSeveridad = Object.entries(ETIQUETA_SEVERIDAD_INCIDENTE).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_INCIDENTE).map(
    ([value, label]) => ({ label, value }),
  );

  protected readonly incidentes = signal<Incidente[]>([]);
  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly nuevo = signal(false);
  protected readonly editando = signal<Incidente | null>(null);
  protected readonly tipo = signal<TipoIncidente>('ACCESO_NO_AUTORIZADO');
  protected readonly severidad = signal<SeveridadIncidente>('MEDIA');
  protected readonly descripcion = signal('');
  protected readonly datos = signal('');
  protected readonly titulares = signal(0);
  protected readonly contencion = signal('');
  protected readonly causa = signal('');
  protected readonly estadoNuevo = signal<EstadoIncidente>('EN_CONTENCION');
  protected readonly autoridad = signal(false);
  protected readonly titularesNotificados = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected abrirNuevo(): void {
    this.error.set(null);
    this.tipo.set('ACCESO_NO_AUTORIZADO');
    this.severidad.set('MEDIA');
    this.descripcion.set('');
    this.datos.set('');
    this.titulares.set(0);
    this.contencion.set('');
    this.nuevo.set(true);
  }

  protected registrar(): void {
    if (this.descripcion().trim().length < 10 || !this.datos().trim()) {
      this.error.set(
        'Describa lo ocurrido, con al menos 10 caracteres, e indique los datos afectados.',
      );
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    this.api
      .registrarIncidente({
        tipo: this.tipo(),
        severidad: this.severidad(),
        descripcion: this.descripcion().trim(),
        datosAfectados: this.datos().trim(),
        titularesAfectados: this.titulares(),
        detectadoEn: null,
        contencion: this.contencion().trim() || null,
      })
      .subscribe({
        next: (i) => {
          this.guardando.set(false);
          this.nuevo.set(false);
          this.notificaciones.success(`Se registró el incidente ${i.codigo}.`);
          this.cargar();
        },
        error: (e: { message: string }) => {
          this.guardando.set(false);
          this.error.set(e.message);
        },
      });
  }

  protected abrirEdicion(i: Incidente): void {
    this.error.set(null);
    this.estadoNuevo.set(i.estado === 'ABIERTO' ? 'EN_CONTENCION' : i.estado);
    this.contencion.set(i.contencion ?? '');
    this.causa.set(i.causaRaiz ?? '');
    this.autoridad.set(!!i.autoridadNotificadaEn);
    this.titularesNotificados.set(!!i.titularesNotificadosEn);
    this.editando.set(i);
  }

  protected actualizar(): void {
    const i = this.editando();
    if (!i) {
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    this.api
      .actualizarIncidente(i.id, {
        estado: this.estadoNuevo(),
        contencion: this.contencion().trim() || null,
        causaRaiz: this.causa().trim() || null,
        autoridadNotificada: this.autoridad(),
        titularesNotificados: this.titularesNotificados(),
        responsableId: null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.editando.set(null);
          this.notificaciones.success('El incidente se actualizó.');
          this.cargar();
        },
        error: (e: { message: string }) => {
          this.guardando.set(false);
          this.error.set(e.message);
        },
      });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.incidentes().subscribe({
      next: (lista) => {
        this.incidentes.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
