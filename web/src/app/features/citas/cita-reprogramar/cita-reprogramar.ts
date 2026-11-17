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
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Skeleton } from 'primeng/skeleton';

import { ApiError } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CitasStore, esCitaActiva } from '@tusalud/app/core/state/citas.store';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { toIsoDate, toIsoDateTime } from '@tusalud/app/core/utils/date';
import { optionalIdAttribute } from '@tusalud/app/core/utils/route-input';
import { Calendario } from '@tusalud/app/shared/components/calendario/calendario';
import {
  CalEvento,
  RangoVisible,
  VistaCalendario,
} from '@tusalud/app/shared/components/calendario/calendario.utils';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { citaAEvento } from '@tusalud/app/shared/utils/cita-calendario';
import {
  construirHuecos,
  horarioLaboral,
} from '@tusalud/app/shared/utils/disponibilidad-calendario';

@Component({
  selector: 'app-cita-reprogramar',
  imports: [DatePipe, RouterLink, Button, Message, Skeleton, Calendario, EmptyState, PageHeader],
  templateUrl: './cita-reprogramar.html',
  styleUrl: './cita-reprogramar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaReprogramar {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly citas = inject(CitasStore);
  protected readonly medicos = inject(MedicosStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly vista = signal<VistaCalendario>('semana');
  protected readonly fecha = signal(new Date());
  protected readonly elegido = signal<Date | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly rango = signal<RangoVisible | null>(null);

  protected readonly cita = this.citas.selected.value;
  protected readonly reprogramable = computed(() => {
    const cita = this.cita();
    return !!cita && esCitaActiva(cita) && new Date(cita.fechaHora).getTime() > Date.now();
  });

  protected readonly huecos = computed(() =>
    construirHuecos(this.medicos.disponibilidadRango.value() ?? []),
  );
  protected readonly laboral = computed(() => horarioLaboral(this.medicos.horarios.value() ?? []));

  protected readonly eventos = computed<CalEvento[]>(() => {
    const actual = this.cita();
    if (!actual) {
      return [];
    }
    const propios = this.auth.isPaciente()
      ? this.citas
          .proximas()
          .filter((cita) => cita.id !== actual.id)
          .map<CalEvento>((cita) => ({
            ...citaAEvento(cita, 'medico'),
            titulo: 'Tu cita',
            detalle: cita.medicoNombre,
            color: 'secondary',
          }))
      : [];
    return [
      ...propios,
      {
        ...citaAEvento(actual, 'medico'),
        titulo: 'Cita actual',
        color: 'warn',
        ignoraCruce: true,
      },
    ];
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.citas.selected.reset();
          this.citas.loadOne(id);
          if (this.auth.isPaciente()) {
            this.citas.loadMisCitas();
          }
        });
      }
    });
    effect(() => {
      const cita = this.cita();
      if (cita) {
        untracked(() => {
          this.medicos.loadHorarios(cita.medicoId);
          this.fecha.set(new Date(cita.fechaHora));
        });
      }
    });
    effect(() => {
      const cita = this.cita();
      const rango = this.rango();
      if (cita && rango) {
        untracked(() =>
          this.medicos.loadDisponibilidadRango(
            cita.medicoId,
            toIsoDate(rango.inicio),
            toIsoDate(rango.fin),
          ),
        );
      }
    });
  }

  protected cargarRango(rango: RangoVisible): void {
    this.rango.set(rango);
  }

  protected confirmar(): void {
    const cita = this.cita();
    const nueva = this.elegido();
    if (!cita || !nueva) {
      return;
    }
    this.error.set(null);
    this.guardando.set(true);
    this.citas.reprogramar(cita.id, toIsoDateTime(nueva)).subscribe({
      next: () => {
        this.guardando.set(false);
        this.notifications.success('Tu cita fue reprogramada. Quedó pendiente de confirmación.');
        void this.router.navigate(['/citas', cita.id]);
      },
      error: (error: ApiError) => {
        this.guardando.set(false);
        this.error.set(error.message);
        this.elegido.set(null);
        const rango = this.rango();
        if (rango) {
          this.medicos.loadDisponibilidadRango(
            cita.medicoId,
            toIsoDate(rango.inicio),
            toIsoDate(rango.fin),
          );
        }
      },
    });
  }
}
