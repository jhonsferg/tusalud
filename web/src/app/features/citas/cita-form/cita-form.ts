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
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { startWith } from 'rxjs';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { Textarea } from 'primeng/textarea';

import { ApiError } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { CitasStore } from '@tusalud/app/core/state/citas.store';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { combineDateAndTime, parseIsoDate, toIsoDate } from '@tusalud/app/core/utils/date';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { AppValidators } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

const MOTIVO_MAX = 500;

@Component({
  selector: 'app-cita-form',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    DatePicker,
    Select,
    Skeleton,
    Textarea,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './cita-form.html',
  styleUrl: './cita-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CitaForm {
  readonly medicoId = input(undefined, { transform: optionalNumberAttribute });
  readonly fecha = input<string>();
  readonly hora = input<string>();

  protected readonly medicos = inject(MedicosStore);
  private readonly citas = inject(CitasStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly minimo = new Date();
  protected readonly motivoMax = MOTIVO_MAX;

  protected readonly form = inject(FormBuilder).group({
    medicoId: [null as number | null, [Validators.required]],
    fecha: [null as Date | null, [Validators.required]],
    hora: [null as string | null, [Validators.required]],
    motivo: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(MOTIVO_MAX)]],
  });

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.value)), {
    initialValue: this.form.value,
  });

  protected readonly opcionesMedico = computed(() =>
    this.medicos.medicosActivos().map((medico) => ({
      value: medico.id,
      label: `${medico.nombreCompleto} · ${medico.especialidadNombre}`,
    })),
  );

  protected readonly medicoElegido = computed(() =>
    this.medicos.medicosActivos().find((medico) => medico.id === this.valores().medicoId),
  );

  protected readonly horas = computed(() =>
    this.medicos.horasDisponibles().map((hora) => hora.slice(0, 5)),
  );

  protected readonly fechaHora = computed(() => {
    const { fecha, hora } = this.valores();
    return fecha && hora ? combineDateAndTime(toIsoDate(fecha), hora) : null;
  });

  constructor() {
    this.medicos.load(null);

    effect(() => {
      const medicoId = this.medicoId();
      const fechaParam = this.fecha();
      const horaParam = this.hora();
      untracked(() => {
        if (medicoId !== undefined) {
          this.form.controls.medicoId.setValue(medicoId);
        }
        const fecha = fechaParam ? parseIsoDate(fechaParam) : null;
        if (fecha) {
          this.form.controls.fecha.setValue(fecha);
        }
        if (horaParam) {
          this.form.controls.hora.setValue(horaParam);
        }
      });
    });

    effect(() => {
      const { medicoId, fecha } = this.valores();
      if (medicoId && fecha) {
        untracked(() => this.medicos.loadDisponibilidad(medicoId, toIsoDate(fecha)));
      }
    });

    effect(() => {
      const disponibles = this.horas();
      const actual = untracked(() => this.form.controls.hora.value);
      if (
        actual &&
        !this.medicos.disponibilidad.loading() &&
        this.medicos.disponibilidad.hasValue() &&
        !disponibles.includes(actual)
      ) {
        untracked(() => this.form.controls.hora.setValue(null));
      }
    });
  }

  protected elegirHora(hora: string): void {
    this.form.controls.hora.setValue(hora);
    this.form.controls.hora.markAsTouched();
  }

  protected cambioMedicoOFecha(): void {
    this.form.controls.hora.setValue(null);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    const fechaHora = this.fechaHora();
    if (this.form.invalid || !fechaHora) {
      return;
    }
    const valor = this.form.getRawValue();
    this.submitting.set(true);
    this.citas
      .crear({ medicoId: valor.medicoId!, fechaHora, motivo: valor.motivo!.trim() })
      .subscribe({
        next: (cita) => {
          this.notifications.success('Tu cita fue reservada. El médico la confirmará pronto.');
          void this.router.navigate(['/citas', cita.id]);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          applyServerErrors(this.form, error);
          if (error.status === 409) {
            this.form.controls.hora.setValue(null);
            const id = valor.medicoId;
            if (id && valor.fecha) {
              this.medicos.loadDisponibilidad(id, toIsoDate(valor.fecha));
            }
          }
        },
      });
  }
}
