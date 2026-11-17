import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import { AuthService } from '@tusalud/app/core/services/auth.service';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';

@Component({
  selector: 'app-forbidden',
  imports: [EmptyState, Button, RouterLink],
  template: `
    <div class="flex align-items-center justify-content-center" style="min-height: 100vh">
      <app-empty-state
        icon="pi-lock"
        tone="error"
        title="Acceso denegado"
        message="Tu cuenta no tiene permisos para ver esta sección."
      >
        <p-button
          label="Volver al inicio"
          icon="pi pi-home"
          [routerLink]="auth.isAuthenticated() ? auth.homeUrl() : '/auth/login'"
        />
      </app-empty-state>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Forbidden {
  protected readonly auth = inject(AuthService);
}
