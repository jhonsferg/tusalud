import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Rating } from 'primeng/rating';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';

import { CitaResponse } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { CitasStore, esCitaActiva } from '@tusalud/app/core/state/citas.store';
import { ClinicaStore } from '@tusalud/app/core/state/clinica.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { EstadoCitaTag } from '@tusalud/app/shared/components/estado-cita-tag/estado-cita-tag';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { PagoDialog } from '@tusalud/app/shared/components/pago-dialog/pago-dialog';
import { ReprogramarDialog } from '@tusalud/app/shared/components/reprogramar-dialog/reprogramar-dialog';
import { ValoracionDialog } from '@tusalud/app/shared/components/valoracion-dialog/valoracion-dialog';
import { CitaActions } from '@tusalud/app/shared/services/cita-actions.service';

@Component({
  selector: 'app-cita-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    RouterLink,
    Button,
    Rating,
    Skeleton,
    Tag,
    EmptyState,
    EstadoCitaTag,
    PageHeader,
    PagoDialog,
    ReprogramarDialog,
    ValoracionDialog,
  ],
  templateUrl: './cita-detail.html',
  styleUrl: './cita-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaDetail {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(CitasStore);
  protected readonly clinica = inject(ClinicaStore);
  protected readonly auth = inject(AuthService);
  private readonly acciones = inject(CitaActions);

  protected readonly pagoVisible = signal(false);
  protected readonly reprogramarVisible = signal(false);
  protected readonly valoracionVisible = signal(false);

  protected readonly volver = computed(() =>
    this.auth.isPaciente() ? '/citas' : this.auth.isMedico() ? '/agenda' : '/admin/auditoria',
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => {
          this.store.selected.reset();
          this.clinica.atencion.reset();
          this.clinica.valoracion.reset();
          this.store.loadOne(id);
        });
      }
    });
    effect(() => {
      const cita = this.store.selected.value();
      if (cita?.estado === 'ATENDIDA') {
        untracked(() => {
          this.clinica.cargarAtencion(cita.id);
          if (this.auth.isPaciente()) {
            this.clinica.cargarValoracion(cita.id);
          }
        });
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

  protected puedeReprogramar(cita: CitaResponse): boolean {
    return (
      esCitaActiva(cita) &&
      (this.auth.isAdmin() || this.auth.isPaciente()) &&
      new Date(cita.fechaHora).getTime() > Date.now()
    );
  }

  protected puedePagar(cita: CitaResponse): boolean {
    return this.auth.isPaciente() && esCitaActiva(cita) && !cita.pagado;
  }

  protected puedeConfirmar(cita: CitaResponse): boolean {
    return cita.estado === 'PENDIENTE' && (this.auth.isMedico() || this.auth.isAdmin());
  }

  protected puedeAtender(cita: CitaResponse): boolean {
    return this.auth.isMedico() && (esCitaActiva(cita) || cita.estado === 'ATENDIDA');
  }

  protected puedeValorar(cita: CitaResponse): boolean {
    return (
      this.auth.isPaciente() && cita.estado === 'ATENDIDA' && !this.clinica.valoracion.hasValue()
    );
  }

  protected cancelar(cita: CitaResponse): void {
    this.acciones.ejecutar('cancelar', cita);
  }

  protected confirmar(cita: CitaResponse): void {
    this.acciones.ejecutar('confirmar', cita);
  }
}
