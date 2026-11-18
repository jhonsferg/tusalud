import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_METODO_CAJA,
  ETIQUETA_ORIGEN_INGRESO,
  FiltroReporte,
  ReportesStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { PageHeader } from '@tusalud/ui/page-header';
import { FiltrosReporte } from '../shared/filtros-reporte/filtros-reporte';
import { OPCIONES_BARRAS, OPCIONES_DONA, PALETA, etiquetaFecha } from '../shared/formato';

@Component({
  selector: 'app-reporte-financiero',
  imports: [
    BotonExportar,
    CurrencyPipe,
    DecimalPipe,
    UIChart,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
    FiltrosReporte,
  ],
  templateUrl: './financiero.html',
  styleUrl: './financiero.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteFinanciero {
  protected readonly store = inject(ReportesStore);
  protected readonly etiquetaMetodo = ETIQUETA_METODO_CAJA;
  protected readonly etiquetaOrigen = ETIQUETA_ORIGEN_INGRESO;
  protected readonly opcionesBarras = OPCIONES_BARRAS;
  protected readonly opcionesDona = OPCIONES_DONA;

  protected readonly graficoDias = computed(() => {
    const datos = this.store.financiero.value()?.porDia ?? [];
    return {
      labels: datos.map((d) => etiquetaFecha(d.fecha)),
      datasets: [
        {
          label: 'Cobrado',
          data: datos.map((d) => d.total),
          backgroundColor: '#3b82f6',
          borderRadius: 6,
        },
      ],
    };
  });

  protected readonly graficoMetodos = computed(() => {
    const datos = this.store.financiero.value()?.porMetodo ?? [];
    return {
      labels: datos.map((d) => this.etiquetaMetodo[d.metodo] ?? d.metodo),
      datasets: [{ data: datos.map((d) => d.total), backgroundColor: PALETA }],
    };
  });

  protected readonly graficoOrigen = computed(() => {
    const datos = this.store.financiero.value()?.porOrigen ?? [];
    return {
      labels: datos.map((d) => this.etiquetaOrigen[d.origen] ?? d.origen),
      datasets: [{ data: datos.map((d) => d.total), backgroundColor: PALETA }],
    };
  });

  protected readonly filtro = signal<FiltroReporte | null>(null);
  protected readonly parametros = computed(() => {
    const actual = this.filtro();
    return actual
      ? { desde: actual.desde, hasta: actual.hasta, agrupacion: actual.agrupacion }
      : {};
  });

  protected cargar(filtro: FiltroReporte): void {
    this.filtro.set(filtro);
    this.store.cargarFinanciero(filtro);
  }
}
