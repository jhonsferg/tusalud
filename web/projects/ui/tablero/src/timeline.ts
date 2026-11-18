import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { StatusBadge } from './status-badge';
import { TonoTablero } from './stat-card';

export interface ItemLineaTiempo {
  readonly id: string;
  readonly hora: string;
  readonly titulo: string;
  readonly detalle?: string | null;
  readonly estado?: string | null;
  readonly tono?: TonoTablero;
  readonly enlace?: string | readonly string[] | null;
  readonly resaltado?: boolean;
  readonly sensible?: boolean;
}

@Component({
  selector: 'app-timeline',
  imports: [RouterLink, StatusBadge],
  template: `
    <ol class="linea">
      @for (item of items(); track item.id) {
        <li [class.resaltado]="item.resaltado" [attr.data-tono]="item.tono ?? 'neutro'">
          <time class="hora">{{ item.hora }}</time>
          <span class="punto" aria-hidden="true"></span>
          <div class="texto">
            @if (item.enlace) {
              <a [routerLink]="item.enlace" [class.sensible]="item.sensible">{{ item.titulo }}</a>
            } @else {
              <strong [class.sensible]="item.sensible">{{ item.titulo }}</strong>
            }
            @if (item.detalle) {
              <small>{{ item.detalle }}</small>
            }
          </div>
          @if (item.estado) {
            <app-status-badge [etiqueta]="item.estado" [tono]="item.tono ?? 'neutro'" />
          }
        </li>
      }
    </ol>
  `,
  styles: `
    .linea {
      display: flex;
      flex-direction: column;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    li {
      position: relative;
      display: grid;
      grid-template-columns: 3.4rem 0.9rem minmax(0, 1fr) auto;
      align-items: start;
      gap: var(--sp-3);
      padding: var(--sp-3) 0;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        bottom: 0;
        left: calc(3.4rem + var(--sp-3) + 0.4rem);
        width: 2px;
        background: var(--p-content-border-color);
      }

      &:first-child::before {
        top: 1.3rem;
      }

      &:last-child::before {
        bottom: calc(100% - 1.3rem);
      }
    }

    .hora {
      color: var(--p-text-muted-color);
      font-size: 0.875rem;
      font-weight: 600;
      text-align: right;
    }

    .punto {
      position: relative;
      z-index: 1;
      width: 0.9rem;
      height: 0.9rem;
      margin-top: 0.2rem;
      border: 3px solid var(--p-content-background);
      border-radius: 50%;
      background: var(--color-tono);
      box-shadow: 0 0 0 1px var(--color-tono);
    }

    .texto {
      display: flex;
      flex-direction: column;
      min-width: 0;

      small {
        color: var(--p-text-muted-color);
      }
    }

    li.resaltado .texto strong,
    li.resaltado .texto a {
      color: var(--app-primary-text);
    }

    [data-tono='neutro'] {
      --color-tono: var(--p-primary-color);
    }

    [data-tono='exito'] {
      --color-tono: var(--estado-exito);
    }

    [data-tono='aviso'] {
      --color-tono: var(--estado-aviso);
    }

    [data-tono='peligro'] {
      --color-tono: var(--estado-peligro);
    }

    [data-tono='info'] {
      --color-tono: var(--estado-info);
    }

    @media (max-width: 520px) {
      li {
        grid-template-columns: 3rem 0.9rem minmax(0, 1fr);

        app-status-badge {
          grid-column: 3;
        }
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Timeline {
  readonly items = input.required<readonly ItemLineaTiempo[]>();
}
