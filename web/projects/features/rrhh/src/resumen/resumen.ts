import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';

import {
  AuthService,
  ETIQUETA_AREA_LABORAL,
  ETIQUETA_TIPO_AUSENCIA,
  RrhhStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-resumen-rrhh',
  imports: [CurrencyPipe, DatePipe, RouterLink, UIChart, TableModule, EmptyState, Kpi, PageHeader],
  templateUrl: './resumen.html',
  styleUrl: './resumen.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResumenRrhh implements OnInit {
  protected readonly store = inject(RrhhStore);
  protected readonly auth = inject(AuthService);
  protected readonly etiquetaArea = ETIQUETA_AREA_LABORAL;
  protected readonly etiquetaTipo = ETIQUETA_TIPO_AUSENCIA;
  protected readonly puedeGestionar = computed(() => this.auth.isRrhh() || this.auth.isAdmin());
  protected readonly opciones = {
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  protected readonly grafico = computed(() => {
    const areas = [...(this.store.resumen.value()?.porArea ?? [])].sort(
      (a, b) => b.total - a.total,
    );
    return {
      labels: areas.map((a) => this.etiquetaArea[a.area] ?? a.area),
      datasets: [{ data: areas.map((a) => a.total), backgroundColor: '#3b82f6', borderRadius: 6 }],
    };
  });

  ngOnInit(): void {
    this.store.cargarResumen();
  }
}
