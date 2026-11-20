import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';

import { AuthService, NAVEGADOR_EXTERNO, PORTAL_LOGIN } from '@tusalud/core';
import { EstadoVacio } from '@tusalud/ui/estado-vacio';

@Component({
  selector: 'app-forbidden',
  imports: [EstadoVacio, Button],
  template: `
    <div class="flex align-items-center justify-content-center" style="min-height: 100vh">
      <app-estado-vacio
        imagen="acceso-denegado"
        title="Acceso denegado"
        message="Tu cuenta no tiene permisos para ver esta sección."
      >
        <p-button
          [label]="auth.isAuthenticated() ? 'Volver al inicio' : 'Iniciar sesión'"
          icon="pi pi-home"
          (onClick)="ir()"
        />
      </app-estado-vacio>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Forbidden {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly navegador = inject(NAVEGADOR_EXTERNO, { optional: true });

  protected ir(): void {
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl(this.auth.homeUrl());
    } else if (this.navegador) {
      this.navegador(PORTAL_LOGIN);
    } else {
      void this.router.navigateByUrl(PORTAL_LOGIN);
    }
  }
}
