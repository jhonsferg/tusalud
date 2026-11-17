import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import { AuthService } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';

@Component({
  selector: 'app-not-found',
  imports: [EmptyState, Button, RouterLink],
  template: `
    <div class="flex align-items-center justify-content-center" style="min-height: 100vh">
      <app-empty-state
        icon="pi-search"
        title="Página no encontrada"
        message="La dirección que buscas no existe o fue movida."
      >
        <p-button
          [label]="auth.isAuthenticated() ? 'Ir al inicio' : 'Iniciar sesión'"
          icon="pi pi-home"
          [routerLink]="auth.isAuthenticated() ? auth.homeUrl() : '/auth/login'"
        />
      </app-empty-state>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFound {
  protected readonly auth = inject(AuthService);
}
