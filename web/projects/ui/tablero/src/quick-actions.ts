import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface AccionRapida {
  readonly label: string;
  readonly icono: string;
  readonly enlace: string | readonly string[];
  readonly detalle?: string | null;
  readonly queryParams?: Record<string, string> | null;
}

@Component({
  selector: 'app-quick-actions',
  imports: [RouterLink],
  template: `
    <nav class="acciones" [attr.aria-label]="etiqueta()">
      @for (accion of acciones(); track accion.label) {
        <a [routerLink]="accion.enlace" [queryParams]="accion.queryParams ?? null">
          <span class="icono" aria-hidden="true"><i [class]="'pi ' + accion.icono"></i></span>
          <span class="texto">
            <strong>{{ accion.label }}</strong>
            @if (accion.detalle) {
              <small>{{ accion.detalle }}</small>
            }
          </span>
        </a>
      }
    </nav>
  `,
  styles: `
    .acciones {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(min(100%, 17rem), 1fr));
      gap: var(--sp-3);
    }

    a {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      min-height: 3.5rem;
      padding: var(--sp-3);
      border: 1px solid var(--p-content-border-color);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
      color: var(--p-text-color);
      text-decoration: none;
      transition:
        border-color 0.12s ease,
        background-color 0.12s ease;

      &:hover {
        border-color: var(--p-primary-color);
        background: var(--app-primary-soft);
        text-decoration: none;
      }
    }

    .icono {
      display: grid;
      flex: none;
      place-items: center;
      width: 2.5rem;
      height: 2.5rem;
      border-radius: var(--radio-sm);
      background: var(--app-primary-soft);
      color: var(--app-primary-text);
      font-size: 1.1rem;
    }

    .texto {
      display: flex;
      flex-direction: column;
      min-width: 0;

      small {
        color: var(--p-text-muted-color);
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuickActions {
  readonly acciones = input.required<readonly AccionRapida[]>();
  readonly etiqueta = input('Accesos rápidos');
}
