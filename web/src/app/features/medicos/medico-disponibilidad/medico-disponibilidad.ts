import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

import {
  AuthService,
  CitasStore,
  MedicosStore,
  optionalIdAttribute,
  toIsoDate,
} from '@tusalud/core';
import { Calendario, CalEvento, RangoVisible, VistaCalendario } from '@tusalud/ui/calendario';
import { citaAEvento, construirHuecos, horarioLaboral } from '@tusalud/ui/utils';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-medico-disponibilidad',
  imports: [DatePipe, Button, Calendario, PageHeader],
  templateUrl: './medico-disponibilidad.html',
  styleUrl: './medico-disponibilidad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDisponibilidad {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(MedicosStore);
  protected readonly auth = inject(AuthService);
  private readonly citas = inject(CitasStore);
  private readonly router = inject(Router);

  protected readonly vista = signal<VistaCalendario>('semana');
  protected readonly fecha = signal(new Date());
  protected readonly elegido = signal<Date | null>(null);
  private readonly rango = signal<RangoVisible | null>(null);

  protected readonly huecos = computed(() =>
    construirHuecos(this.store.disponibilidadRango.value() ?? []),
  );
  protected readonly laboral = computed(() => horarioLaboral(this.store.horarios.value() ?? []));
  protected readonly propios = computed<CalEvento[]>(() =>
    this.auth.isPaciente()
      ? this.citas.proximas().map((cita) => ({
          ...citaAEvento(cita, 'medico'),
          titulo: 'Tu cita',
          detalle: cita.medicoNombre,
          color: 'secondary' as const,
        }))
      : [],
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
        this.store.loadHorarios(id);
        this.elegido.set(null);
      }
    });
    effect(() => {
      if (this.auth.isPaciente()) {
        untracked(() => this.citas.loadMisCitas());
      }
    });
    effect(() => {
      const id = this.id();
      const rango = this.rango();
      if (id !== undefined && rango) {
        untracked(() =>
          this.store.loadDisponibilidadRango(id, toIsoDate(rango.inicio), toIsoDate(rango.fin)),
        );
      }
    });
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rango.set(rango);
  }

  protected reservar(): void {
    const inicio = this.elegido();
    const id = this.id();
    if (!inicio || id === undefined) {
      return;
    }
    void this.router.navigate(['/citas/nueva'], {
      queryParams: {
        medicoId: id,
        fecha: toIsoDate(inicio),
        hora: `${String(inicio.getHours()).padStart(2, '0')}:${String(inicio.getMinutes()).padStart(2, '0')}`,
      },
    });
  }
}
