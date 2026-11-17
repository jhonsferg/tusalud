import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Button } from 'primeng/button';

import { RrhhStore, toIsoDate } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { horaCorta, lunesDe, sumarDias } from '../shared/ausencia';

@Component({
  selector: 'app-mis-turnos',
  imports: [DatePipe, Button, EmptyState],
  template: `
    <div class="barra">
      <p-button
        icon="pi pi-chevron-left"
        [text]="true"
        [rounded]="true"
        ariaLabel="Semana anterior"
        (onClick)="mover(-7)"
      />
      <strong>{{ inicio() | date: 'd MMM' }} al {{ fin() | date: 'd MMM y' }}</strong>
      <p-button
        icon="pi pi-chevron-right"
        [text]="true"
        [rounded]="true"
        ariaLabel="Semana siguiente"
        (onClick)="mover(7)"
      />
      <p-button label="Esta semana" [outlined]="true" size="small" (onClick)="hoy()" />
    </div>

    @if (sinFicha()) {
      <div class="panel">
        <app-empty-state
          icon="pi-id-card"
          title="Aún no tienes una ficha laboral"
          message="Recursos humanos registra tu ficha y asigna tus turnos. Consúltalos con ellos si crees que falta."
        />
      </div>
    } @else {
      <div class="panel">
        <ul class="turnos">
          @for (turno of store.misTurnos.value() ?? []; track turno.id) {
            <li>
              <span class="fecha">{{ turno.fecha | date: 'EEEE d MMM' }}</span>
              <strong>{{ turno.plantillaNombre }}</strong>
              <span class="text-muted"
                >{{ hora(turno.horaInicio) }} a {{ hora(turno.horaFin) }} ·
                {{ turno.horas }} h</span
              >
              @if (turno.notas) {
                <small class="text-muted">{{ turno.notas }}</small>
              }
            </li>
          } @empty {
            <li class="vacio text-muted">No tienes turnos asignados en esta semana.</li>
          }
        </ul>
      </div>
    }
  `,
  styles: `
    .barra {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }

    .turnos {
      display: flex;
      flex-direction: column;
      gap: 0.6rem;
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.9rem;
        padding: 0.7rem 1rem;
        border-radius: 10px;
        background: var(--p-content-hover-background);

        &.vacio {
          background: transparent;
        }
      }
    }

    .fecha {
      min-width: 9rem;
      text-transform: capitalize;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisTurnos implements OnInit {
  protected readonly store = inject(RrhhStore);
  protected readonly hora = horaCorta;
  protected readonly inicio = signal(lunesDe(new Date()));
  protected readonly fin = computed(() => sumarDias(this.inicio(), 6));
  protected readonly sinFicha = computed(() => this.store.misTurnos.error()?.status === 404);

  ngOnInit(): void {
    this.cargar();
  }

  protected mover(dias: number): void {
    this.inicio.set(sumarDias(this.inicio(), dias));
    this.cargar();
  }

  protected hoy(): void {
    this.inicio.set(lunesDe(new Date()));
    this.cargar();
  }

  private cargar(): void {
    this.store.cargarMisTurnos(toIsoDate(this.inicio()), toIsoDate(this.fin()));
  }
}
