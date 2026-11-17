import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';

import { ApiError } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { EspecialidadesStore } from '@tusalud/app/core/state/especialidades.store';
import { optionalIdAttribute } from '@tusalud/app/core/utils/route-input';
import { AppValidators } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-especialidad-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputNumber,
    InputText,
    Textarea,
    EmptyState,
    FormField,
    PageHeader,
  ],
  templateUrl: './especialidad-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EspecialidadForm {
  readonly id = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(EspecialidadesStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly editando = computed(() => this.id() !== undefined);
  protected readonly submitting = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    nombre: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    descripcion: ['', [Validators.maxLength(255)]],
    precioConsulta: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.loadOne(id);
      }
    });
    effect(() => {
      const actual = this.store.selected.value();
      if (actual && actual.id === this.id()) {
        this.form.reset({
          nombre: actual.nombre,
          descripcion: actual.descripcion ?? '',
          precioConsulta: actual.precioConsulta,
        });
      }
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nombre, descripcion, precioConsulta } = this.form.getRawValue();
    const request = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim() || null,
      precioConsulta: precioConsulta!,
    };
    const id = this.id();
    this.submitting.set(true);
    const operacion =
      id === undefined ? this.store.crear(request) : this.store.actualizar(id, request);
    operacion.subscribe({
      next: (guardada) => {
        this.notifications.success(
          id === undefined
            ? `La especialidad ${guardada.nombre} fue creada.`
            : `La especialidad ${guardada.nombre} fue actualizada.`,
        );
        void this.router.navigate(['/especialidades']);
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        applyServerErrors(this.form, error);
      },
    });
  }
}
