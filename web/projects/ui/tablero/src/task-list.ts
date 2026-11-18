import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TonoTablero } from './stat-card';

export interface ItemTarea {
  readonly id: string;
  readonly titulo: string;
  readonly detalle?: string | null;
  readonly cantidad?: number | null;
  readonly tono?: TonoTablero;
  readonly icono?: string | null;
  readonly enlace?: string | readonly string[] | null;
  readonly sensible?: boolean;
}

@Component({
  selector: 'app-task-list',
  imports: [NgTemplateOutlet, RouterLink],
  template: `
    <ul class="tareas">
      @for (item of items(); track item.id) {
        <li [attr.data-tono]="item.tono ?? 'neutro'">
          @if (item.enlace) {
            <a [routerLink]="item.enlace">
              <ng-container *ngTemplateOutlet="fila; context: { $implicit: item }" />
            </a>
          } @else {
            <div class="sin-enlace">
              <ng-container *ngTemplateOutlet="fila; context: { $implicit: item }" />
            </div>
          }
        </li>
      }
    </ul>

    <ng-template #fila let-item>
      <span class="marca" aria-hidden="true"
        ><i [class]="'pi ' + (item.icono ?? 'pi-circle')"></i
      ></span>
      <span class="texto">
        <strong [class.sensible]="item.sensible">{{ item.titulo }}</strong>
        @if (item.detalle) {
          <small>{{ item.detalle }}</small>
        }
      </span>
      @if (item.cantidad !== null && item.cantidad !== undefined) {
        <span class="cantidad">{{ item.cantidad }}</span>
      }
    </ng-template>
  `,
  styles: `
    .tareas {
      display: flex;
      flex-direction: column;
      margin: 0;
      padding: 0;
      list-style: none;
    }

    li + li {
      border-top: 1px solid var(--p-content-border-color);
    }

    a,
    .sin-enlace {
      display: flex;
      align-items: center;
      gap: var(--sp-3);
      min-height: 2.75rem;
      padding: var(--sp-2) var(--sp-1);
      color: var(--p-text-color);
      text-decoration: none;
    }

    a:hover {
      background: var(--p-content-hover-background);
      text-decoration: none;
    }

    .marca {
      display: grid;
      flex: none;
      place-items: center;
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background: var(--fondo);
      color: var(--texto);
      font-size: 0.9rem;
    }

    .texto {
      display: flex;
      flex: 1;
      flex-direction: column;
      min-width: 0;

      small {
        color: var(--p-text-muted-color);
      }
    }

    .cantidad {
      min-width: 1.75rem;
      padding: 0.1rem 0.5rem;
      border-radius: 999px;
      background: var(--fondo);
      color: var(--texto);
      font-weight: 700;
      font-variant-numeric: tabular-nums;
      text-align: center;
    }

    [data-tono='neutro'] {
      --fondo: var(--estado-neutro-fondo);
      --texto: var(--estado-neutro);
    }

    [data-tono='exito'] {
      --fondo: var(--estado-exito-fondo);
      --texto: var(--estado-exito);
    }

    [data-tono='aviso'] {
      --fondo: var(--estado-aviso-fondo);
      --texto: var(--estado-aviso);
    }

    [data-tono='peligro'] {
      --fondo: var(--estado-peligro-fondo);
      --texto: var(--estado-peligro);
    }

    [data-tono='info'] {
      --fondo: var(--estado-info-fondo);
      --texto: var(--estado-info);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TaskList {
  readonly items = input.required<readonly ItemTarea[]>();
}
