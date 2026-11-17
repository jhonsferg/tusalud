import { DatePipe } from '@angular/common';
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
import { Skeleton } from 'primeng/skeleton';

import {
  addDays,
  CitaResponse,
  CitasStore,
  esCitaActiva,
  parseIsoDate,
  today,
  toIsoDate,
} from '@tusalud/core';
import { Calendario, CalEvento, RangoVisible } from '@tusalud/ui/calendario';
import { CitaActions } from '@tusalud/ui/services';
import { citaAEvento } from '@tusalud/ui/utils';
import { EmptyState } from '@tusalud/ui/empty-state';
import { EstadoCitaTag } from '@tusalud/ui/estado-cita-tag';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-agenda',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    SelectButton,
    Skeleton,
    Calendario,
    EmptyState,
    EstadoCitaTag,
    PageHeader,
  ],
  templateUrl: './agenda.html',
  styleUrl: './agenda.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Agenda implements OnInit {
  protected readonly store = inject(CitasStore);
  private readonly acciones = inject(CitaActions);
  private readonly router = inject(Router);

  protected readonly modo = signal<'lista' | 'calendario'>('lista');
  protected readonly modos = [
    { label: 'Día', value: 'lista', icon: 'pi pi-list' },
    { label: 'Calendario', value: 'calendario', icon: 'pi pi-calendar' },
  ];
  protected readonly eventos = computed(() =>
    (this.store.agendaRango.value() ?? []).map((cita) => citaAEvento(cita, 'paciente')),
  );

  protected readonly fecha = signal(today());
  protected readonly fechaDate = computed(() => parseIsoDate(this.fecha()) ?? new Date());
  protected readonly esHoy = computed(() => this.fecha() === today());
  protected readonly activas = computed(() => this.store.citasAgenda().filter(esCitaActiva).length);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargarRango(rango: RangoVisible): void {
    this.store.loadAgendaRango(toIsoDate(rango.inicio), toIsoDate(rango.fin));
  }

  protected abrirEvento(evento: CalEvento): void {
    void this.router.navigate(['/citas', evento.id]);
  }

  protected cargar(): void {
    this.store.loadAgenda(this.fecha());
  }

  protected irA(fecha: string): void {
    this.fecha.set(fecha);
    this.cargar();
  }

  protected irAHoy(): void {
    this.irA(today());
  }

  protected mover(dias: number): void {
    this.irA(addDays(this.fecha(), dias));
  }

  protected elegir(valor: Date | null): void {
    if (valor) {
      this.irA(toIsoDate(valor));
    }
  }

  protected confirmar(cita: CitaResponse): void {
    this.acciones.ejecutar('confirmar', cita);
  }

  protected atender(cita: CitaResponse): void {
    this.acciones.ejecutar('atender', cita);
  }
}
