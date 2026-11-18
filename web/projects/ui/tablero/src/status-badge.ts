import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TonoTablero } from './stat-card';

@Component({
  selector: 'app-status-badge',
  template: `
    <span class="insignia" [attr.data-tono]="tono()">
      @if (icono()) {
        <i [class]="'pi ' + icono()" aria-hidden="true"></i>
      }
      {{ etiqueta() }}
    </span>
  `,
  styles: `
    .insignia {
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
      padding: 0.15rem 0.6rem;
      border-radius: 999px;
      background: var(--fondo);
      color: var(--texto);
      font-size: 0.8125rem;
      font-weight: 600;
      white-space: nowrap;
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
export class StatusBadge {
  readonly etiqueta = input.required<string>();
  readonly tono = input<TonoTablero>('neutro');
  readonly icono = input<string | null>(null);
}
