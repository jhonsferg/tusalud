import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Select } from 'primeng/select';

import { ApiError } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { EspecialidadesStore } from '@tusalud/app/core/state/especialidades.store';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { AppValidators, PASSWORD_MAX } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-medico-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputNumber,
    InputText,
    Password,
    Select,
    FormField,
    PageHeader,
  ],
  templateUrl: './medico-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoForm implements OnInit {
  protected readonly especialidades = inject(EspecialidadesStore);
  private readonly medicos = inject(MedicosStore);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);

  protected readonly form = inject(FormBuilder).group({
    nombre: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: [
      '',
      [Validators.required, AppValidators.password, Validators.maxLength(PASSWORD_MAX)],
    ],
    telefono: ['', [AppValidators.telefono]],
    colegiatura: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(20)]],
    consultorio: ['', [Validators.maxLength(60)]],
    precioConsulta: [null as number | null, [Validators.required, Validators.min(0.01)]],
    especialidadId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.especialidades.load();
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.submitting.set(true);
    this.medicos
      .crear({
        nombre: valor.nombre!.trim(),
        apellido: valor.apellido!.trim(),
        email: valor.email!,
        password: valor.password!,
        telefono: valor.telefono || null,
        colegiatura: valor.colegiatura!.trim(),
        consultorio: valor.consultorio?.trim() || null,
        precioConsulta: valor.precioConsulta!,
        especialidadId: valor.especialidadId!,
      })
      .subscribe({
        next: (medico) => {
          this.notifications.success(
            `El médico ${medico.nombreCompleto} fue registrado. Ahora configura sus horarios.`,
          );
          void this.router.navigate(['/medicos', medico.id, 'horarios']);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          applyServerErrors(this.form, error);
        },
      });
  }
}
