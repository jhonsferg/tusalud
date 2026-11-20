import { ChangeDetectionStrategy, Component, computed, input, model, signal } from '@angular/core';

import {
  EstadoPieza,
  Odontograma as OdontogramaDatos,
  PiezaOdontograma,
  SuperficieDental,
} from '@tusalud/core';
import {
  COLOR_ESTADO_PIEZA,
  ETIQUETA_ESTADO_PIEZA,
  FILAS_PERMANENTES,
  FILAS_TEMPORALES,
  SUPERFICIES,
  descripcionPieza,
  estadoDeSuperficie,
  poligonosDiente,
  vecina,
} from '../odontologia.utils';

interface DienteVista {
  readonly numero: number;
  readonly descripcion: string;
  readonly ausente: boolean;
  readonly completo: EstadoPieza | null;
  readonly poligonos: { superficie: SuperficieDental; puntos: string; color: string }[];
}

@Component({
  selector: 'app-odontograma',
  template: `
    <div class="odontograma" role="group" aria-label="Odontograma">
      <p class="ayuda text-muted" id="ayuda-odontograma">
        Use las flechas para moverse entre las piezas y Enter o Espacio para seleccionar una.
      </p>
      @for (grupo of grupos(); track grupo.titulo) {
        <section [attr.aria-label]="grupo.titulo">
          <h3>{{ grupo.titulo }}</h3>
          @for (fila of grupo.filas; track $index) {
            <div
              class="fila"
              [style.grid-template-columns]="'repeat(' + fila.length + ', minmax(0, 1fr))'"
              (keydown)="teclado($event, grupo.todas)"
            >
              @for (d of fila; track d.numero) {
                <button
                  type="button"
                  class="diente"
                  [class.seleccionado]="seleccionada() === d.numero"
                  [attr.data-pieza]="d.numero"
                  [attr.aria-label]="d.descripcion"
                  [attr.aria-pressed]="seleccionada() === d.numero"
                  [attr.aria-describedby]="'ayuda-odontograma'"
                  [attr.tabindex]="d.numero === enfocable() ? 0 : -1"
                  (click)="elegir(d.numero)"
                  (focus)="enfocada.set(d.numero)"
                >
                  <svg viewBox="0 0 40 40" aria-hidden="true" focusable="false">
                    @for (poligono of d.poligonos; track poligono.superficie) {
                      <polygon
                        [attr.points]="poligono.puntos"
                        [attr.fill]="poligono.color"
                        stroke="#475569"
                        stroke-width="1"
                      />
                    }
                    @if (d.completo === 'AUSENTE') {
                      <line x1="4" y1="4" x2="36" y2="36" stroke="#334155" stroke-width="3" />
                      <line x1="36" y1="4" x2="4" y2="36" stroke="#334155" stroke-width="3" />
                    } @else if (d.completo) {
                      <rect
                        x="2"
                        y="2"
                        width="36"
                        height="36"
                        fill="none"
                        [attr.stroke]="colorCompleto(d.completo)"
                        stroke-width="3"
                        rx="4"
                      />
                    }
                  </svg>
                  <span class="numero">{{ d.numero }}</span>
                </button>
              }
            </div>
          }
        </section>
      }
      <ul class="leyenda" aria-label="Leyenda de estados">
        @for (e of leyenda; track e.estado) {
          <li>
            <span class="muestra" [style.background]="e.color"></span>
            {{ e.etiqueta }}
          </li>
        }
      </ul>
    </div>
  `,
  styles: `
    .odontograma {
      display: grid;
      gap: 0.75rem;
    }

    .ayuda {
      margin: 0;
      font-size: 0.8rem;
    }

    h3 {
      margin: 0 0 0.35rem;
      font-size: 0.9rem;
    }

    .fila {
      display: grid;
      gap: 0.2rem;
      margin-bottom: 0.35rem;
    }

    .diente {
      display: grid;
      justify-items: center;
      gap: 0.1rem;
      padding: 0.15rem;
      border: 2px solid transparent;
      border-radius: 0.4rem;
      background: transparent;
      cursor: pointer;
      min-width: 0;
    }

    .diente svg {
      width: 100%;
      max-width: 2.6rem;
      height: auto;
    }

    .diente:hover {
      background: var(--p-surface-100, #f1f5f9);
    }

    .diente:focus-visible {
      outline: 3px solid var(--p-primary-500, #2563eb);
      outline-offset: 1px;
    }

    .diente.seleccionado {
      border-color: var(--p-primary-600, #2563eb);
      background: var(--p-primary-50, #eff6ff);
    }

    .numero {
      font-size: 0.7rem;
      font-weight: 600;
      color: var(--p-surface-700, #334155);
    }

    .leyenda {
      list-style: none;
      display: flex;
      flex-wrap: wrap;
      gap: 0.35rem 0.9rem;
      margin: 0;
      padding: 0;
      font-size: 0.78rem;
    }

    .muestra {
      display: inline-block;
      width: 0.8rem;
      height: 0.8rem;
      border: 1px solid #475569;
      border-radius: 0.15rem;
      margin-right: 0.25rem;
      vertical-align: -0.1rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Odontograma {
  readonly datos = input.required<OdontogramaDatos>();
  readonly seleccionada = model<number | null>(null);
  readonly incluirTemporales = input(false);

  protected readonly enfocada = signal<number | null>(null);
  protected readonly leyenda = (Object.keys(COLOR_ESTADO_PIEZA) as EstadoPieza[]).map((estado) => ({
    estado,
    etiqueta: ETIQUETA_ESTADO_PIEZA[estado],
    color: COLOR_ESTADO_PIEZA[estado],
  }));

  private readonly porNumero = computed(() => {
    const mapa = new Map<number, PiezaOdontograma>();
    for (const p of this.datos().piezas) {
      mapa.set(p.pieza, p);
    }
    return mapa;
  });

  protected readonly grupos = computed(() => {
    const permanentes = FILAS_PERMANENTES.map((fila) => fila.map((n) => this.vista(n)));
    const grupos = [
      { titulo: 'Dentición permanente', filas: permanentes, todas: FILAS_PERMANENTES },
    ];
    const temporales =
      this.incluirTemporales() || this.datos().edadAnios === null || this.datos().edadAnios! < 13;
    if (temporales || this.datos().piezas.some((p) => p.dentadura === 'TEMPORAL')) {
      grupos.push({
        titulo: 'Dentición temporal',
        filas: FILAS_TEMPORALES.map((fila) => fila.map((n) => this.vista(n))),
        todas: FILAS_TEMPORALES,
      });
    }
    return grupos;
  });

  protected readonly enfocable = computed(() => this.seleccionada() ?? this.enfocada() ?? 16);

  protected colorCompleto(estado: EstadoPieza): string {
    return COLOR_ESTADO_PIEZA[estado];
  }

  protected elegir(numero: number): void {
    this.seleccionada.set(numero);
  }

  protected teclado(evento: KeyboardEvent, filas: readonly (readonly number[])[]): void {
    const actual = Number((evento.target as HTMLElement).closest('button')?.dataset['pieza']);
    const movimientos: Record<string, [number, number]> = {
      ArrowLeft: [-1, 0],
      ArrowRight: [1, 0],
      ArrowUp: [0, -1],
      ArrowDown: [0, 1],
    };
    const mov = movimientos[evento.key];
    if (!mov || Number.isNaN(actual)) {
      return;
    }
    evento.preventDefault();
    const destino = vecina(actual, filas, mov[0], mov[1]);
    if (destino !== null) {
      const documento = (evento.currentTarget as HTMLElement).ownerDocument;
      this.enfocada.set(destino);
      queueMicrotask(() =>
        documento.querySelector<HTMLElement>(`button[data-pieza="${destino}"]`)?.focus(),
      );
    }
  }

  private vista(numero: number): DienteVista {
    const pieza = this.porNumero().get(numero);
    const completo = pieza?.estado ?? null;
    const poligonos = poligonosDiente(numero);
    return {
      numero,
      descripcion: descripcionPieza(numero, pieza),
      ausente: completo === 'AUSENTE',
      completo: completo === 'SANO' ? null : completo,
      poligonos: SUPERFICIES.map((superficie) => ({
        superficie,
        puntos: poligonos[superficie],
        color:
          COLOR_ESTADO_PIEZA[
            completo === 'AUSENTE' ? 'SANO' : estadoDeSuperficie(pieza, superficie)
          ],
      })),
    };
  }
}
