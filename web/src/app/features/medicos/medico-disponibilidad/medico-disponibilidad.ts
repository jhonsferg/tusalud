import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Skeleton } from 'primeng/skeleton';

import { AuthService } from '@tusalud/app/core/services/auth.service';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-medico-disponibilidad',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    DatePicker,
    Skeleton,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './medico-disponibilidad.html',
  styleUrl: './medico-disponibilidad.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDisponibilidad {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(MedicosStore);
  protected readonly auth = inject(AuthService);

  protected readonly minimo = new Date();
  protected readonly fecha = signal<Date>(new Date());

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
      }
    });
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.loadDisponibilidad(id, toIsoDate(this.fecha()));
      }
    });
  }

  protected cambiarFecha(valor: Date | null): void {
    if (valor) {
      this.fecha.set(valor);
    }
  }

  protected fechaIso(): string {
    return toIsoDate(this.fecha());
  }

  protected recargar(): void {
    const id = this.id();
    if (id !== undefined) {
      this.store.loadDisponibilidad(id, this.fechaIso());
    }
  }
}
