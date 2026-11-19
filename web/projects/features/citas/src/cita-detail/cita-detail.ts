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

import {
  AuthService,
  CitaResponse,
  CitasStore,
  ClinicaStore,
  esCitaActiva,
  optionalIdAttribute,
} from '@tusalud/core';
import { CitaActions } from '@tusalud/ui/services';
import { EmptyState } from '@tusalud/ui/empty-state';
import { EstadoCitaTag } from '@tusalud/ui/estado-cita-tag';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { PageHeader } from '@tusalud/ui/page-header';
import { PagoDialog } from '@tusalud/ui/pago-dialog';
import { ValoracionDialog } from '@tusalud/ui/valoracion-dialog';
import { ListaDiagnosticos } from '@tusalud/ui/selector-cie10';

@Component({
  selector: 'app-cita-detail',
  imports: [
    BotonDocumento,
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
    ValoracionDialog,
    ListaDiagnosticos,
  ],
  templateUrl: './cita-detail.html',
  styleUrl: './cita-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaDetail {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(CitasStore);
  protected readonly clinica = inject(ClinicaStore);
  protected readonly auth = inject(AuthService);
  private readonly acciones = inject(CitaActions);

  protected readonly pagoVisible = signal(false);
  protected readonly valoracionVisible = signal(false);

  protected readonly puedeVerClinica = computed(
    () => this.auth.isPaciente() || this.auth.isMedico(),
  );
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
          if (this.puedeVerClinica()) {
            this.clinica.cargarAtencion(cita.id);
          }
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
    return (
      (this.auth.isPaciente() || this.auth.isMedico()) &&
      new Date(cita.fechaHora).getTime() > Date.now()
    );
  }

  protected puedeReprogramar(cita: CitaResponse): boolean {
    return (
      esCitaActiva(cita) &&
      (this.auth.isAdmin() || this.auth.isPaciente()) &&
      new Date(cita.fechaHora).getTime() > Date.now()
    );
  }

  protected puedeConstancia(cita: CitaResponse): boolean {
    return (this.auth.isPaciente() || this.auth.isMedico()) && cita.estado !== 'CANCELADA';
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
    this.acciones.cancelar(cita);
  }

  protected confirmar(cita: CitaResponse): void {
    this.acciones.ejecutar('confirmar', cita);
  }
}
