import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  CATEGORIAS_SERVICIO,
  CategoriaServicio,
  CoberturaPacienteResponse,
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_ESTADO_PREAUTORIZACION,
  NotificationService,
  PacienteMostradorResponse,
  SegurosApiService,
  SegurosStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PacienteBuscador } from '@tusalud/ui/paciente-buscador';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_PREAUTORIZACION } from '../seguros.utils';

@Component({
  selector: 'app-mis-preautorizaciones',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PacienteBuscador,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Preautorizaciones"
      subtitle="Solicita la autorización de la aseguradora antes de indicar estudios o internaciones"
      icon="pi-verified"
    >
      <p-button label="Nueva solicitud" icon="pi pi-plus" (onClick)="abrir()" />
    </app-page-header>

    <div class="panel">
      <p-table
        [value]="store.misPreautorizaciones.value() ?? []"
        [loading]="store.misPreautorizaciones.loading()"
        [paginator]="(store.misPreautorizaciones.value() ?? []).length > 10"
        [rows]="10"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Solicitud</th>
            <th>Paciente</th>
            <th>Aseguradora</th>
            <th>Servicio</th>
            <th class="text-right">Estimado</th>
            <th>Estado</th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <div class="font-semibold">{{ p.codigo }}</div>
              <small class="text-muted">{{ p.solicitadaEn | date: 'd MMM, HH:mm' }}</small>
            </td>
            <td>
              <span class="sensible">{{ p.pacienteNombre }}</span>
            </td>
            <td>{{ p.aseguradoraNombre }}</td>
            <td>
              {{ etiquetaCategoria[p.categoria]
              }}<small class="text-muted block">{{ p.descripcion }}</small>
            </td>
            <td class="text-right">{{ p.montoEstimado | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td>
              <p-tag [value]="etiquetaEstado[p.estado]" [severity]="severidad[p.estado]" />
              @if (p.codigoAutorizacion) {
                <small class="text-muted block"
                  >{{ p.codigoAutorizacion }}
                  @if (p.vigenteHasta) {
                    · hasta {{ p.vigenteHasta | date: 'd MMM' }}
                  }
                </small>
              }
              @if (p.motivoRechazo) {
                <small class="text-muted block">{{ p.motivoRechazo }}</small>
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-verified"
                title="Sin solicitudes"
                message="Tus solicitudes de preautorización aparecerán aquí."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <p-dialog
      header="Solicitar preautorización"
      [visible]="visible()"
      (visibleChange)="visible.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '36rem', maxWidth: '96vw' }"
    >
      <div class="form">
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label
          >Paciente
          <app-paciente-buscador
            inputId="pre-paciente"
            [paciente]="paciente()"
            (pacienteChange)="elegir($event)"
        /></label>
        @if (cobertura(); as c) {
          @if (c.tieneCobertura) {
            <p class="cobertura" id="pre-cobertura">
              <i class="pi pi-shield"></i>
              Cobertura de <strong>{{ c.aseguradora }}</strong
              >.
              @if (c.requierenPreautorizacion.length > 0) {
                Exige preautorización en {{ nombresExigidas() }}.
              } @else {
                No exige preautorización.
              }
            </p>
          } @else {
            <p class="sin-cobertura" id="pre-sin-cobertura">
              El paciente no tiene una póliza vigente.
            </p>
          }
        }
        <label>
          Servicio
          <p-select
            inputId="pre-categoria"
            [options]="categorias()"
            optionLabel="label"
            optionValue="value"
            [ngModel]="categoria()"
            (ngModelChange)="categoria.set($event)"
            appendTo="body"
            styleClass="w-full"
          />
        </label>
        <label
          >Descripción del estudio o procedimiento
          <input
            pInputText
            id="pre-descripcion"
            class="w-full"
            [ngModel]="descripcion()"
            (ngModelChange)="descripcion.set($event)"
            maxlength="200"
            autocomplete="off"
        /></label>
        <label>
          Monto estimado
          <p-inputnumber
            inputId="pre-estimado"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            [min]="0"
            [ngModel]="estimado()"
            (ngModelChange)="estimado.set($event ?? 0)"
            [fluid]="true"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="visible.set(false)"
        />
        <p-button
          label="Solicitar"
          icon="pi pi-send"
          [loading]="guardando()"
          (onClick)="solicitar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      gap: var(--sp-3);
    }
    .form label {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-weight: 600;
      font-size: 0.9rem;
    }
    .cobertura,
    .sin-cobertura {
      display: flex;
      align-items: center;
      gap: var(--sp-2);
      margin: 0;
      padding: var(--sp-2) var(--sp-3);
      border-radius: var(--radio-sm);
    }
    .cobertura {
      background: var(--estado-info-fondo);
    }
    .sin-cobertura {
      background: var(--estado-aviso-fondo);
      color: var(--estado-aviso);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisPreautorizaciones implements OnInit {
  protected readonly store = inject(SegurosStore);
  private readonly api = inject(SegurosApiService);
  private readonly notifications = inject(NotificationService);

  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly etiquetaEstado: Readonly<Record<string, string>> =
    ETIQUETA_ESTADO_PREAUTORIZACION;
  protected readonly severidad = SEVERIDAD_PREAUTORIZACION;

  protected readonly visible = signal(false);
  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly cobertura = signal<CoberturaPacienteResponse | null>(null);
  protected readonly categoria = signal<CategoriaServicio>('IMAGEN');
  protected readonly descripcion = signal('');
  protected readonly estimado = signal(0);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly categorias = computed(() => {
    const exigidas = this.cobertura()?.requierenPreautorizacion ?? [];
    const lista = exigidas.length > 0 ? exigidas : CATEGORIAS_SERVICIO;
    return lista.map((valor) => ({ label: ETIQUETA_CATEGORIA_SERVICIO[valor], value: valor }));
  });

  protected readonly nombresExigidas = computed(() =>
    (this.cobertura()?.requierenPreautorizacion ?? [])
      .map((c) => ETIQUETA_CATEGORIA_SERVICIO[c].toLowerCase())
      .join(', '),
  );

  ngOnInit(): void {
    this.store.cargarMisPreautorizaciones();
  }

  protected abrir(): void {
    this.paciente.set(null);
    this.cobertura.set(null);
    this.categoria.set('IMAGEN');
    this.descripcion.set('');
    this.estimado.set(0);
    this.error.set(null);
    this.visible.set(true);
  }

  protected elegir(paciente: PacienteMostradorResponse | null): void {
    this.paciente.set(paciente);
    this.cobertura.set(null);
    if (!paciente) {
      return;
    }
    this.api.coberturaDelPaciente(paciente.pacienteId).subscribe({
      next: (cobertura) => {
        this.cobertura.set(cobertura);
        if (cobertura.requierenPreautorizacion.length > 0) {
          this.categoria.set(cobertura.requierenPreautorizacion[0]);
        }
      },
      error: () => this.cobertura.set(null),
    });
  }

  protected solicitar(): void {
    const paciente = this.paciente();
    if (!paciente || !this.descripcion().trim()) {
      this.error.set('Elige al paciente y describe el estudio o procedimiento.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.store
      .solicitar({
        pacienteId: paciente.pacienteId,
        categoria: this.categoria(),
        descripcion: this.descripcion().trim(),
        montoEstimado: this.estimado(),
      })
      .subscribe({
        next: (solicitada) => {
          this.guardando.set(false);
          this.visible.set(false);
          this.notifications.success(`La solicitud ${solicitada.codigo} fue enviada.`);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
