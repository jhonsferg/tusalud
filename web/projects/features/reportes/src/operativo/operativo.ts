import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { UIChart } from 'primeng/chart';
import { TableModule } from 'primeng/table';

import { NOMBRES_DIAS_SEMANA, ETIQUETA_AREA, FiltroReporte, ReportesStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';
import { FiltrosReporte } from '../shared/filtros-reporte/filtros-reporte';
import {
  COLOR_TRIAJE_NIVEL,
  OPCIONES_APILADAS,
  OPCIONES_BARRAS,
  OPCIONES_DONA,
  OPCIONES_LINEA,
  etiquetaFecha,
  horaDelDia,
} from '../shared/formato';

interface CeldaCalor {
  citas: number;
  intensidad: number;
}

@Component({
  selector: 'app-reporte-operativo',
  imports: [DecimalPipe, UIChart, TableModule, EmptyState, Kpi, PageHeader, FiltrosReporte],
  templateUrl: './operativo.html',
  styleUrl: './operativo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReporteOperativo {
  protected readonly store = inject(ReportesStore);
  protected readonly etiquetaArea = ETIQUETA_AREA;
  protected readonly dias = NOMBRES_DIAS_SEMANA;
  protected readonly horaDelDia = horaDelDia;
  protected readonly opcionesApiladas = OPCIONES_APILADAS;
  protected readonly opcionesBarras = OPCIONES_BARRAS;
  protected readonly opcionesDona = OPCIONES_DONA;
  protected readonly opcionesLinea = OPCIONES_LINEA;

  protected readonly graficoCamas = computed(() => {
    const areas = this.store.operativo.value()?.camasActuales ?? [];
    const nombre = (area: string): string => this.etiquetaArea[area] ?? area;
    return {
      labels: areas.map((a) => nombre(a.area)),
      datasets: [
        { label: 'Ocupadas', data: areas.map((a) => a.ocupadas), backgroundColor: '#ef4444' },
        { label: 'Libres', data: areas.map((a) => a.libres), backgroundColor: '#10b981' },
        { label: 'Limpieza', data: areas.map((a) => a.limpieza), backgroundColor: '#f59e0b' },
        {
          label: 'Mantenimiento',
          data: areas.map((a) => a.mantenimiento),
          backgroundColor: '#64748b',
        },
      ],
    };
  });

  protected readonly graficoOcupacion = computed(() => {
    const serie = this.store.operativo.value()?.ocupacionSerie ?? [];
    return {
      labels: serie.map((p) => etiquetaFecha(p.fecha)),
      datasets: [
        {
          label: 'Ocupación %',
          data: serie.map((p) => p.porcentaje),
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.15)',
          fill: true,
        },
      ],
    };
  });

  protected readonly graficoEsperas = computed(() => {
    const horas = this.store.operativo.value()?.esperas.porHora ?? [];
    return {
      labels: horas.map((h) => horaDelDia(h.hora)),
      datasets: [
        {
          label: 'Espera promedio (min)',
          data: horas.map((h) => h.promedioMin),
          backgroundColor: '#f59e0b',
          borderRadius: 6,
        },
      ],
    };
  });

  protected readonly graficoEmergencias = computed(() => {
    const niveles = this.store.operativo.value()?.emergenciasPorNivel ?? [];
    return {
      labels: niveles.map((n) => 'Nivel ' + n.nivel + ' ' + n.clasificacion),
      datasets: [
        {
          data: niveles.map((n) => n.cantidad),
          backgroundColor: niveles.map((n) => COLOR_TRIAJE_NIVEL[n.nivel] ?? '#64748b'),
        },
      ],
    };
  });

  protected readonly horasCalor = computed(() => {
    const demanda = this.store.operativo.value()?.demanda ?? [];
    const horas = demanda.map((d) => d.hora);
    const minima = Math.min(7, ...(horas.length > 0 ? horas : [7]));
    const maxima = Math.max(19, ...(horas.length > 0 ? horas : [19]));
    return Array.from({ length: maxima - minima + 1 }, (_, i) => minima + i);
  });

  protected readonly calor = computed(() => {
    const demanda = this.store.operativo.value()?.demanda ?? [];
    const mayor = Math.max(1, ...demanda.map((d) => d.citas));
    const mapa = new Map<string, CeldaCalor>();
    for (const celda of demanda) {
      mapa.set(celda.diaSemana + '-' + celda.hora, {
        citas: celda.citas,
        intensidad: celda.citas / mayor,
      });
    }
    return mapa;
  });

  protected celda(dia: number, hora: number): CeldaCalor {
    return this.calor().get(dia + '-' + hora) ?? { citas: 0, intensidad: 0 };
  }

  protected cargar(filtro: FiltroReporte): void {
    this.store.cargarOperativo(filtro);
  }
}
