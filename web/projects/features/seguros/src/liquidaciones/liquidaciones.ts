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
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import {
  AuthService,
  CuentaAseguradoraResponse,
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_ESTADO_LIQUIDACION,
  LiquidacionResponse,
  NotificationService,
  SegurosApiService,
  SegurosStore,
} from '@tusalud/core';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_LIQUIDACION } from '../seguros.utils';

@Component({
  selector: 'app-liquidaciones',
  imports: [
    BotonDocumento,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputNumber,
    Message,
    TableModule,
    Tag,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Liquidaciones"
      subtitle="Lotes de atenciones cubiertas que se envían a cada aseguradora"
      icon="pi-file-edit"
    />

    <div class="panel">
      <p-table
        [value]="store.liquidaciones.value() ?? []"
        [loading]="store.liquidaciones.loading()"
        [paginator]="(store.liquidaciones.value() ?? []).length > 10"
        [rows]="10"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Código</th>
            <th>Aseguradora</th>
            <th>Periodo</th>
            <th class="text-right">Cuentas</th>
            <th class="text-right">Total</th>
            <th>Estado</th>
            <th class="text-right">Pagado</th>
            <th style="width: 14rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-l>
          <tr>
            <td class="font-semibold">{{ l.codigo }}</td>
            <td>{{ l.aseguradoraNombre }}</td>
            <td>{{ l.desde | date: 'd MMM' }} al {{ l.hasta | date: 'd MMM y' }}</td>
            <td class="text-right">{{ l.cantidadCuentas }}</td>
            <td class="text-right font-semibold">
              {{ l.total | currency: 'PEN' : 'symbol-narrow' }}
            </td>
            <td>
              <p-tag [value]="etiquetaEstado[l.estado]" [severity]="severidad[l.estado]" />
              @if (l.montoGlosa && l.montoGlosa > 0) {
                <small class="text-muted block" [title]="l.motivoGlosa ?? ''"
                  >Glosa {{ l.montoGlosa | currency: 'PEN' : 'symbol-narrow' }}</small
                >
              }
            </td>
            <td class="text-right">
              {{
                l.montoPagado != null ? (l.montoPagado | currency: 'PEN' : 'symbol-narrow') : '—'
              }}
            </td>
            <td class="text-right acciones">
              <p-button
                icon="pi pi-list"
                [text]="true"
                [rounded]="true"
                severity="secondary"
                [ariaLabel]="'Ver cuentas de ' + l.codigo"
                (onClick)="verCuentas(l)"
              />
              <app-boton-documento
                tipo="liquidacion"
                [registro]="l.id"
                [soloIcono]="true"
                [ariaEtiqueta]="'Descargar el PDF de ' + l.codigo"
              />
              @if (puedeGestionar()) {
                @if (l.estado === 'BORRADOR') {
                  <p-button
                    label="Enviar"
                    icon="pi pi-send"
                    size="small"
                    [ariaLabel]="'Enviar ' + l.codigo"
                    (onClick)="enviar(l)"
                  />
                }
                @if (l.estado === 'ENVIADA' || l.estado === 'OBSERVADA') {
                  <p-button
                    label="Registrar pago"
                    icon="pi pi-dollar"
                    size="small"
                    [ariaLabel]="'Registrar el pago de ' + l.codigo"
                    (onClick)="abrirPago(l)"
                  />
                }
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="8">
              <app-empty-state
                icon="pi-file-edit"
                title="Sin liquidaciones"
                message="Arma una desde la cartera de la aseguradora."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <p-dialog
      [header]="'Cuentas de ' + (verLiquidacion()?.codigo ?? '')"
      [visible]="verLiquidacion() !== null"
      (visibleChange)="$event ? null : verLiquidacion.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '56rem', maxWidth: '96vw' }"
    >
      <p-table [value]="cuentas()" [loading]="cargandoCuentas()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Fecha</th>
            <th>Servicio</th>
            <th>Comprobante</th>
            <th class="text-right">Cubierto</th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>{{ c.fecha | date: 'd MMM y' }}</td>
            <td>
              {{ etiquetaCategoria[c.categoria]
              }}<small class="text-muted block">{{ c.descripcion }}</small>
            </td>
            <td>{{ c.comprobanteCodigo || '—' }}</td>
            <td class="text-right font-semibold">
              {{ c.montoCubierto | currency: 'PEN' : 'symbol-narrow' }}
            </td>
          </tr>
        </ng-template>
      </p-table>
    </p-dialog>

    <p-dialog
      [header]="'Registrar el pago de ' + (pagando()?.codigo ?? '')"
      [visible]="pagando() !== null"
      (visibleChange)="$event ? null : pagando.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      @if (pagando(); as l) {
        <div class="form">
          @if (error(); as mensaje) {
            <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
          }
          <p>
            Total liquidado <strong>{{ l.total | currency: 'PEN' : 'symbol-narrow' }}</strong>
          </p>
          <label>
            Monto pagado por la aseguradora
            <p-inputnumber
              inputId="liq-monto"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0"
              [max]="l.total"
              [ngModel]="monto()"
              (ngModelChange)="monto.set($event ?? 0)"
              [fluid]="true"
            />
          </label>
          @if (glosa() > 0) {
            <p class="glosa">
              Glosa de {{ glosa() | currency: 'PEN' : 'symbol-narrow' }}. Explica el motivo.
            </p>
            <label>
              Motivo de la glosa
              <textarea
                pTextarea
                id="liq-glosa"
                rows="3"
                class="w-full"
                [ngModel]="motivo()"
                (ngModelChange)="motivo.set($event)"
                maxlength="300"
                autocomplete="off"
              ></textarea>
            </label>
          }
        </div>
      }
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="pagando.set(null)"
        />
        <p-button
          label="Registrar"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="registrarPago()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
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
    .glosa {
      margin: 0;
      color: var(--estado-aviso);
      font-weight: 600;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Liquidaciones implements OnInit {
  protected readonly store = inject(SegurosStore);
  private readonly api = inject(SegurosApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly puedeGestionar = computed(() => this.auth.hasAnyRole(['ROLE_FINANZAS']));
  protected readonly etiquetaEstado: Readonly<Record<string, string>> = ETIQUETA_ESTADO_LIQUIDACION;
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly severidad = SEVERIDAD_LIQUIDACION;

  protected readonly verLiquidacion = signal<LiquidacionResponse | null>(null);
  protected readonly cuentas = signal<CuentaAseguradoraResponse[]>([]);
  protected readonly cargandoCuentas = signal(false);
  protected readonly pagando = signal<LiquidacionResponse | null>(null);
  protected readonly monto = signal(0);
  protected readonly motivo = signal('');
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly glosa = computed(() => {
    const total = this.pagando()?.total ?? 0;
    return Math.max(0, Math.round((total - this.monto()) * 100) / 100);
  });

  ngOnInit(): void {
    this.store.cargarLiquidaciones();
  }

  protected verCuentas(liquidacion: LiquidacionResponse): void {
    this.verLiquidacion.set(liquidacion);
    this.cuentas.set([]);
    this.cargandoCuentas.set(true);
    this.api.cuentasDeLiquidacion(liquidacion.id).subscribe({
      next: (cuentas) => {
        this.cuentas.set(cuentas);
        this.cargandoCuentas.set(false);
      },
      error: () => this.cargandoCuentas.set(false),
    });
  }

  protected enviar(liquidacion: LiquidacionResponse): void {
    this.store.enviarLiquidacion(liquidacion.id).subscribe({
      next: () => this.notifications.success(`La liquidación ${liquidacion.codigo} fue enviada.`),
      error: () => undefined,
    });
  }

  protected abrirPago(liquidacion: LiquidacionResponse): void {
    this.pagando.set(liquidacion);
    this.monto.set(liquidacion.total);
    this.motivo.set('');
    this.error.set(null);
  }

  protected registrarPago(): void {
    const liquidacion = this.pagando();
    if (!liquidacion) {
      return;
    }
    if (this.glosa() > 0 && this.motivo().trim().length < 5) {
      this.error.set('Indica el motivo de la glosa.');
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.store
      .registrarPago(liquidacion.id, {
        montoPagado: this.monto(),
        motivoGlosa: this.glosa() > 0 ? this.motivo().trim() : null,
      })
      .subscribe({
        next: (actualizada) => {
          this.guardando.set(false);
          this.pagando.set(null);
          this.notifications.success(
            actualizada.estado === 'PAGADA'
              ? `La liquidación ${actualizada.codigo} quedó pagada.`
              : `La liquidación ${actualizada.codigo} quedó observada por la glosa.`,
          );
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
