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
import { FormField } from '@tusalud/app/shared/components/form-field/form-field';

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
  protected readonly mfaToken = signal<string | null>(null);
  protected readonly usarRecuperacion = signal(false);

  protected readonly codigo = inject(FormBuilder).nonNullable.control('', [Validators.required]);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, AppValidators.notBlank]],
    password: ['', [Validators.required]],
  });

  protected submit(): void {
    this.errorMessage.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (resultado) => {
        if ('mfa' in resultado) {
          this.submitting.set(false);
          this.mfaToken.set(resultado.mfa.mfaToken);
          return;
        }
        this.entrar();
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

  protected verificar(): void {
    const token = this.mfaToken();
    this.errorMessage.set(null);
    if (!token || this.codigo.invalid) {
      this.codigo.markAsTouched();
      return;
    }
    this.submitting.set(true);
    this.auth.verificarMfa(token, this.codigo.value.trim()).subscribe({
      next: () => this.entrar(),
      error: (error: ApiError) => {
        this.submitting.set(false);
        this.errorMessage.set(
          error.status === 401 || error.status === 400
            ? 'El código no es válido o ya venció.'
            : error.message,
        );
      },
    });
  }

  protected volver(): void {
    this.mfaToken.set(null);
    this.codigo.reset('');
    this.errorMessage.set(null);
    this.usarRecuperacion.set(false);
  }

  protected alternarRecuperacion(): void {
    this.usarRecuperacion.update((valor) => !valor);
    this.errorMessage.set(null);
    this.codigo.reset('');
  }

  private entrar(): void {
    const destino = this.returnUrl();
    void this.router.navigateByUrl(
      destino && destino.startsWith('/') && !destino.startsWith('//')
        ? destino
        : this.auth.homeUrl(),
    );
  }
}
