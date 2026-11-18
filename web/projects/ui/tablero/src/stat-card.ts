import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export type TonoTablero = 'neutro' | 'exito' | 'aviso' | 'peligro' | 'info';

@Component({
  selector: 'app-stat-card',
  imports: [NgTemplateOutlet, RouterLink],
  template: `
    @if (enlace(); as destino) {
      <a class="stat" [routerLink]="destino" [attr.data-tono]="tono()">
        <ng-container *ngTemplateOutlet="contenido" />
      </a>
    } @else {
      <div class="stat" [attr.data-tono]="tono()">
        <ng-container *ngTemplateOutlet="contenido" />
      </div>
    }

    <ng-template #contenido>
      <div class="cabecera">
        <span class="etiqueta">{{ etiqueta() }}</span>
        @if (icono()) {
          <span class="icono" aria-hidden="true"><i [class]="'pi ' + icono()"></i></span>
        }
      </div>
      <strong class="valor">{{ valor() }}</strong>
      <div class="pie">
        @if (tendencia() !== null) {
          <span
            class="tendencia"
            [class.sube]="tendencia()! > 0"
            [class.baja]="tendencia()! < 0"
            [attr.aria-label]="textoTendencia()"
          >
            <i
              [class]="
                'pi ' +
                (tendencia()! > 0
                  ? 'pi-arrow-up-right'
                  : tendencia()! < 0
                    ? 'pi-arrow-down-right'
                    : 'pi-minus')
              "
              aria-hidden="true"
            ></i>
            {{ absoluta() }}%
          </span>
        }
        @if (detalle()) {
          <span class="detalle">{{ detalle() }}</span>
        }
      </div>
      @if (puntos(); as linea) {
        <svg class="serie" viewBox="0 0 100 28" preserveAspectRatio="none" aria-hidden="true">
          <polyline [attr.points]="linea" />
        </svg>
      }
    </ng-template>
  `,
  styles: `
    :host {
      display: block;
      min-width: 0;
    }

    .stat {
      display: flex;
      flex-direction: column;
      gap: var(--sp-1);
      height: 100%;
      padding: var(--sp-4);
      border: 1px solid var(--p-content-border-color);
      border-left: 4px solid var(--color-tono);
      border-radius: var(--radio-md);
      background: var(--p-content-background);
      box-shadow: var(--sombra-1);
      color: var(--p-text-color);
      text-decoration: none;
    }

    a.stat:hover {
      border-color: var(--color-tono);
      box-shadow: var(--sombra-2);
      text-decoration: none;
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

    .cabecera {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--sp-2);
    }

    .etiqueta {
      color: var(--p-text-muted-color);
      font-size: 0.8125rem;
      font-weight: 600;
    }

    .icono {
      color: var(--color-tono);
    }

    .valor {
      font-size: 1.75rem;
      font-weight: 700;
      line-height: 1.15;
      font-variant-numeric: tabular-nums;
    }

    .pie {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--sp-1) var(--sp-2);
      min-height: 1.25rem;
      font-size: 0.8125rem;
    }

    .detalle {
      color: var(--p-text-muted-color);
    }

    .tendencia {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      font-weight: 700;
      color: var(--p-text-muted-color);

      &.sube {
        color: var(--estado-exito);
      }

      &.baja {
        color: var(--estado-peligro);
      }
    }

    .serie {
      width: 100%;
      height: 1.75rem;
      margin-top: var(--sp-1);

      polyline {
        fill: none;
        stroke: var(--color-tono);
        stroke-width: 2;
        stroke-linejoin: round;
        stroke-linecap: round;
        vector-effect: non-scaling-stroke;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatCard {
  readonly etiqueta = input.required<string>();
  readonly valor = input.required<string | number>();
  readonly icono = input<string | null>(null);
  readonly tono = input<TonoTablero>('neutro');
  readonly tendencia = input<number | null>(null);
  readonly detalle = input<string | null>(null);
  readonly serie = input<readonly number[] | null>(null);
  readonly enlace = input<string | readonly string[] | null>(null);

  protected readonly absoluta = computed(() => Math.abs(Math.round(this.tendencia() ?? 0)));

  protected readonly textoTendencia = computed(() => {
    const valor = this.tendencia() ?? 0;
    const cuerpo = `${Math.abs(Math.round(valor))} por ciento`;
    return valor > 0 ? `Aumento de ${cuerpo}` : valor < 0 ? `Descenso de ${cuerpo}` : 'Sin cambios';
  });

  protected readonly puntos = computed(() => {
    const datos = this.serie();
    if (!datos || datos.length < 2) {
      return null;
    }
    const minimo = Math.min(...datos);
    const rango = Math.max(...datos) - minimo || 1;
    return datos
      .map((d, i) => `${(i / (datos.length - 1)) * 100},${26 - ((d - minimo) / rango) * 24}`)
      .join(' ');
  });
}
