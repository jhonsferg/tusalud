import { DatePipe } from '@angular/common';
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
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ConvenioResponse,
  ETIQUETA_ESTADO_POLIZA,
  NotificationService,
  PacienteMostradorResponse,
  PolizaResponse,
  SegurosApiService,
  VerificacionCoberturaResponse,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PacienteBuscador } from '@tusalud/ui/paciente-buscador';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_POLIZA, hoyIso } from '../seguros.utils';

const PARENTESCOS = ['TITULAR', 'CONYUGE', 'HIJO', 'PADRE', 'MADRE', 'OTRO'];

@Component({
  selector: 'app-polizas',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    Dialog,
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
      title="Pólizas de pacientes"
      subtitle="Registra y verifica la cobertura antes de atender"
      icon="pi-id-card"
    >
      <p-button
        label="Registrar póliza"
        icon="pi pi-plus"
        [disabled]="paciente() === null"
        (onClick)="abrirRegistro()"
      />
    </app-page-header>

    <div class="panel">
      <div class="busqueda">
        <label class="campo">
          Paciente
          <app-paciente-buscador
            inputId="pol-paciente"
            [paciente]="paciente()"
            (pacienteChange)="elegir($event)"
          />
        </label>
        <label class="campo">
          O busca por número de póliza o nombre
          <span class="fila">
            <input
              pInputText
              id="pol-q"
              class="w-full"
              [ngModel]="q()"
              (ngModelChange)="q.set($event)"
              (keydown.enter)="buscarTexto()"
              autocomplete="off"
            />
            <p-button
              label="Buscar"
              icon="pi pi-search"
              [outlined]="true"
              (onClick)="buscarTexto()"
            />
          </span>
        </label>
      </div>

      @if (errorBusqueda(); as mensaje) {
        <p-message severity="warn" styleClass="w-full mb-3">{{ mensaje }}</p-message>
      }

      <p-table [value]="polizas()" [loading]="cargando()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Paciente</th>
            <th>Aseguradora y plan</th>
            <th>Póliza</th>
            <th>Titular</th>
            <th>Vigencia</th>
            <th>Estado</th>
            <th style="width: 13rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <span class="sensible">{{ p.pacienteNombre }}</span
              ><small class="text-muted block"
                >DNI <span class="sensible">{{ p.pacienteDni }}</span></small
              >
            </td>
            <td>
              {{ p.aseguradoraNombre
              }}<small class="text-muted block"
                >{{ p.convenioNombre }}{{ p.plan ? ' · ' + p.plan : '' }}</small
              >
            </td>
            <td class="font-semibold">
              <span class="sensible">{{ p.numero }}</span>
            </td>
            <td>
              <span class="sensible">{{ p.titularNombre }}</span
              ><small class="text-muted block">{{ p.parentesco }}</small>
            </td>
            <td>
              {{ p.vigenciaDesde | date: 'd MMM y' }} al {{ p.vigenciaHasta | date: 'd MMM y' }}
              @if (p.diasParaVencer != null && p.diasParaVencer <= 30) {
                <small class="aviso block">Vence en {{ p.diasParaVencer }} días</small>
              }
            </td>
            <td>
              <p-tag [value]="etiquetaEstado[p.estado]" [severity]="severidad[p.estado]" />
              @if (p.motivoSuspension) {
                <small class="text-muted block">{{ p.motivoSuspension }}</small>
              }
            </td>
            <td class="text-right acciones">
              <p-button
                label="Verificar"
                icon="pi pi-shield"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Verificar la póliza ' + p.numero"
                (onClick)="verificar(p)"
              />
              @if (p.estado === 'SUSPENDIDA') {
                <p-button
                  label="Reactivar"
                  size="small"
                  severity="secondary"
                  [text]="true"
                  [ariaLabel]="'Reactivar la póliza ' + p.numero"
                  (onClick)="reactivar(p)"
                />
              } @else if (p.estado === 'ACTIVA') {
                <p-button
                  label="Suspender"
                  size="small"
                  severity="danger"
                  [text]="true"
                  [ariaLabel]="'Suspender la póliza ' + p.numero"
                  (onClick)="suspender(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-id-card"
                title="Sin pólizas"
                message="Elige un paciente o busca por número de póliza."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <p-dialog
      header="Registrar póliza"
      [visible]="registrando()"
      (visibleChange)="registrando.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      <div class="form">
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <p>
          Paciente: <strong class="sensible">{{ paciente()?.nombreCompleto }}</strong>
        </p>
        <label>
          Convenio
          <p-select
            inputId="pol-convenio"
            [options]="opcionesConvenio()"
            optionLabel="label"
            optionValue="value"
            [ngModel]="convenioId()"
            (ngModelChange)="convenioId.set($event)"
            placeholder="Selecciona un convenio vigente"
            appendTo="body"
            styleClass="w-full"
          />
        </label>
        <label
          >Número de póliza
          <input
            pInputText
            id="pol-numero"
            class="w-full"
            [ngModel]="numero()"
            (ngModelChange)="numero.set($event)"
            maxlength="30"
            autocomplete="off"
        /></label>
        <label
          >Titular
          <input
            pInputText
            id="pol-titular"
            class="w-full"
            [ngModel]="titular()"
            (ngModelChange)="titular.set($event)"
            maxlength="120"
            autocomplete="off"
        /></label>
        <div class="dos">
          <label>
            Parentesco con el titular
            <p-select
              inputId="pol-parentesco"
              [options]="parentescos"
              [ngModel]="parentesco()"
              (ngModelChange)="parentesco.set($event)"
              appendTo="body"
              styleClass="w-full"
            />
          </label>
          <label
            >Plan
            <input
              pInputText
              id="pol-plan"
              class="w-full"
              [ngModel]="plan()"
              (ngModelChange)="plan.set($event)"
              maxlength="60"
          /></label>
        </div>
        <div class="dos">
          <label
            >Vigente desde
            <input
              pInputText
              id="pol-desde"
              type="date"
              class="w-full"
              [ngModel]="desde()"
              (ngModelChange)="desde.set($event)"
          /></label>
          <label
            >Vigente hasta
            <input
              pInputText
              id="pol-hasta"
              type="date"
              class="w-full"
              [ngModel]="hasta()"
              (ngModelChange)="hasta.set($event)"
          /></label>
        </div>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="registrando.set(false)"
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
      header="Verificación de cobertura"
      [visible]="verificacion() !== null"
      (visibleChange)="$event ? null : verificacion.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '28rem', maxWidth: '96vw' }"
    >
      @if (verificacion(); as v) {
        <div class="resultado" id="pol-verificacion" [class.vigente]="v.vigente">
          <i
            class="pi"
            [class.pi-check-circle]="v.vigente"
            [class.pi-times-circle]="!v.vigente"
          ></i>
          <strong>{{ v.vigente ? 'Cobertura vigente' : 'Sin cobertura vigente' }}</strong>
          <span>Estado: {{ v.estado }}</span>
          @if (v.saldoTope != null) {
            <span>Saldo del tope anual: S/ {{ v.saldoTope.toFixed(2) }}</span>
          }
          <span
            >Código de verificación
            <strong id="pol-codigo-verificacion">{{ v.codigo }}</strong></span
          >
          <small class="text-muted">{{ v.mensaje }}</small>
        </div>
      }
      <ng-template #footer>
        <p-button label="Cerrar" (onClick)="verificacion.set(null)" />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .busqueda {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-4);
      margin-bottom: var(--sp-3);
    }
    .campo {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-size: 0.9rem;
      font-weight: 600;
    }
    .fila {
      display: flex;
      gap: var(--sp-2);
    }
    .aviso {
      color: var(--estado-aviso);
      font-weight: 600;
    }
    .acciones {
      white-space: nowrap;
    }
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
    .dos {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-3);
    }
    .resultado {
      display: flex;
      flex-direction: column;
      gap: var(--sp-2);
      padding: var(--sp-3);
      border-radius: var(--radio-sm);
      background: var(--estado-peligro-fondo);
    }
    .resultado.vigente {
      background: var(--estado-exito-fondo);
    }
    .resultado i {
      font-size: 1.6rem;
    }
    @media (max-width: 720px) {
      .busqueda,
      .dos {
        grid-template-columns: 1fr;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Polizas implements OnInit {
  private readonly api = inject(SegurosApiService);
  private readonly notifications = inject(NotificationService);
  private readonly motivos = inject(MotivoService);

  protected readonly etiquetaEstado: Readonly<Record<string, string>> = ETIQUETA_ESTADO_POLIZA;
  protected readonly severidad = SEVERIDAD_POLIZA;
  protected readonly parentescos = PARENTESCOS;

  protected readonly paciente = signal<PacienteMostradorResponse | null>(null);
  protected readonly polizas = signal<PolizaResponse[]>([]);
  protected readonly cargando = signal(false);
  protected readonly errorBusqueda = signal<string | null>(null);
  protected readonly q = signal('');
  private readonly convenios = signal<ConvenioResponse[]>([]);

  protected readonly registrando = signal(false);
  protected readonly convenioId = signal<string | null>(null);
  protected readonly numero = signal('');
  protected readonly titular = signal('');
  protected readonly parentesco = signal('TITULAR');
  protected readonly plan = signal('');
  protected readonly desde = signal(hoyIso());
  protected readonly hasta = signal(hoyIso(365));
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verificacion = signal<VerificacionCoberturaResponse | null>(null);

  protected readonly opcionesConvenio = computed(() =>
    this.convenios()
      .filter((c) => c.vigente)
      .map((c) => ({ label: `${c.aseguradoraNombre} · ${c.nombre}`, value: c.id })),
  );

  ngOnInit(): void {
    this.api.convenios().subscribe((convenios) => this.convenios.set(convenios));
  }

  protected elegir(paciente: PacienteMostradorResponse | null): void {
    this.paciente.set(paciente);
    this.errorBusqueda.set(null);
    if (paciente) {
      this.cargarDelPaciente(paciente.pacienteId);
    } else {
      this.polizas.set([]);
    }
  }

  private cargarDelPaciente(pacienteId: string): void {
    this.cargando.set(true);
    this.api.polizasDelPaciente(pacienteId).subscribe({
      next: (polizas) => {
        this.polizas.set(polizas);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected buscarTexto(): void {
    const texto = this.q().trim();
    this.errorBusqueda.set(null);
    if (texto.length < 2) {
      this.errorBusqueda.set('Escribe al menos 2 caracteres para buscar.');
      return;
    }
    this.paciente.set(null);
    this.cargando.set(true);
    this.api.buscarPolizas(texto).subscribe({
      next: (polizas) => {
        this.polizas.set(polizas);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }

  protected abrirRegistro(): void {
    const paciente = this.paciente();
    this.convenioId.set(null);
    this.numero.set('');
    this.titular.set(paciente?.nombreCompleto ?? '');
    this.parentesco.set('TITULAR');
    this.plan.set('');
    this.desde.set(hoyIso());
    this.hasta.set(hoyIso(365));
    this.error.set(null);
    this.registrando.set(true);
  }

  protected registrar(): void {
    const paciente = this.paciente();
    const convenioId = this.convenioId();
    if (!paciente || !convenioId || !this.numero().trim() || !this.titular().trim()) {
      this.error.set('Elige el convenio e indica el número de póliza y el titular.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.api
      .registrarPoliza({
        pacienteId: paciente.pacienteId,
        convenioId,
        numero: this.numero().trim(),
        titularNombre: this.titular().trim(),
        parentesco: this.parentesco(),
        plan: this.plan().trim() || null,
        vigenciaDesde: this.desde(),
        vigenciaHasta: this.hasta(),
      })
      .subscribe({
        next: (poliza) => {
          this.guardando.set(false);
          this.registrando.set(false);
          this.notifications.success(`La póliza ${poliza.numero} fue registrada.`);
          this.cargarDelPaciente(paciente.pacienteId);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected verificar(poliza: PolizaResponse): void {
    this.api.verificarPoliza(poliza.id).subscribe((resultado) => this.verificacion.set(resultado));
  }

  protected suspender(poliza: PolizaResponse): void {
    this.motivos
      .solicitar({
        titulo: `Suspender la póliza ${poliza.numero}`,
        descripcion: 'Mientras esté suspendida no se aplica cobertura en caja.',
        etiqueta: 'Motivo de la suspensión',
        confirmar: 'Suspender',
        severidad: 'danger',
      })
      .subscribe((motivo) => this.cambiarEstado(poliza, 'SUSPENDIDA', motivo));
  }

  protected reactivar(poliza: PolizaResponse): void {
    this.cambiarEstado(poliza, 'ACTIVA', null);
  }

  private cambiarEstado(
    poliza: PolizaResponse,
    estado: 'ACTIVA' | 'SUSPENDIDA',
    motivo: string | null,
  ): void {
    this.api.cambiarEstadoPoliza(poliza.id, { estado, motivo }).subscribe({
      next: (actualizada) => {
        this.notifications.success(
          `La póliza ${actualizada.numero} quedó ${estado === 'ACTIVA' ? 'activa' : 'suspendida'}.`,
        );
        const paciente = this.paciente();
        if (paciente) {
          this.cargarDelPaciente(paciente.pacienteId);
        } else {
          this.polizas.update((lista) =>
            lista.map((p) => (p.id === actualizada.id ? actualizada : p)),
          );
        }
      },
      error: () => undefined,
    });
  }
}
