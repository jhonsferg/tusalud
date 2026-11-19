import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  ConsentimientoResumen,
  DescargaService,
  DocumentosMedicosApiService,
  DocumentosMedicosStore,
  ETIQUETA_ESTADO_CONSENTIMIENTO,
  NotificationService,
  PacienteMostradorResponse,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PacienteBuscador } from '@tusalud/ui/paciente-buscador';
import { PageHeader } from '@tusalud/ui/page-header';
import { ConsentimientoFirma } from '../consentimiento-firma/consentimiento-firma';
import { SEVERIDAD_ESTADO_CONSENTIMIENTO, SeveridadEtiqueta } from '../documentos-medicos.utils';

@Component({
  selector: 'app-consentimientos-medico',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    Select,
    Tag,
    EmptyState,
    PacienteBuscador,
    PageHeader,
    ConsentimientoFirma,
  ],
  template: `
    <app-page-header
      title="Consentimientos informados"
      subtitle="Solicita el consentimiento de un procedimiento y registra la firma en consultorio"
      icon="pi-pencil"
    >
      <p-button label="Solicitar consentimiento" icon="pi pi-plus" (onClick)="abrirSolicitud()" />
    </app-page-header>

    <section class="panel">
      <label class="buscar" for="cons-paciente">Paciente</label>
      <app-paciente-buscador
        inputId="cons-paciente"
        [(paciente)]="paciente"
        (pacienteChange)="cargar()"
      />
    </section>

    @if (store.consentimientos.value(); as lista) {
      @if (lista.length === 0) {
        <div class="panel">
          <app-empty-state
            icon="pi-pencil"
            title="Sin consentimientos"
            message="Este paciente no tiene consentimientos registrados."
          />
        </div>
      } @else {
        <div class="grid">
          @for (c of lista; track c.id) {
            <article class="panel consentimiento">
              <header>
                <div>
                  <strong>{{ c.procedimiento }}</strong>
                  <small class="block text-muted">{{ c.plantillaNombre }}</small>
                </div>
                <p-tag [value]="etiqueta[c.estado]" [severity]="severidad[c.estado]" />
              </header>
              <p class="text-muted">
                Solicitado el {{ c.creadoEn | date: 'd MMM y' }}
                @if (c.firmadoEn) {
                  · firmado el {{ c.firmadoEn | date: 'd MMM y, HH:mm' }}
                }
              </p>
              <footer>
                @if (c.estado === 'PENDIENTE_FIRMA') {
                  <p-button
                    label="Firmar en consultorio"
                    icon="pi pi-pencil"
                    size="small"
                    (onClick)="firmando.set(c.id)"
                  />
                }
                @if (c.estado === 'FIRMADO') {
                  <p-button
                    label="Revocar"
                    severity="danger"
                    size="small"
                    [outlined]="true"
                    (onClick)="revocar(c)"
                  />
                }
                @if (c.estado !== 'PENDIENTE_FIRMA') {
                  <p-button
                    label="Descargar PDF"
                    icon="pi pi-download"
                    size="small"
                    severity="secondary"
                    [outlined]="true"
                    (onClick)="descargar(c)"
                  />
                }
              </footer>
            </article>
          }
        </div>
      }
    }

    <p-dialog
      header="Solicitar consentimiento"
      [(visible)]="solicitando"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      <form class="form" (submit)="$event.preventDefault(); solicitar()" novalidate>
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <label for="sol-paciente">Paciente</label>
        <app-paciente-buscador inputId="sol-paciente" [(paciente)]="pacienteSolicitud" />
        <label for="sol-plantilla">Plantilla</label>
        <p-select
          inputId="sol-plantilla"
          [options]="opcionesPlantilla()"
          optionLabel="label"
          optionValue="value"
          [ngModel]="plantillaId()"
          (ngModelChange)="plantillaId.set($event)"
          [ngModelOptions]="{ standalone: true }"
          placeholder="Selecciona la plantilla"
          appendTo="body"
          styleClass="w-full"
        />
        <label for="sol-procedimiento">Procedimiento</label>
        <input
          pInputText
          id="sol-procedimiento"
          class="w-full"
          maxlength="200"
          autocomplete="off"
          [ngModel]="procedimiento()"
          (ngModelChange)="procedimiento.set($event)"
          [ngModelOptions]="{ standalone: true }"
        />
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="solicitando.set(false)"
        />
        <p-button
          label="Solicitar"
          icon="pi pi-send"
          [loading]="enviando()"
          (onClick)="solicitar()"
        />
      </ng-template>
    </p-dialog>

    <app-consentimiento-firma
      [id]="firmando()"
      via="CONSULTORIO"
      [nombreInicial]="paciente()?.nombreCompleto ?? ''"
      [dniInicial]="paciente()?.dni ?? ''"
      (firmado)="alFirmar()"
      (cerrar)="firmando.set(null)"
    />
  `,
  styles: `
    .buscar {
      display: block;
      margin-bottom: 0.4rem;
      font-weight: 600;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(22rem, 1fr));
      gap: 1rem;
      margin-top: 1rem;
    }

    .consentimiento {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    .consentimiento header,
    .consentimiento footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .consentimiento p {
      margin: 0;
    }

    .form {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .form label {
      font-weight: 600;
      font-size: 0.9rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConsentimientosMedico implements OnInit {
  readonly pacienteId = input<string>();

  protected readonly store = inject(DocumentosMedicosStore);
  private readonly api = inject(DocumentosMedicosApiService);
  private readonly descargas = inject(DescargaService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly etiqueta: Record<string, string> = ETIQUETA_ESTADO_CONSENTIMIENTO;
  protected readonly severidad: Record<string, SeveridadEtiqueta> = SEVERIDAD_ESTADO_CONSENTIMIENTO;
  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly pacienteSolicitud = signal<PacienteMostradorResponse | null>(null);
  protected readonly solicitando = signal(false);
  protected readonly firmando = signal<string | null>(null);
  protected readonly plantillaId = signal<string | null>(null);
  protected readonly procedimiento = signal('');
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly opcionesPlantilla = computed(() =>
    (this.store.plantillas.value() ?? [])
      .filter((p) => p.activa)
      .map((p) => ({ label: p.nombre, value: p.id })),
  );

  ngOnInit(): void {
    this.store.cargarPlantillas();
    const id = this.pacienteId();
    if (id) {
      this.store.cargarConsentimientosDelPaciente(id);
    }
  }

  protected cargar(): void {
    const paciente = this.paciente();
    if (paciente) {
      this.store.cargarConsentimientosDelPaciente(paciente.pacienteId);
    }
  }

  protected abrirSolicitud(): void {
    this.error.set(null);
    this.procedimiento.set('');
    this.plantillaId.set(null);
    this.pacienteSolicitud.set(this.paciente());
    this.solicitando.set(true);
  }

  protected solicitar(): void {
    const paciente = this.pacienteSolicitud();
    const plantilla = this.plantillaId();
    if (!paciente || !plantilla || !this.procedimiento().trim()) {
      this.error.set('Elige el paciente, la plantilla e indica el procedimiento.');
      return;
    }
    this.enviando.set(true);
    this.store
      .solicitarConsentimiento({
        plantillaId: plantilla,
        pacienteId: paciente.pacienteId,
        procedimiento: this.procedimiento().trim(),
      })
      .subscribe({
        next: () => {
          this.enviando.set(false);
          this.solicitando.set(false);
          this.notificaciones.success('Se solicitó el consentimiento. El paciente fue notificado.');
          this.paciente.set(paciente);
          this.store.cargarConsentimientosDelPaciente(paciente.pacienteId);
        },
        error: (e: { message: string }) => {
          this.enviando.set(false);
          this.error.set(e.message);
        },
      });
  }

  protected alFirmar(): void {
    this.firmando.set(null);
    this.cargar();
  }

  protected revocar(consentimiento: ConsentimientoResumen): void {
    this.motivos
      .solicitar({
        titulo: 'Revocar consentimiento',
        etiqueta: 'Motivo de la revocación',
        confirmar: 'Revocar',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.store.revocarConsentimiento(consentimiento.id, motivo).subscribe(() => {
          this.notificaciones.success('El consentimiento fue revocado.');
          this.cargar();
        }),
      );
  }

  protected descargar(consentimiento: ConsentimientoResumen): void {
    this.api.consentimientoPdf(consentimiento.id).subscribe((a) => this.descargas.guardar(a));
  }
}
