import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';

import { CitaResponse } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { CitasStore, esCitaActiva } from '@tusalud/app/core/state/citas.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { EstadoCitaTag } from '@tusalud/app/shared/components/estado-cita-tag/estado-cita-tag';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { CitaActions } from '@tusalud/app/shared/services/cita-actions.service';

@Component({
  selector: 'app-cita-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    Button,
    Skeleton,
    EmptyState,
    EstadoCitaTag,
    PageHeader,
  ],
  templateUrl: './cita-detail.html',
  styleUrl: './cita-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaDetail {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(CitasStore);
  protected readonly auth = inject(AuthService);
  private readonly acciones = inject(CitaActions);

  protected readonly volver = computed(() =>
    this.auth.isPaciente() ? '/citas' : this.auth.isMedico() ? '/agenda' : '/admin/auditoria',
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
      }
    });
  }

  protected puedeCancelar(cita: CitaResponse): boolean {
    if (!esCitaActiva(cita)) {
      return false;
    }
    if (this.auth.isAdmin()) {
      return true;
    }
    return this.auth.isPaciente() && new Date(cita.fechaHora).getTime() > Date.now();
  }

  protected puedeConfirmar(cita: CitaResponse): boolean {
    return cita.estado === 'PENDIENTE' && (this.auth.isMedico() || this.auth.isAdmin());
  }

  protected puedeAtender(cita: CitaResponse): boolean {
    return esCitaActiva(cita) && this.auth.isMedico();
  }

  protected cancelar(cita: CitaResponse): void {
    this.acciones.ejecutar('cancelar', cita);
  }

  protected confirmar(cita: CitaResponse): void {
    this.acciones.ejecutar('confirmar', cita);
  }

  protected atender(cita: CitaResponse): void {
    this.acciones.ejecutar('atender', cita);
  }
}
