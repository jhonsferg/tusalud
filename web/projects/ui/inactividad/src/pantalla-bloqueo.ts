import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthService, InactividadService } from '@tusalud/core';

@Component({
  selector: 'app-pantalla-bloqueo',
  imports: [FormsModule],
  template: `
    @if (inactividad.estado() === 'aviso') {
      <div class="aviso" role="alertdialog" aria-labelledby="aviso-titulo" aria-live="assertive">
        <i class="pi pi-clock" aria-hidden="true"></i>
        <div class="texto">
          <strong id="aviso-titulo">Tu sesión se bloqueará por inactividad</strong>
          <span>
            Se bloqueará en {{ inactividad.segundosRestantes() }}
            {{ inactividad.segundosRestantes() === 1 ? 'segundo' : 'segundos' }}.
          </span>
        </div>
        <button type="button" class="primario" (click)="inactividad.seguirTrabajando()">
          Seguir trabajando
        </button>
      </div>
    }

    @if (inactividad.estado() === 'bloqueado') {
      <div class="bloqueo" role="dialog" aria-modal="true" aria-labelledby="bloqueo-titulo">
        <form class="caja" (submit)="$event.preventDefault(); desbloquear()" autocomplete="off">
          <span class="candado" aria-hidden="true"><i class="pi pi-lock"></i></span>
          <h2 id="bloqueo-titulo">Sesión bloqueada</h2>
          <p class="quien">
            {{ nombre() }}<br /><small
              >Por seguridad, la pantalla se bloqueó tras un periodo sin actividad.</small
            >
          </p>

          @if (error()) {
            <p class="error" role="alert">{{ error() }}</p>
          }

          @if (mfaToken()) {
            <label for="bloqueo-codigo">Código de verificación</label>
            <input
              id="bloqueo-codigo"
              name="codigo"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="20"
              [ngModel]="codigo()"
              (ngModelChange)="codigo.set($event)"
              [ngModelOptions]="{ standalone: true }"
            />
          } @else {
            <label for="bloqueo-password">Contraseña</label>
            <input
              id="bloqueo-password"
              name="password"
              type="password"
              autocomplete="current-password"
              [ngModel]="password()"
              (ngModelChange)="password.set($event)"
              [ngModelOptions]="{ standalone: true }"
            />
          }

          <button type="submit" class="primario" [disabled]="enviando()">
            {{ enviando() ? 'Verificando…' : 'Desbloquear' }}
          </button>
          <button type="button" class="secundario" (click)="inactividad.cerrarSesion()">
            Cerrar sesión
          </button>
        </form>
      </div>
    }
  `,
  styles: `
    .aviso {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 2000;
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      max-width: 28rem;
      padding: var(--sp-3) var(--sp-4);
      border: 1px solid var(--estado-aviso);
      border-radius: var(--radio-md);
      background: var(--estado-aviso-fondo);
      box-shadow: var(--sombra-3);
      color: var(--p-text-color);

      > i {
        color: var(--estado-aviso);
        font-size: 1.4rem;
      }
    }

    .texto {
      display: flex;
      flex: 1;
      flex-direction: column;
    }

    .bloqueo {
      position: fixed;
      inset: 0;
      z-index: 3000;
      display: grid;
      place-items: center;
      padding: var(--sp-4);
      background: var(--app-bg);
    }

    .caja {
      display: flex;
      flex-direction: column;
      gap: var(--sp-2);
      width: min(24rem, 100%);
      padding: var(--sp-6);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-lg);
      background: var(--p-content-background);
      box-shadow: var(--sombra-3);
      text-align: center;
    }

    .candado {
      display: grid;
      place-items: center;
      width: 3.5rem;
      height: 3.5rem;
      margin: 0 auto var(--sp-1);
      border-radius: 50%;
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
      font-size: 1.5rem;
    }

    h2 {
      margin: 0;
      font-size: 1.25rem;
    }

    .quien {
      margin: 0 0 var(--sp-2);

      small {
        color: var(--p-text-muted-color);
      }
    }

    label {
      text-align: left;
      font-size: 0.875rem;
      font-weight: 600;
    }

    input {
      min-height: 2.75rem;
      padding: 0 var(--sp-3);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-sm);
      background: var(--p-content-background);
      color: var(--p-text-color);
      font: inherit;
    }

    .error {
      margin: 0;
      padding: var(--sp-2);
      border-radius: var(--radio-sm);
      background: var(--estado-peligro-fondo);
      color: var(--estado-peligro);
      font-size: 0.875rem;
    }

    button {
      min-height: 2.75rem;
      padding: 0 var(--sp-4);
      border: 1px solid var(--p-primary-color);
      border-radius: var(--radio-sm);
      font: inherit;
      font-weight: 600;
      cursor: pointer;
    }

    .primario {
      background: var(--p-primary-color);
      color: var(--p-primary-contrast-color);
    }

    .primario:disabled {
      opacity: 0.6;
      cursor: progress;
    }

    .secundario {
      border-color: var(--p-content-border-color);
      background: transparent;
      color: var(--p-text-color);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PantallaBloqueo {
  protected readonly inactividad = inject(InactividadService);
  private readonly auth = inject(AuthService);

  protected readonly password = signal('');
  protected readonly codigo = signal('');
  protected readonly mfaToken = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly nombre = computed(() => this.auth.user()?.nombreCompleto ?? '');

  protected desbloquear(): void {
    const usuario = this.auth.user();
    if (!usuario || this.enviando()) {
      return;
    }
    this.error.set(null);
    this.enviando.set(true);
    const mfa = this.mfaToken();
    const alFallar = (): void => {
      this.enviando.set(false);
      this.password.set('');
      this.error.set(
        mfa ? 'El código no es válido. Inténtalo de nuevo.' : 'La contraseña no es correcta.',
      );
    };
    if (mfa) {
      this.auth.verificarMfa(mfa, this.codigo().trim()).subscribe({
        next: () => this.terminar(),
        error: alFallar,
      });
      return;
    }
    this.auth.login({ email: usuario.email, password: this.password() }).subscribe({
      next: (resultado) => {
        if ('mfa' in resultado) {
          this.enviando.set(false);
          this.mfaToken.set(resultado.mfa.mfaToken);
          return;
        }
        this.terminar();
      },
      error: alFallar,
    });
  }

  private terminar(): void {
    this.enviando.set(false);
    this.limpiar();
    this.inactividad.desbloquear();
  }

  private limpiar(): void {
    this.password.set('');
    this.codigo.set('');
    this.mfaToken.set(null);
    this.error.set(null);
  }
}
