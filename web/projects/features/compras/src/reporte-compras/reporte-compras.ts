import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';

import { ComprasApiService, ReporteCompraProveedor, toIsoDate } from '@tusalud/core';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

@Component({
  selector: 'app-reporte-compras',
  imports: [
    CurrencyPipe,
    FormsModule,
    DatePicker,
    TableModule,
    BotonExportar,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Compras por proveedor"
      subtitle="Lo ordenado y recibido por orden aprobada, y lo facturado y pagado por comprobante emitido en el periodo"
      icon="pi-chart-bar"
    >
      <app-boton-exportar
        tipo="reporte-compras"
        formato="XLSX"
        [parametros]="parametros()"
        etiqueta="Exportar a Excel"
      />
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <p-datepicker
          selectionMode="range"
          [ngModel]="rango()"
          (ngModelChange)="cambiarRango($event)"
          [showIcon]="true"
          [showButtonBar]="true"
          dateFormat="dd/mm/yy"
          placeholder="Últimos 90 días"
          [readonlyInput]="true"
          appendTo="body"
          ariaLabel="Periodo del reporte"
        />
      </div>

      <div class="resumen" aria-label="Totales del periodo">
        <div>
          <strong>{{ totales().ordenado | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Ordenado</span>
        </div>
        <div>
          <strong>{{ totales().recibido | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Recibido</span>
        </div>
        <div>
          <strong>{{ totales().facturado | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Facturado</span>
        </div>
        <div>
          <strong>{{ totales().saldo | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Saldo por pagar</span>
        </div>
      </div>

      <p-table [value]="lista()" [loading]="cargando()" responsiveLayout="scroll">
        <ng-template #header>
          <tr>
            <th>Proveedor</th>
            <th class="text-right">Órdenes</th>
            <th class="text-right">Ordenado</th>
            <th class="text-right">Recibido</th>
            <th class="text-right">Facturado</th>
            <th class="text-right">Pagado</th>
            <th class="text-right">Saldo</th>
          </tr>
        </ng-template>
        <ng-template #body let-r>
          <tr>
            <td>
              {{ r.proveedorNombre }}<small class="block text-muted">{{ r.proveedorRuc }}</small>
            </td>
            <td class="text-right">{{ r.ordenes }}</td>
            <td class="text-right">{{ r.montoOrdenado | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ r.montoRecibido | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ r.facturado | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ r.pagado | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ r.saldo | currency: 'PEN' : 'symbol-narrow' }}</td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-chart-bar"
                title="Sin compras"
                message="No hay compras aprobadas ni comprobantes en este periodo."
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
export class ReporteCompras implements OnInit {
  private readonly api = inject(ComprasApiService);

  protected readonly lista = signal<ReporteCompraProveedor[]>([]);
  protected readonly cargando = signal(false);
  protected readonly rango = signal<Date[] | null>(null);

  protected readonly parametros = computed(() => {
    const rango = this.rango();
    const desde = rango?.[0];
    return desde ? { desde: toIsoDate(desde), hasta: toIsoDate(rango?.[1] ?? desde) } : {};
  });

  protected readonly totales = computed(() =>
    this.lista().reduce(
      (t, r) => ({
        ordenado: t.ordenado + r.montoOrdenado,
        recibido: t.recibido + r.montoRecibido,
        facturado: t.facturado + r.facturado,
        saldo: t.saldo + r.saldo,
      }),
      { ordenado: 0, recibido: 0, facturado: 0, saldo: 0 },
    ),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarRango(rango: Date[] | null): void {
    this.rango.set(rango);
    if (!rango || rango[1]) {
      this.cargar();
    }
  }

  private cargar(): void {
    this.cargando.set(true);
    this.api.reportePorProveedor(this.parametros()).subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
