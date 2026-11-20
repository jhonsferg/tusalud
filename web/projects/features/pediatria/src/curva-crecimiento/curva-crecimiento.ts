import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Tag } from 'primeng/tag';

import { CurvaCrecimiento as Curva } from '@tusalud/core';
import {
  COLOR_NIVEL,
  ETIQUETA_NIVEL,
  Lienzo,
  SEVERIDAD_NIVEL,
  escalasDeCurva,
  fechaLegible,
  posicionX,
  posicionY,
  referenciaVisible,
  resumenAccesible,
  trazo,
} from '../pediatria.utils';

const LIENZO: Lienzo = { ancho: 560, alto: 300, margen: 36 };
const LINEAS = [
  { clave: 'p3', etiqueta: 'P3', ancho: 1, color: '#94a3b8' },
  { clave: 'p15', etiqueta: 'P15', ancho: 1, color: '#64748b' },
  { clave: 'p50', etiqueta: 'P50', ancho: 2, color: '#0f766e' },
  { clave: 'p85', etiqueta: 'P85', ancho: 1, color: '#64748b' },
  { clave: 'p97', etiqueta: 'P97', ancho: 1, color: '#94a3b8' },
] as const;

@Component({
  selector: 'app-curva-crecimiento',
  imports: [DecimalPipe, Tag],
  template: `
    <figure class="curva">
      <figcaption>
        <strong>{{ curva().etiqueta }}</strong>
        <span class="text-muted">en {{ curva().unidad }}</span>
      </figcaption>
      @if (grafico(); as g) {
        <svg
          [attr.viewBox]="'0 0 ' + lienzo.ancho + ' ' + lienzo.alto"
          role="img"
          [attr.aria-label]="resumen()"
          preserveAspectRatio="xMidYMid meet"
        >
          <line
            [attr.x1]="lienzo.margen"
            [attr.y1]="lienzo.alto - lienzo.margen"
            [attr.x2]="lienzo.ancho - lienzo.margen"
            [attr.y2]="lienzo.alto - lienzo.margen"
            stroke="#cbd5e1"
          />
          <line
            [attr.x1]="lienzo.margen"
            [attr.y1]="lienzo.margen"
            [attr.x2]="lienzo.margen"
            [attr.y2]="lienzo.alto - lienzo.margen"
            stroke="#cbd5e1"
          />
          @for (marca of g.marcasX; track marca.edad) {
            <text [attr.x]="marca.x" [attr.y]="lienzo.alto - 12" text-anchor="middle" class="marca">
              {{ marca.texto }}
            </text>
          }
          @for (marca of g.marcasY; track marca.valor) {
            <text
              [attr.x]="lienzo.margen - 6"
              [attr.y]="marca.y + 3"
              text-anchor="end"
              class="marca"
            >
              {{ marca.valor | number: '1.0-1' }}
            </text>
          }
          @for (linea of g.lineas; track linea.etiqueta) {
            <path
              [attr.d]="linea.d"
              fill="none"
              [attr.stroke]="linea.color"
              [attr.stroke-width]="linea.ancho"
            />
            <text [attr.x]="linea.xFin + 3" [attr.y]="linea.yFin + 3" class="marca">
              {{ linea.etiqueta }}
            </text>
          }
          @if (g.recorrido) {
            <path
              [attr.d]="g.recorrido"
              fill="none"
              stroke="#1d4ed8"
              stroke-width="1.5"
              stroke-dasharray="4 3"
            />
          }
          @for (punto of g.puntos; track punto.id) {
            <circle
              [attr.cx]="punto.x"
              [attr.cy]="punto.y"
              r="5"
              [attr.fill]="punto.color"
              stroke="#fff"
              stroke-width="1.5"
            >
              <title>{{ punto.titulo }}</title>
            </circle>
          }
          <text
            [attr.x]="lienzo.ancho / 2"
            [attr.y]="lienzo.alto - 1"
            text-anchor="middle"
            class="marca"
          >
            Edad en meses
          </text>
        </svg>
      } @else {
        <p class="text-muted sin-datos">No hay datos de referencia para mostrar esta curva.</p>
      }
      <details class="tabla">
        <summary>Ver las mediciones en una tabla</summary>
        @if (curva().puntos.length === 0) {
          <p class="text-muted">Sin mediciones registradas.</p>
        } @else {
          <table>
            <caption class="sr-only">
              Mediciones de
              {{
                curva().etiqueta
              }}
            </caption>
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Edad en meses</th>
                <th scope="col">Valor</th>
                <th scope="col">Percentil</th>
                <th scope="col">Z</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (p of curva().puntos; track p.medicionId) {
                <tr>
                  <td>{{ fecha(p.fecha) }}</td>
                  <td>{{ p.edadMeses | number: '1.0-1' }}</td>
                  <td>{{ p.valor }} {{ curva().unidad }}</td>
                  <td>{{ p.percentil === null ? '—' : (p.percentil | number: '1.0-0') }}</td>
                  <td>{{ p.z === null ? '—' : (p.z | number: '1.1-1') }}</td>
                  <td>
                    <p-tag [value]="etiquetaNivel[p.nivel]" [severity]="severidadNivel[p.nivel]" />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </details>
    </figure>
  `,
  styles: `
    .curva {
      margin: 0;
      display: grid;
      gap: 0.5rem;
    }

    figcaption {
      display: flex;
      gap: 0.5rem;
      align-items: baseline;
    }

    svg {
      width: 100%;
      height: auto;
      max-height: 22rem;
      background: var(--p-surface-0, #fff);
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-radius: 0.5rem;
    }

    .marca {
      font-size: 10px;
      fill: #475569;
    }

    .sin-datos {
      margin: 0;
    }

    .tabla summary {
      cursor: pointer;
      font-weight: 600;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 0.5rem;
      font-size: 0.9rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.3rem 0.5rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
    }

    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CurvaCrecimiento {
  readonly curva = input.required<Curva>();

  protected readonly lienzo = LIENZO;
  protected readonly etiquetaNivel = ETIQUETA_NIVEL;
  protected readonly severidadNivel = SEVERIDAD_NIVEL;
  protected readonly fecha = fechaLegible;
  protected readonly resumen = computed(() => resumenAccesible(this.curva()));

  protected readonly grafico = computed(() => {
    const curva = this.curva();
    const escalas = escalasDeCurva(curva);
    if (!escalas) {
      return null;
    }
    const maxEdad = curva.puntos.length ? Math.max(...curva.puntos.map((p) => p.edadMeses)) : 12;
    const ref = referenciaVisible(curva.referencia, maxEdad * 1.15 + 6);
    const x = (edad: number) => posicionX(edad, escalas, LIENZO);
    const y = (valor: number) => posicionY(valor, escalas, LIENZO);
    const lineas = LINEAS.map((l) => {
      const puntos = ref.map((r) => ({ x: x(r.edadMeses), y: y(r[l.clave]) }));
      const fin = puntos[puntos.length - 1];
      return { ...l, d: trazo(puntos), xFin: fin.x, yFin: fin.y };
    });
    const puntos = curva.puntos.map((p) => ({
      id: p.medicionId,
      x: x(p.edadMeses),
      y: y(p.valor),
      color: COLOR_NIVEL[p.nivel],
      titulo: `${fechaLegible(p.fecha)}: ${p.valor} ${curva.unidad}${
        p.percentil === null ? '' : `, percentil ${Math.round(p.percentil)}`
      }`,
    }));
    const paso = Math.max(1, Math.round((escalas.xMax - escalas.xMin) / 6 / 6) * 6);
    const marcasX: { edad: number; x: number; texto: string }[] = [];
    for (let edad = Math.ceil(escalas.xMin); edad <= escalas.xMax; edad += paso) {
      marcasX.push({ edad, x: x(edad), texto: String(edad) });
    }
    const marcasY = [0, 1, 2, 3, 4].map((i) => {
      const valor = escalas.yMin + ((escalas.yMax - escalas.yMin) * i) / 4;
      return { valor, y: y(valor) };
    });
    return {
      lineas,
      puntos,
      marcasX,
      marcasY,
      recorrido: puntos.length > 1 ? trazo(puntos) : '',
    };
  });
}
