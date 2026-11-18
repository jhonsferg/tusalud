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
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  CarteraResponse,
  CuentaAseguradoraResponse,
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_ESTADO_CUENTA,
  NotificationService,
  SegurosApiService,
  SegurosStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_CUENTA, hoyIso } from '../seguros.utils';

@Component({
  selector: 'app-cartera',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Dialog,
    InputText,
    Message,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Cartera de aseguradoras"
      subtitle="Lo que cada aseguradora debe a la clínica y su antigüedad"
      icon="pi-briefcase"
    />

    <div class="panel">
      <p-table
        [value]="store.cartera.value() ?? []"
        [loading]="store.cartera.loading()"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Aseguradora</th>
            <th class="text-right">Cuentas</th>
            <th class="text-right">Pendiente</th>
            <th class="text-right">0 a 30 días</th>
            <th class="text-right">31 a 60</th>
            <th class="text-right">61 a 90</th>
            <th class="text-right">Más de 90</th>
            <th style="width: 12rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td class="font-semibold">{{ c.aseguradoraNombre }}</td>
            <td class="text-right">{{ c.cuentas }}</td>
            <td class="text-right font-semibold">
              {{ c.pendiente | currency: 'PEN' : 'symbol-narrow' }}
            </td>
            <td class="text-right">{{ c.tramo0a30 | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ c.tramo31a60 | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ c.tramo61a90 | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right" [class.alerta]="c.tramoMas90 > 0">
              {{ c.tramoMas90 | currency: 'PEN' : 'symbol-narrow' }}
            </td>
            <td class="text-right acciones">
              <p-button
                icon="pi pi-list"
                [text]="true"
                [rounded]="true"
                severity="secondary"
                [ariaLabel]="'Ver cuentas de ' + c.aseguradoraNombre"
                (onClick)="verCuentas(c)"
              />
              @if (puedeLiquidar()) {
                <p-button
                  label="Liquidar"
                  icon="pi pi-file-edit"
                  size="small"
                  [ariaLabel]="'Liquidar ' + c.aseguradoraNombre"
                  (onClick)="abrirLiquidar(c)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="8">
              <app-empty-state
                icon="pi-check-circle"
                title="Sin cartera pendiente"
                message="Ninguna aseguradora tiene cuentas por cobrar."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>

    <p-dialog
      [header]="'Cuentas de ' + (aseguradora()?.aseguradoraNombre ?? '')"
      [visible]="aseguradora() !== null && !liquidando()"
      (visibleChange)="$event ? null : aseguradora.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '60rem', maxWidth: '96vw' }"
    >
      <p-table
        [value]="cuentas()"
        [loading]="cargandoCuentas()"
        [paginator]="cuentas().length > 8"
        [rows]="8"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Fecha</th>
            <th>Servicio</th>
            <th>Comprobante</th>
            <th class="text-right">Total</th>
            <th class="text-right">Cubierto</th>
            <th class="text-right">Copago</th>
            <th>Estado</th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>
              {{ c.fecha | date: 'd MMM y'
              }}<small class="text-muted block">{{ c.antiguedadDias }} días</small>
            </td>
            <td>
              {{ etiquetaCategoria[c.categoria]
              }}<small class="text-muted block">{{ c.descripcion }}</small>
            </td>
            <td>{{ c.comprobanteCodigo || '—' }}</td>
            <td class="text-right">{{ c.montoTotal | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right font-semibold">
              {{ c.montoCubierto | currency: 'PEN' : 'symbol-narrow' }}
            </td>
            <td class="text-right">{{ c.copago | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td><p-tag [value]="etiquetaEstado[c.estado]" [severity]="severidad[c.estado]" /></td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7"><app-empty-state icon="pi-inbox" title="Sin cuentas" /></td>
          </tr>
        </ng-template>
      </p-table>
    </p-dialog>

    <p-dialog
      [header]="'Liquidar a ' + (aseguradora()?.aseguradoraNombre ?? '')"
      [visible]="liquidando()"
      (visibleChange)="$event ? null : liquidando.set(false)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <div class="form">
        @if (error(); as mensaje) {
          <p-message severity="error" styleClass="w-full">{{ mensaje }}</p-message>
        }
        <p class="text-muted">
          Se agrupan las cuentas pendientes de la aseguradora dentro del periodo.
        </p>
        <div class="dos">
          <label
            >Desde
            <input
              pInputText
              id="liq-desde"
              type="date"
              class="w-full"
              [ngModel]="desde()"
              (ngModelChange)="desde.set($event)"
          /></label>
          <label
            >Hasta
            <input
              pInputText
              id="liq-hasta"
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
          (onClick)="liquidando.set(false)"
        />
        <p-button
          label="Armar liquidación"
          icon="pi pi-check"
          [loading]="guardando()"
          (onClick)="liquidar()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: `
    .alerta {
      color: var(--estado-peligro);
      font-weight: 700;
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
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cartera implements OnInit {
  protected readonly store = inject(SegurosStore);
  private readonly api = inject(SegurosApiService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  protected readonly puedeLiquidar = computed(() => this.auth.hasAnyRole(['ROLE_FINANZAS']));
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly etiquetaEstado: Readonly<Record<string, string>> = ETIQUETA_ESTADO_CUENTA;
  protected readonly severidad = SEVERIDAD_CUENTA;

  protected readonly aseguradora = signal<CarteraResponse | null>(null);
  protected readonly cuentas = signal<CuentaAseguradoraResponse[]>([]);
  protected readonly cargandoCuentas = signal(false);
  protected readonly liquidando = signal(false);
  protected readonly desde = signal(hoyIso(-30));
  protected readonly hasta = signal(hoyIso());
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.store.cargarCartera();
  }

  protected verCuentas(cartera: CarteraResponse): void {
    this.aseguradora.set(cartera);
    this.cuentas.set([]);
    this.cargandoCuentas.set(true);
    this.api.cuentasDeAseguradora(cartera.aseguradoraId).subscribe({
      next: (cuentas) => {
        this.cuentas.set(cuentas);
        this.cargandoCuentas.set(false);
      },
      error: () => this.cargandoCuentas.set(false),
    });
  }

  protected abrirLiquidar(cartera: CarteraResponse): void {
    this.aseguradora.set(cartera);
    this.error.set(null);
    this.desde.set(hoyIso(-30));
    this.hasta.set(hoyIso());
    this.liquidando.set(true);
  }

  protected liquidar(): void {
    const cartera = this.aseguradora();
    if (!cartera) {
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    this.store
      .crearLiquidacion({
        aseguradoraId: cartera.aseguradoraId,
        desde: this.desde(),
        hasta: this.hasta(),
      })
      .subscribe({
        next: (liquidacion) => {
          this.guardando.set(false);
          this.liquidando.set(false);
          this.aseguradora.set(null);
          this.notifications.success(`La liquidación ${liquidacion.codigo} quedó en borrador.`);
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
