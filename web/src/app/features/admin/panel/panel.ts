import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';
import { Skeleton } from 'primeng/skeleton';

import { AdminStore } from '@tusalud/app/core/state/admin.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { UserAvatar } from '@tusalud/app/shared/components/user-avatar/user-avatar';

const COLORES = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#ec4899'];
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

@Component({
  selector: 'app-panel',
  imports: [
    CurrencyPipe,
    RouterLink,
    Button,
    UIChart,
    Skeleton,
    EmptyState,
    PageHeader,
    UserAvatar,
  ],
  templateUrl: './panel.html',
  styleUrl: './panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Panel implements OnInit {
  protected readonly store = inject(AdminStore);

  protected readonly graficoIngresos = computed(() => {
    const datos = this.store.estadisticas.value()?.ingresosPorMes ?? [];
    return {
      labels: datos.map((item) => {
        const [anio, mes] = item.mes.split('-');
        return `${MESES[Number(mes) - 1]} ${anio.slice(2)}`;
      }),
      datasets: [
        {
          label: 'Ingresos',
          data: datos.map((item) => item.total),
          backgroundColor: '#3b82f6',
          borderRadius: 6,
        },
      ],
    };
  });

  protected readonly graficoEspecialidades = computed(() => {
    const datos = this.store.estadisticas.value()?.citasPorEspecialidad ?? [];
    return {
      labels: datos.map((item) => item.especialidad),
      datasets: [
        {
          data: datos.map((item) => item.cantidad),
          backgroundColor: datos.map((_, i) => COLORES[i % COLORES.length]),
          borderWidth: 0,
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
    this.store.cargarEstadisticas();
  }
}
