import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { ApiError, DIAS_SEMANA, HorarioResponse, NOMBRE_DIA } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { optionalIdAttribute } from '@tusalud/app/core/utils/route-input';
import {
  AppValidators,
  DURACION_MAX,
  DURACION_MIN,
} from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { firstErrorMessage } from '@tusalud/app/core/validators/validation-messages';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-medico-horarios',
  imports: [
    ReactiveFormsModule,
    Button,
    InputNumber,
    Select,
    Skeleton,
    TableModule,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './medico-horarios.html',
  styleUrl: './medico-horarios.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoHorarios {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(MedicosStore);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);

  protected readonly submitting = signal(false);
  protected readonly nombreDia: Readonly<Record<string, string>> = NOMBRE_DIA;
  protected readonly dias = DIAS_SEMANA.map((dia) => ({ value: dia, label: NOMBRE_DIA[dia] }));
  protected readonly duracionMin = DURACION_MIN;
  protected readonly duracionMax = DURACION_MAX;

  protected readonly form = inject(FormBuilder).group(
    {
      diaSemana: [null as (typeof DIAS_SEMANA)[number] | null, [Validators.required]],
      horaInicio: ['08:00', [Validators.required, AppValidators.hora]],
      horaFin: ['13:00', [Validators.required, AppValidators.hora]],
      duracionCitaMinutos: [30, [Validators.required, AppValidators.duracionCita]],
    },
    { validators: [AppValidators.timeRange('horaInicio', 'horaFin', 'duracionCitaMinutos')] },
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
        this.store.loadHorarios(id);
      }
    });
  }

  protected errorDeGrupo(): string | null {
    return this.form.touched || this.form.dirty ? firstErrorMessage(this.form.errors) : null;
  }

  protected agregar(): void {
    const id = this.id();
    this.form.markAllAsTouched();
    if (id === undefined || this.form.invalid) {
      return;
    }
    const valor = this.form.getRawValue();
    this.submitting.set(true);
    this.store
      .agregarHorario(id, {
        diaSemana: valor.diaSemana!,
        horaInicio: valor.horaInicio!,
        horaFin: valor.horaFin!,
        duracionCitaMinutos: valor.duracionCitaMinutos!,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.notifications.success('El bloque de atención fue agregado.');
          this.form.controls.diaSemana.reset(null);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          applyServerErrors(this.form, error);
        },
      });
  }

  protected eliminar(horario: HorarioResponse): void {
    const id = this.id();
    if (id === undefined) {
      return;
    }
    this.confirmation.confirm({
      header: 'Eliminar bloque de atención',
      message: `¿Quieres quitar el bloque del ${NOMBRE_DIA[horario.diaSemana].toLowerCase()} de ${horario.horaInicio.slice(0, 5)} a ${horario.horaFin.slice(0, 5)}?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminarHorario(id, horario.id).subscribe({
          next: () => this.notifications.success('El bloque de atención fue eliminado.'),
          error: () => undefined,
        }),
    });
  }
}
