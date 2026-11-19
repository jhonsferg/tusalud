import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';

import { DescargaService, FacturacionApiService, RegistroVentas, toIsoDate } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { EstadoCpeTag } from '../estado-cpe-tag';

@Component({
  selector: 'app-registro-ventas',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    TableModule,
    EmptyState,
    EstadoCpeTag,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Registro de ventas"
      subtitle="Comprobantes aceptados del periodo, con las notas de crédito restando"
      icon="pi-book"
    >
      <p-button
        label="Exportar a Excel"
        icon="pi pi-file-excel"
        [outlined]="true"
        [loading]="exportando()"
        (onClick)="exportar()"
      />
    </app-page-header>

    <section class="panel">
      <div class="filtros">
        <p-datepicker
          selectionMode="range"
          [ngModel]="rango()"
          (ngModelChange)="cambiarRango($event)"
          [showIcon]="true"
          dateFormat="dd/mm/yy"
          [readonlyInput]="true"
          appendTo="body"
          ariaLabel="Periodo del registro"
        />
      </div>

      @if (error(); as mensaje) {
        <app-empty-state
          tone="error"
          icon="pi-exclamation-triangle"
          title="No pudimos cargar el registro"
          [message]="mensaje"
        />
      } @else if (registro(); as r) {
        <p-table
          [value]="r.filas"
          [loading]="cargando()"
          [rowHover]="true"
          responsiveLayout="scroll"
        >
          <ng-template #header>
            <tr>
              <th>Fecha</th>
              <th>Comprobante</th>
              <th>Cliente</th>
              <th class="text-right">Gravada</th>
              <th class="text-right">Exonerada</th>
              <th class="text-right">IGV</th>
              <th class="text-right">Total</th>
              <th>Estado</th>
            </tr>
          </ng-template>
          <ng-template #body let-f>
            <tr>
              <td>{{ f.fecha | date: 'd MMM y' }}</td>
              <td class="font-semibold">{{ f.serie }}-{{ f.numero }}</td>
              <td>
                {{ f.clienteNombre }}
                <small class="block text-muted">{{ f.clienteDoc || 'Sin documento' }}</small>
              </td>
              <td class="text-right">{{ f.gravada | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ f.exonerada | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ f.igv | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ f.total | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td><app-estado-cpe-tag [estado]="f.estado" /></td>
            </tr>
          </ng-template>
          <ng-template #footer>
            <tr class="pie">
              <td colspan="3">Totales del periodo</td>
              <td class="text-right">{{ r.totalGravada | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ r.totalExonerada | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ r.totalIgv | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ r.total | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td></td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="8">
                <app-empty-state
                  icon="pi-book"
                  title="Sin ventas en el periodo"
                  message="Solo aparecen los comprobantes aceptados por SUNAT y los anulados."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
      }
    </section>
  `,
  styles: `
    .filtros {
      margin-bottom: 1rem;
    }

    .pie td {
      font-weight: 700;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroVentasPagina implements OnInit {
  private readonly api = inject(FacturacionApiService);
  private readonly descargas = inject(DescargaService);

  protected readonly registro = signal<RegistroVentas | null>(null);
  protected readonly cargando = signal(false);
  protected readonly exportando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly rango = signal<Date[] | null>(this.mesActual());

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarRango(rango: Date[] | null): void {
    this.rango.set(rango);
    if (rango?.[0] && rango[1]) {
      this.cargar();
    }
  }

  protected exportar(): void {
    const [desde, hasta] = this.fechas();
    this.exportando.set(true);
    this.api.registroVentasExcel(desde, hasta).subscribe({
      next: (archivo) => {
        this.exportando.set(false);
        this.descargas.guardar(archivo);
      },
      error: () => this.exportando.set(false),
    });
  }

  private cargar(): void {
    const [desde, hasta] = this.fechas();
    this.cargando.set(true);
    this.error.set(null);
    this.api.registroVentas(desde, hasta).subscribe({
      next: (r) => {
        this.registro.set(r);
        this.cargando.set(false);
      },
      error: (e: { message: string }) => {
        this.error.set(e.message);
        this.cargando.set(false);
      },
    });
  }

  private fechas(): [string | undefined, string | undefined] {
    const rango = this.rango();
    const desde = rango?.[0];
    const hasta = rango?.[1] ?? desde;
    return [desde ? toIsoDate(desde) : undefined, hasta ? toIsoDate(hasta) : undefined];
  }

  private mesActual(): Date[] {
    const hoy = new Date();
    return [new Date(hoy.getFullYear(), hoy.getMonth(), 1), hoy];
  }
}
