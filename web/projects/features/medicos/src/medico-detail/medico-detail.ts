import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Dialog } from 'primeng/dialog';
import { MultiSelect } from 'primeng/multiselect';
import { Rating } from 'primeng/rating';
import { Button } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ClinicaStore,
  EspecialidadesStore,
  MedicoResponse,
  MedicosStore,
  NOMBRE_DIA,
  NotificationService,
  optionalIdAttribute,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-medico-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Dialog,
    MultiSelect,
    Rating,
    RouterLink,
    Button,
    Skeleton,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './medico-detail.html',
  styleUrl: './medico-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDetail {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(MedicosStore);
  protected readonly clinica = inject(ClinicaStore);
  protected readonly auth = inject(AuthService);
  protected readonly especialidades = inject(EspecialidadesStore);
  private readonly notifications = inject(NotificationService);
  protected readonly nombreDia = NOMBRE_DIA;

  protected readonly editando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly seleccion = signal<string[]>([]);

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
        this.store.loadHorarios(id);
        this.clinica.cargarValoracionesDeMedico(id);
      }
    });
  }

  protected abrirEspecialidades(medico: MedicoResponse): void {
    this.especialidades.load();
    this.seleccion.set(medico.especialidades.map((especialidad) => especialidad.id));
    this.editando.set(true);
  }

  protected guardarEspecialidades(medico: MedicoResponse): void {
    this.guardando.set(true);
    this.store.actualizarEspecialidades(medico.id, this.seleccion()).subscribe({
      next: () => {
        this.guardando.set(false);
        this.editando.set(false);
        this.notifications.success('Las especialidades del médico fueron actualizadas.');
      },
      error: () => this.guardando.set(false),
    });
  }
}
