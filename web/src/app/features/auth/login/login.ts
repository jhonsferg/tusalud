import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Password } from 'primeng/password';

import { ApiError } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { AppValidators } from '@tusalud/app/core/validators/app.validators';
import { applyServerErrors } from '@tusalud/app/core/validators/server-errors';
import { environment } from '@tusalud/environments/environment';
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';

interface CuentaDemo {
  readonly rol: string;
  readonly email: string;
  readonly password: string;
}

const CUENTAS_DEMO: readonly CuentaDemo[] = [
  { rol: 'Paciente', email: 'juan.perez@correo.com', password: 'Paciente123!' },
  { rol: 'Médico', email: 'dr.garcia@tusalud.com', password: 'Medico123!' },
  { rol: 'Admin', email: 'admin@tusalud.com', password: 'Admin123!' },
];

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, Button, InputText, Password, Message, FormField],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Login {
  readonly returnUrl = input<string>();

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly cuentasDemo = environment.production ? [] : CUENTAS_DEMO;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, AppValidators.notBlank]],
    password: ['', [Validators.required]],
  });

  protected usarCuenta(cuenta: CuentaDemo): void {
    this.form.setValue({ email: cuenta.email, password: cuenta.password });
  }

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const destino = this.returnUrl();
        void this.router.navigateByUrl(
          destino && destino.startsWith('/') && !destino.startsWith('//')
            ? destino
            : this.auth.homeUrl(),
        );
      },
      error: (error: ApiError) => {
        this.submitting.set(false);
        const sinMapear = applyServerErrors(this.form, error);
        this.errorMessage.set(
          error.status === 401
            ? 'Email o contraseña incorrectos.'
            : (sinMapear[0] ?? error.message),
        );
      },
    });
  }
}
