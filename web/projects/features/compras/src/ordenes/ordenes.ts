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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ComprasApiService,
  ETIQUETA_ESTADO_ORDEN_COMPRA,
  EstadoOrdenCompra,
  OrdenCompraResumen,
  SEVERIDAD_ESTADO_ORDEN_COMPRA,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

@Component({
  selector: 'app-ordenes',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Órdenes de compra"
      subtitle="Compras a proveedores, con aprobación según el monto"
      icon="pi-shopping-cart"
    >
      @if (esAlmacen()) {
        <p-button label="Nueva orden" icon="pi pi-plus" routerLink="nueva" />
      }
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <p-select
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estado()"
          (ngModelChange)="estado.set($event); cargar()"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
          styleClass="w-full md:w-14rem"
        />
        @if (esAprobador()) {
          <p-button
            label="Por aprobar"
            icon="pi pi-clock"
            size="small"
            [outlined]="estado() !== 'PENDIENTE_APROBACION'"
            (onClick)="verPorAprobar()"
          />
        }
      </div>
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [paginator]="lista().length > 12"
        [rows]="12"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Orden</th>
            <th>Proveedor</th>
            <th class="text-right">Total</th>
            <th>Recibido</th>
            <th>Estado</th>
            <th style="width: 6rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-o>
          <tr>
            <td>
              <span class="font-semibold">{{ o.codigo ?? 'Borrador' }}</span>
              <small class="block text-muted"
                >{{ o.creadaEn | date: 'd MMM y' }} · {{ o.creadaPorNombre }}</small
              >
            </td>
            <td>
              {{ o.proveedorNombre }}
              @if (o.requisicionCodigo) {
                <small class="block text-muted">{{ o.requisicionCodigo }}</small>
              }
            </td>
            <td class="text-right">{{ o.total | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td>{{ o.porcentajeRecibido }} %</td>
            <td>
              <p-tag [value]="etiquetas[o.estado]" [severity]="severidades[o.estado]" />
              @if (o.estado === 'PENDIENTE_APROBACION' && o.nivelAprobacion) {
                <small class="block text-muted"
                  >Aprueba {{ o.nivelAprobacion === 'GERENCIA' ? 'gerencia' : 'finanzas' }}</small
                >
              }
            </td>
            <td class="derecha">
              <p-button
                label="Ver"
                size="small"
                [outlined]="true"
                [routerLink]="[o.id]"
                [ariaLabel]="'Ver la orden ' + (o.codigo ?? 'en borrador de ' + o.proveedorNombre)"
              />
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="6">
              <app-empty-state
                icon="pi-shopping-cart"
                title="Sin órdenes"
                message="No hay órdenes de compra con este filtro."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ordenes implements OnInit {
  private readonly api = inject(ComprasApiService);
  private readonly auth = inject(AuthService);

  protected readonly etiquetas: Record<string, string> = ETIQUETA_ESTADO_ORDEN_COMPRA;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_ESTADO_ORDEN_COMPRA;
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_ORDEN_COMPRA).map(
    ([value, label]) => ({
      label,
      value,
    }),
  );
  protected readonly lista = signal<OrdenCompraResumen[]>([]);
  protected readonly cargando = signal(false);
  protected readonly estado = signal<EstadoOrdenCompra | null>(null);
  protected readonly esAlmacen = computed(() => this.auth.isAlmacen());
  protected readonly esAprobador = computed(() => this.auth.isFinanzas() || this.auth.isGerencia());

  ngOnInit(): void {
    if (this.esAprobador()) {
      this.estado.set('PENDIENTE_APROBACION');
    }
    this.cargar();
  }

  protected verPorAprobar(): void {
    this.estado.set('PENDIENTE_APROBACION');
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.ordenes({ estado: this.estado() }).subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
