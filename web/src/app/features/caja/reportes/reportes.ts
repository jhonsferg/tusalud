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
import { UIChart } from 'primeng/chart';

import { CajaStore, ETIQUETA_METODO_CAJA, ETIQUETA_ORIGEN, toIsoDate } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-reportes-caja',
  imports: [CurrencyPipe, FormsModule, DatePicker, UIChart, EmptyState, Kpi, PageHeader],
  templateUrl: './reportes.html',
  styleUrl: './reportes.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportesCaja implements OnInit {
  protected readonly store = inject(CajaStore);
  protected readonly rango = signal<Date[] | null>([
    new Date(Date.now() - 29 * 86_400_000),
    new Date(),
  ]);
  protected readonly etiquetaMetodo = ETIQUETA_METODO_CAJA;
  protected readonly etiquetaOrigen = ETIQUETA_ORIGEN;

  protected readonly graficoDias = computed(() => {
    const datos = this.store.reporte.value()?.porDia ?? [];
    return {
      labels: datos.map((d) => d.fecha.slice(5)),
      datasets: [
        {
          label: 'Ingresos',
          data: datos.map((d) => d.total),
          backgroundColor: '#3b82f6',
          borderRadius: 6,
        },
      ],
    };
  });

  protected readonly graficoMetodos = computed(() => {
    const datos = this.store.reporte.value()?.porMetodo ?? [];
    return {
      labels: datos.map((d) => this.etiquetaMetodo[d.metodo] ?? d.metodo),
      datasets: [
        {
          data: datos.map((d) => d.total),
          backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444'],
        },
      ],
    };
  });

  protected readonly opcionesBarras = {
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };
  protected readonly opcionesDona = {
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: { legend: { position: 'bottom' } },
  };

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiar(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio] = valor ?? [];
    if (inicio) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    if (inicio) {
      this.store.cargarReporte(toIsoDate(inicio), toIsoDate(fin ?? inicio));
    }
  }
}
