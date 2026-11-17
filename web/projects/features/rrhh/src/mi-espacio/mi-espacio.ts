import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-mi-espacio',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, PageHeader],
  template: `
    <app-page-header
      title="Mi espacio"
      subtitle="Tus turnos, tus ausencias y tu ficha laboral"
      icon="pi-clock"
    />
    <nav class="pestanas" aria-label="Secciones de Mi espacio">
      <a routerLink="turnos" routerLinkActive="activa">Mis turnos</a>
      <a routerLink="ausencias" routerLinkActive="activa">Mis ausencias</a>
      <a routerLink="ficha" routerLinkActive="activa">Mi ficha</a>
    </nav>
    <router-outlet />
  `,
  styles: `
    .pestanas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1.25rem;
    }

    a {
      padding: 0.5rem 1rem;
      border-radius: 999px;
      background: var(--p-content-hover-background);
      color: var(--p-text-color);
      font-weight: 600;
      text-decoration: none;
    }

    a.activa {
      background: var(--p-primary-color);
      color: var(--p-primary-contrast-color);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiEspacio {}
