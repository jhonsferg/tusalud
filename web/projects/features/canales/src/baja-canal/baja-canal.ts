import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

import { ApiError, CanalesApiService, ETIQUETA_CANAL } from '@tusalud/core';

type Estado = 'listo' | 'procesando' | 'hecho' | 'error';

@Component({
  selector: 'app-baja-canal',
  imports: [Button],
  template: `
    <main class="baja">
      <h1>Dejar de recibir avisos</h1>
      @switch (estado()) {
        @case ('listo') {
          <p>
            Vas a dejar de recibir avisos de TuSalud por este canal. Las alertas de seguridad de tu
            cuenta seguirán llegando dentro de la aplicación.
          </p>
          <p-button
            label="Confirmar baja"
            icon="pi pi-ban"
            severity="danger"
            (onClick)="confirmar()"
          />
        }
        @case ('procesando') {
          <p role="status">Procesando tu solicitud…</p>
        }
        @case ('hecho') {
          <p role="status" class="ok">
            <i class="pi pi-check-circle" aria-hidden="true"></i>
            Listo. Ya no recibirás avisos por {{ etiqueta() }}. Puedes volver a activarlo desde tu
            perfil cuando quieras.
          </p>
          <p-button label="Ir a TuSalud" [outlined]="true" (onClick)="irAlInicio()" />
        }
        @default {
          <p role="alert" class="error">{{ mensaje() }}</p>
          <p-button label="Ir a TuSalud" [outlined]="true" (onClick)="irAlInicio()" />
        }
      }
    </main>
  `,
  styles: `
    .baja {
      max-width: 34rem;
      margin: 3rem auto;
      padding: 0 1rem;
    }

    h1 {
      margin: 0 0 1rem;
      font-size: 1.5rem;
    }

    .ok {
      color: var(--p-green-700, #15803d);
    }

    .error {
      color: var(--p-red-600, #dc2626);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BajaCanal implements OnInit {
  private readonly api = inject(CanalesApiService);
  private readonly router = inject(Router);

  readonly t = input<string>();

  protected readonly estado = signal<Estado>('listo');
  protected readonly etiqueta = signal('ese canal');
  protected readonly mensaje = signal('');

  ngOnInit(): void {
    if (!this.t()) {
      this.estado.set('error');
      this.mensaje.set('El enlace no es válido. Usa el enlace del último mensaje que recibiste.');
    }
  }

  protected confirmar(): void {
    const token = this.t();
    if (!token) {
      return;
    }
    this.estado.set('procesando');
    this.api.darDeBaja(token).subscribe({
      next: (respuesta) => {
        this.etiqueta.set(ETIQUETA_CANAL[respuesta.canal] ?? 'ese canal');
        this.estado.set('hecho');
      },
      error: (error: ApiError) => {
        this.mensaje.set(error.message);
        this.estado.set('error');
      },
    });
  }

  protected irAlInicio(): void {
    void this.router.navigateByUrl('/');
  }
}
