import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { UIChart } from 'primeng/chart';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';

import { ESTADOS_CITA } from '@tusalud/app/core/models';
import { AuditoriaStore } from '@tusalud/app/core/state/auditoria.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { EstadoCitaTag } from '@tusalud/app/shared/components/estado-cita-tag/estado-cita-tag';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { COLOR_ESTADO, ETIQUETA_ESTADO, ICONO_ESTADO } from '@tusalud/app/shared/utils/cita-estado';

@Component({
  selector: 'app-auditoria',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    TableModule,
    UIChart,
    EmptyState,
    EstadoCitaTag,
    PageHeader,
  ],
  templateUrl: './auditoria.html',
  styleUrl: './auditoria.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Auditoria implements OnInit {
  protected readonly store = inject(AuditoriaStore);

  protected readonly rango = signal<Date[] | null>(null);
  protected readonly etiqueta = ETIQUETA_ESTADO;
  protected readonly icono = ICONO_ESTADO;
  protected readonly color = COLOR_ESTADO;

  protected readonly grafico = computed(() => ({
    labels: this.store.resumen().map((item) => ETIQUETA_ESTADO[item.estado]),
    datasets: [
      {
        data: this.store.resumen().map((item) => item.cantidad),
        backgroundColor: ESTADOS_CITA.map((estado) => COLOR_ESTADO[estado]),
        borderWidth: 0,
      },
    ],
  }));

  protected readonly opcionesGrafico = {
    maintainAspectRatio: false,
    cutout: '65%',
    plugins: { legend: { position: 'bottom' } },
  };

  ngOnInit(): void {
    this.store.loadPorEstado();
    this.cargarCitas();
  }

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio] = valor ?? [];
    if (!valor || inicio) {
      this.cargarCitas();
    }
  }

  protected cargarCitas(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.loadCitas({
      inicio: inicio ? toIsoDate(inicio) : null,
      fin: fin ? toIsoDate(fin) : inicio ? toIsoDate(inicio) : null,
    });
  }

  protected recargar(): void {
    this.store.loadPorEstado();
    this.cargarCitas();
  }
}
