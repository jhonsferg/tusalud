import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';

import { FiltroReporte, ReportesStore } from '@tusalud/core';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';
import { FiltrosReporte } from '../shared/filtros-reporte/filtros-reporte';
import { OPCIONES_BARRAS, OPCIONES_LINEA, PALETA, etiquetaFecha } from '../shared/formato';

@Component({
  selector: 'app-reporte-morbilidad',
  imports: [
    BotonExportar,
    DecimalPipe,
    UIChart,
    TableModule,
    EmptyState,
    Kpi,
    PageHeader,
    FiltrosReporte,
  ],
  templateUrl: './morbilidad.html',
  styleUrl: './morbilidad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteMorbilidad {
  protected readonly store = inject(ReportesStore);
  protected readonly opcionesLinea = OPCIONES_LINEA;
  protected readonly opcionesTop = {
    ...OPCIONES_BARRAS,
    indexAxis: 'y' as const,
    scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  protected readonly filtro = signal<FiltroReporte | null>(null);
  protected readonly parametros = computed(() => {
    const actual = this.filtro();
    return actual
      ? { desde: actual.desde, hasta: actual.hasta, agrupacion: actual.agrupacion }
      : {};
  });

  protected readonly graficoTop = computed(() => {
    const top = this.store.morbilidad.value()?.top ?? [];
    return {
      labels: top.map((d) => d.codigo),
      datasets: [
        {
          label: 'Casos',
          data: top.map((d) => d.casos),
          backgroundColor: top.map((_, i) => PALETA[i % PALETA.length]),
          borderRadius: 6,
        },
      ],
    };
  });

  protected readonly graficoSerie = computed(() => {
    const serie = this.store.morbilidad.value()?.serie ?? [];
    return {
      labels: serie.map((p) => etiquetaFecha(p.fecha)),
      datasets: [
        {
          label: 'Casos',
          data: serie.map((p) => p.casos),
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.15)',
          fill: true,
        },
      ],
    };
  });

  protected readonly graficoTendencia = computed(() => {
    const tendencia = this.store.morbilidad.value()?.tendencia ?? [];
    const base = tendencia[0]?.serie ?? [];
    return {
      labels: base.map((p) => etiquetaFecha(p.fecha)),
      datasets: tendencia.map((t, i) => ({
        label: t.codigo,
        data: t.serie.map((p) => p.casos),
        borderColor: PALETA[i % PALETA.length],
        backgroundColor: PALETA[i % PALETA.length],
        fill: false,
      })),
    };
  });

  protected cargar(filtro: FiltroReporte): void {
    this.filtro.set(filtro);
    this.store.cargarMorbilidad(filtro);
  }
}
