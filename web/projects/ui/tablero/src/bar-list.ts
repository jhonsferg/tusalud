import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { TonoTablero } from './stat-card';

export interface ItemBarra {
  readonly etiqueta: string;
  readonly valor: number;
  readonly texto?: string | null;
  readonly tono?: TonoTablero;
}

@Component({
  selector: 'app-bar-list',
  template: `
    <ul class="barras" [attr.aria-label]="etiqueta()">
      @for (item of filas(); track item.etiqueta) {
        <li [attr.data-tono]="item.tono ?? 'neutro'">
          <div class="texto">
            <span class="nombre">{{ item.etiqueta }}</span>
            <strong class="valor">{{ item.texto ?? item.valor }}</strong>
          </div>
          <div class="pista" role="presentation">
            <span class="relleno" [style.width.%]="item.ancho"></span>
          </div>
        </li>
      }
    </ul>
  `,
  styles: `
    .barras {
      display: grid;
      gap: var(--sp-3);
      margin: 0;
      padding: 0;
      list-style: none;
    }

    .texto {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: var(--sp-3);
      margin-bottom: 0.25rem;
      font-size: var(--texto-dato);
    }

    .nombre {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .valor {
      font-variant-numeric: tabular-nums;
    }

    .pista {
      height: 0.55rem;
      border-radius: 999px;
      background: var(--p-content-hover-background);
      overflow: hidden;
    }

    .relleno {
      display: block;
      height: 100%;
      min-width: 2px;
      border-radius: 999px;
      background: var(--color-tono);
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
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarList {
  readonly items = input.required<readonly ItemBarra[]>();
  readonly etiqueta = input('Distribución');
  readonly maximo = input<number | null>(null);

  protected readonly filas = computed(() => {
    const lista = this.items();
    const tope = this.maximo() ?? Math.max(1, ...lista.map((item) => item.valor));
    return lista.map((item) => ({
      ...item,
      ancho: Math.max(0, Math.min(100, (item.valor / tope) * 100)),
    }));
  });
}
