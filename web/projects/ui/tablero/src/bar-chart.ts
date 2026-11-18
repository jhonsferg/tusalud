import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface PuntoBarra {
  readonly etiqueta: string;
  readonly valor: number;
}

@Component({
  selector: 'app-bar-chart',
  template: `
    <figure>
      <svg
        [attr.viewBox]="'0 0 ' + ancho + ' ' + alto"
        preserveAspectRatio="none"
        role="img"
        [attr.aria-label]="titulo()"
      >
        @for (barra of barras(); track barra.etiqueta) {
          <rect
            [attr.x]="barra.x"
            [attr.y]="barra.y"
            [attr.width]="barra.ancho"
            [attr.height]="barra.alto"
            rx="2"
          >
            <title>{{ barra.etiqueta }}: {{ barra.texto }}</title>
          </rect>
        }
        <line [attr.x1]="0" [attr.x2]="ancho" [attr.y1]="alto - 0.5" [attr.y2]="alto - 0.5" />
      </svg>
      <figcaption>
        <span>{{ primera() }}</span>
        <span class="max">Máximo {{ maximoTexto() }}</span>
        <span>{{ ultima() }}</span>
      </figcaption>
      <table class="solo-lectores">
        <caption>
          {{
            titulo()
          }}
        </caption>
        <tbody>
          @for (punto of datos(); track punto.etiqueta) {
            <tr>
              <th scope="row">{{ punto.etiqueta }}</th>
              <td>{{ formato()(punto.valor) }}</td>
            </tr>
          }
        </tbody>
      </table>
    </figure>
  `,
  styles: `
    figure {
      margin: 0;
    }

    svg {
      display: block;
      width: 100%;
      height: 11rem;
      overflow: visible;
    }

    rect {
      fill: var(--p-primary-color);
      opacity: 0.85;
    }

    rect:hover {
      opacity: 1;
    }

    line {
      stroke: var(--p-content-border-color);
      stroke-width: 1;
      vector-effect: non-scaling-stroke;
    }

    figcaption {
      display: flex;
      justify-content: space-between;
      gap: var(--sp-2);
      margin-top: var(--sp-2);
      color: var(--p-text-muted-color);
      font-size: 0.8125rem;
    }

    .max {
      font-variant-numeric: tabular-nums;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BarChart {
  readonly datos = input.required<readonly PuntoBarra[]>();
  readonly titulo = input('Gráfico de barras');
  readonly formato = input<(valor: number) => string>((valor) => String(valor));

  protected readonly ancho = 300;
  protected readonly alto = 100;

  protected readonly maximo = computed(() => Math.max(1, ...this.datos().map((d) => d.valor)));
  protected readonly maximoTexto = computed(() => this.formato()(this.maximo()));
  protected readonly primera = computed(() => this.datos()[0]?.etiqueta ?? '');
  protected readonly ultima = computed(() => this.datos().at(-1)?.etiqueta ?? '');

  protected readonly barras = computed(() => {
    const datos = this.datos();
    if (datos.length === 0) {
      return [];
    }
    const paso = this.ancho / datos.length;
    const ancho = Math.max(1, paso * 0.7);
    return datos.map((punto, i) => {
      const alto = Math.max(
        punto.valor > 0 ? 1.5 : 0,
        (punto.valor / this.maximo()) * (this.alto - 2),
      );
      return {
        etiqueta: punto.etiqueta,
        texto: this.formato()(punto.valor),
        x: i * paso + (paso - ancho) / 2,
        y: this.alto - alto,
        ancho,
        alto,
      };
    });
  });
}
