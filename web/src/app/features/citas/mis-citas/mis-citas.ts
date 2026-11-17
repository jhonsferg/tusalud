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
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { SelectButton } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';

import { CitaResponse, ESTADOS_CITA } from '@tusalud/app/core/models';
import { CitasStore, esCitaActiva } from '@tusalud/app/core/state/citas.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { Calendario } from '@tusalud/app/shared/components/calendario/calendario';
import { CalEvento } from '@tusalud/app/shared/components/calendario/calendario.utils';
import { citaAEvento } from '@tusalud/app/shared/utils/cita-calendario';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { EstadoCitaTag } from '@tusalud/app/shared/components/estado-cita-tag/estado-cita-tag';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { CitaActions } from '@tusalud/app/shared/services/cita-actions.service';
import { ETIQUETA_ESTADO } from '@tusalud/app/shared/utils/cita-estado';

type Vista = 'proximas' | 'historial' | 'todas';

@Component({
  selector: 'app-mis-citas',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    SelectButton,
    TableModule,
    Calendario,
    EmptyState,
    EstadoCitaTag,
    PageHeader,
  ],
  templateUrl: './mis-citas.html',
  styleUrl: './mis-citas.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisCitas implements OnInit {
  protected readonly store = inject(CitasStore);
  private readonly acciones = inject(CitaActions);
  private readonly router = inject(Router);

  protected readonly modo = signal<'lista' | 'calendario'>('lista');
  protected readonly modos = [
    { label: 'Lista', value: 'lista', icon: 'pi pi-list' },
    { label: 'Calendario', value: 'calendario', icon: 'pi pi-calendar' },
  ];
  protected readonly eventos = computed(() =>
    this.store.citas().map((cita) => citaAEvento(cita, 'medico')),
  );

  protected readonly vista = signal<Vista>('proximas');
  protected readonly rango = signal<Date[] | null>(null);
  protected readonly estados = ESTADOS_CITA;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO;
  protected readonly opciones = [
    { label: 'Próximas', value: 'proximas' },
    { label: 'Historial', value: 'historial' },
    { label: 'Todas', value: 'todas' },
  ];

  protected readonly visibles = computed<CitaResponse[]>(() => {
    switch (this.vista()) {
      case 'proximas':
        return this.store.proximas();
      case 'historial':
        return [...this.store.historial()].reverse();
      default:
        return this.store.citas();
    }
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio, fin] = valor ?? [];
    if (!valor || (inicio && fin)) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.loadMisCitas({
      inicio: inicio ? toIsoDate(inicio) : null,
      fin: fin ? toIsoDate(fin) : null,
    });
  }

  protected puedeCancelar(cita: CitaResponse): boolean {
    return esCitaActiva(cita) && new Date(cita.fechaHora).getTime() > Date.now();
  }

  protected abrirEvento(evento: CalEvento): void {
    void this.router.navigate(['/citas', evento.id]);
  }

  protected cancelar(cita: CitaResponse): void {
    this.acciones.ejecutar('cancelar', cita);
  }
}
