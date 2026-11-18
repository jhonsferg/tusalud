import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UIChart } from 'primeng/chart';

import { AuthService, FiltroReporte, ReportesStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { PageHeader } from '@tusalud/ui/page-header';
import { FiltrosReporte } from '../shared/filtros-reporte/filtros-reporte';
import { KpiVariacion } from '../shared/kpi-variacion/kpi-variacion';
import { OPCIONES_BARRAS, OPCIONES_LINEA, etiquetaFecha } from '../shared/formato';

@Component({
  selector: 'app-reporte-ejecutivo',
  imports: [
    BotonExportar,
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    UIChart,
    EmptyState,
    PageHeader,
    FiltrosReporte,
    KpiVariacion,
  ],
  templateUrl: './ejecutivo.html',
  styleUrl: './ejecutivo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteEjecutivo {
  protected readonly store = inject(ReportesStore);
  protected readonly auth = inject(AuthService);

  protected readonly opcionesLinea = OPCIONES_LINEA;
  protected readonly opcionesBarras = OPCIONES_BARRAS;

  protected readonly graficoCitas = computed(() => {
    const serie = this.store.ejecutivo.value()?.serie ?? [];
    return {
      labels: serie.map((p) => etiquetaFecha(p.fecha)),
      datasets: [
        { label: 'Citas', data: serie.map((p) => p.citas), borderColor: '#3b82f6', fill: false },
        {
          label: 'Atendidas',
          data: serie.map((p) => p.atendidas),
          borderColor: '#10b981',
          fill: false,
        },
      ],
    };
  });

  protected readonly graficoIngresos = computed(() => {
    const serie = this.store.ejecutivo.value()?.serie ?? [];
    return {
      labels: serie.map((p) => etiquetaFecha(p.fecha)),
      datasets: [
        {
          label: 'Ingresos',
          data: serie.map((p) => p.ingresos),
          backgroundColor: '#8b5cf6',
          borderRadius: 6,
        },
      ],
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
    this.store.cargarEjecutivo(filtro);
  }
}
