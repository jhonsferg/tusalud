import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';

import {
  ApiError,
  applyServerErrors,
  AppValidators,
  AuthService,
  PASSWORD_MAX,
  toIsoDate,
} from '@tusalud/core';
import { FormField } from '@tusalud/ui/form-field';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    DatePicker,
    InputText,
    Message,
    Password,
    FormField,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly ayer = (() => {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() - 1);
    return fecha;
  })();

  protected readonly form = inject(FormBuilder).group({
    nombre: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    apellido: ['', [Validators.required, AppValidators.notBlank, Validators.maxLength(80)]],
    dni: ['', [Validators.required, AppValidators.dni]],
    telefono: ['', [AppValidators.telefono]],
    fechaNacimiento: [null as Date | null, [Validators.required]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: [
      '',
      [Validators.required, AppValidators.password, Validators.maxLength(PASSWORD_MAX)],
    ],
  });

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    this.submitting.set(true);
    this.auth
      .register({
        nombre: valor.nombre!.trim(),
        apellido: valor.apellido!.trim(),
        dni: valor.dni!,
        telefono: valor.telefono || null,
        fechaNacimiento: toIsoDate(valor.fechaNacimiento!),
        email: valor.email!,
        password: valor.password!,
      })
      .subscribe({
        next: () => void this.router.navigateByUrl(this.auth.homeUrl()),
        error: (error: ApiError) => {
          this.submitting.set(false);
          const sinMapear = applyServerErrors(this.form, error);
          this.errorMessage.set(
            sinMapear[0] ?? (Object.keys(error.validationErrors).length > 0 ? null : error.message),
          );
        },
      });
  }
}
