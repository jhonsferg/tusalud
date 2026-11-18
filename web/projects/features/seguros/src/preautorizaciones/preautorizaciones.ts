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
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  EstadoPreautorizacion,
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_ESTADO_PREAUTORIZACION,
  NotificationService,
  PreautorizacionResponse,
  SegurosStore,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_PREAUTORIZACION } from '../seguros.utils';

@Component({
  selector: 'app-preautorizaciones-gestion',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    Message,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Preautorizaciones"
      subtitle="Solicitudes de los médicos que el convenio exige antes de atender"
      icon="pi-verified"
    />

    <div class="panel">
      <div class="filtros">
        <p-select
          [options]="estados"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estado()"
          (ngModelChange)="cambiarEstado($event)"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
          styleClass="w-full md:w-14rem"
        />
      </div>
      <p-table
        [value]="store.preautorizaciones.value() ?? []"
        [loading]="store.preautorizaciones.loading()"
        [paginator]="(store.preautorizaciones.value() ?? []).length > 10"
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
            <th style="width: 13rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-p>
          <tr>
            <td>
              <div class="font-semibold">{{ p.codigo }}</div>
              <small class="text-muted"
                >{{ p.solicitadaEn | date: 'd MMM, HH:mm' }} · {{ p.solicitadaPorNombre }}</small
              >
            </td>
            <td>
              <span class="sensible">{{ p.pacienteNombre }}</span>
            </td>
            <td>
              {{ p.aseguradoraNombre
              }}<small class="text-muted block"
                >Póliza <span class="sensible">{{ p.polizaNumero }}</span></small
              >
            </td>
            <td>
              {{ etiquetaCategoria[p.categoria]
              }}<small class="text-muted block">{{ p.descripcion }}</small>
            </td>
            <td class="text-right">{{ p.montoEstimado | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td>
              <p-tag [value]="etiquetaEstado[p.estado]" [severity]="severidad[p.estado]" />
              @if (p.codigoAutorizacion) {
                <small class="text-muted block">{{ p.codigoAutorizacion }}</small>
              }
              @if (p.motivoRechazo) {
                <small class="text-muted block">{{ p.motivoRechazo }}</small>
              }
            </td>
            <td class="text-right acciones">
              @if (puedeGestionar() && p.estado === 'SOLICITADA') {
                <p-button
                  label="Aprobar"
                  icon="pi pi-check"
                  size="small"
                  [ariaLabel]="'Aprobar ' + p.codigo"
                  (onClick)="abrirAprobar(p)"
                />
                <p-button
                  label="Rechazar"
                  icon="pi pi-times"
                  size="small"
                  severity="danger"
                  [outlined]="true"
                  [ariaLabel]="'Rechazar ' + p.codigo"
                  (onClick)="rechazar(p)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-verified"
                title="Sin preautorizaciones"
                message="No hay solicitudes con ese estado."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <p-dialog
      [header]="'Aprobar ' + (aprobando()?.codigo ?? '')"
      [visible]="aprobando() !== null"
      (visibleChange)="$event ? null : aprobando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '28rem', maxWidth: '96vw' }"
    >
      @if (aprobando(); as p) {
        <div class="form">
          @if (error(); as mensaje) {
            <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
          }
          <p>
            {{ p.descripcion }} · estimado
            <strong>{{ p.montoEstimado | currency: 'PEN' : 'symbol-narrow' }}</strong>
          </p>
          <label>
            Monto aprobado
            <p-inputnumber
              inputId="pre-monto"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0.01"
              [ngModel]="monto()"
              (ngModelChange)="monto.set($event ?? 0)"
              [fluid]="true"
            />
          </label>
          <label>
            Vigencia en días
            <p-inputnumber
              inputId="pre-dias"
              [min]="1"
              [max]="180"
              [ngModel]="dias()"
              (ngModelChange)="dias.set($event ?? 30)"
              [fluid]="true"
            />
          </label>
        </div>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="aprobando.set(null)"
        />
        <p-button
          label="Aprobar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="aprobar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .filtros {
      margin-bottom: var(--sp-3);
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
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreautorizacionesGestion implements OnInit {
  protected readonly store = inject(SegurosStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly motivos = inject(MotivoService);

  protected readonly puedeGestionar = computed(() =>
    this.auth.hasAnyRole(['ROLE_FINANZAS', 'ROLE_RECEPCION']),
  );
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly etiquetaEstado: Readonly<Record<string, string>> =
    ETIQUETA_ESTADO_PREAUTORIZACION;
  protected readonly severidad = SEVERIDAD_PREAUTORIZACION;
  protected readonly estados = (['SOLICITADA', 'APROBADA', 'RECHAZADA', 'VENCIDA'] as const).map(
    (valor) => ({
      label: ETIQUETA_ESTADO_PREAUTORIZACION[valor],
      value: valor,
    }),
  );

  protected readonly estado = signal<EstadoPreautorizacion | null>('SOLICITADA');
  protected readonly aprobando = signal<PreautorizacionResponse | null>(null);
  protected readonly monto = signal(0);
  protected readonly dias = signal(30);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarEstado(valor: EstadoPreautorizacion | null): void {
    this.estado.set(valor);
    this.cargar();
  }

  private cargar(): void {
    this.store.cargarPreautorizaciones(this.estado());
  }

  protected abrirAprobar(preautorizacion: PreautorizacionResponse): void {
    this.aprobando.set(preautorizacion);
    this.monto.set(preautorizacion.montoEstimado);
    this.dias.set(30);
    this.error.set(null);
  }

  protected aprobar(): void {
    const preautorizacion = this.aprobando();
    if (!preautorizacion) {
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.store
      .aprobar(preautorizacion.id, { montoAprobado: this.monto(), vigenciaDias: this.dias() })
      .subscribe({
        next: (aprobada) => {
          this.guardando.set(false);
          this.aprobando.set(null);
          this.notifications.success(
            `La preautorización ${aprobada.codigo} fue aprobada con el código ${aprobada.codigoAutorizacion}.`,
          );
          this.cargar();
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected rechazar(preautorizacion: PreautorizacionResponse): void {
    this.motivos
      .solicitar({
        titulo: `Rechazar ${preautorizacion.codigo}`,
        descripcion: 'El médico verá este motivo.',
        etiqueta: 'Motivo del rechazo',
        confirmar: 'Rechazar',
        severidad: 'danger',
      })
      .subscribe((motivo) => {
        this.store.rechazar(preautorizacion.id, motivo).subscribe({
          next: (rechazada) => {
            this.notifications.success(`La preautorización ${rechazada.codigo} fue rechazada.`);
            this.cargar();
          },
          error: () => undefined,
        });
      });
  }
}
