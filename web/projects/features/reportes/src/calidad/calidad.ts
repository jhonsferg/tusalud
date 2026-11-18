import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { FiltroReporte, ReportesStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { PageHeader } from '@tusalud/ui/page-header';
import { FiltrosReporte } from '../shared/filtros-reporte/filtros-reporte';
import { OPCIONES_BARRAS } from '../shared/formato';

@Component({
  selector: 'app-reporte-calidad',
  imports: [
    BotonExportar,
    DecimalPipe,
    UIChart,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
    FiltrosReporte,
  ],
  templateUrl: './calidad.html',
  styleUrl: './calidad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteCalidad {
  protected readonly store = inject(ReportesStore);
  protected readonly opcionesBarras = OPCIONES_BARRAS;

  protected readonly graficoDistribucion = computed(() => {
    const distribucion = this.store.calidad.value()?.valoraciones.distribucion ?? [];
    return {
      labels: ['1 estrella', '2 estrellas', '3 estrellas', '4 estrellas', '5 estrellas'],
      datasets: [
        {
          label: 'Valoraciones',
          data: distribucion,
          backgroundColor: ['#ef4444', '#f97316', '#f59e0b', '#84cc16', '#10b981'],
          borderRadius: 6,
        },
      ],
    };
  });

  protected severidad(tasa: number): 'success' | 'warn' | 'danger' {
    return tasa >= 25 ? 'danger' : tasa >= 10 ? 'warn' : 'success';
  }

  protected readonly filtro = signal<FiltroReporte | null>(null);
  protected readonly parametros = computed(() => {
    const actual = this.filtro();
    return actual
      ? { desde: actual.desde, hasta: actual.hasta, agrupacion: actual.agrupacion }
      : {};
  });

  protected cargar(filtro: FiltroReporte): void {
    this.filtro.set(filtro);
    this.store.cargarCalidad(filtro);
  }
}
