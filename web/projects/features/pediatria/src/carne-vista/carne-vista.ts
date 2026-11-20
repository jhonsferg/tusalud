import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import { CarneVacunacion, DosisCarne, RegistroVacuna } from '@tusalud/core';
import {
  ETIQUETA_ESTADO_DOSIS,
  SEVERIDAD_ESTADO_DOSIS,
  edadLegible,
  fechaLegible,
} from '../pediatria.utils';

@Component({
  selector: 'app-carne-vista',
  imports: [Button, Message, Tag],
  template: `
    @let c = carne();
    <section class="carne" aria-label="Carné de vacunación">
      <header class="cabecera">
        <div>
          <h2>{{ c.pacienteNombre }}</h2>
          <p class="text-muted">
            DNI {{ c.dni }} · {{ edad(c.edadMeses) }}
            @if (c.fechaNacimiento) {
              · nació el {{ fecha(c.fechaNacimiento) }}
            }
          </p>
        </div>
        <dl class="resumen">
          <div>
            <dt>Aplicadas</dt>
            <dd>{{ c.aplicadas }}</dd>
          </div>
          <div>
            <dt>Atrasadas</dt>
            <dd [class.alerta]="c.atrasadas > 0">{{ c.atrasadas }}</dd>
          </div>
          <div>
            <dt>Próximas</dt>
            <dd>{{ c.proximas }}</dd>
          </div>
        </dl>
      </header>
      @if (c.esquemaPorValidar) {
        <p-message severity="warn" icon="pi pi-info-circle" size="small">
          El esquema "{{ c.esquemaNombre }}" está pendiente de validación con la norma técnica
          vigente.
        </p-message>
      }
      @if (c.proximaDosis; as p) {
        <p class="proxima" role="status">
          Próxima dosis: <strong>{{ p.vacunaNombre }}, {{ p.etiqueta }}</strong
          >, recomendada el
          {{ fecha(p.fechaRecomendada) }}
          @if (p.atrasoDias) {
            · atrasada {{ p.atrasoDias }} días
          }
        </p>
      }
      <div class="tabla-envoltorio">
        <table>
          <caption class="sr-only">
            Dosis del esquema de vacunación
          </caption>
          <thead>
            <tr>
              <th scope="col">Vacuna</th>
              <th scope="col">Dosis</th>
              <th scope="col">Recomendada</th>
              <th scope="col">Estado</th>
              <th scope="col">Aplicación</th>
              @if (permiteRegistrar()) {
                <th scope="col"><span class="sr-only">Acciones</span></th>
              }
            </tr>
          </thead>
          <tbody>
            @for (d of c.dosis; track d.vacunaId + d.numeroDosis) {
              <tr>
                <th scope="row">{{ d.vacunaNombre }}</th>
                <td>{{ d.etiqueta }}</td>
                <td>{{ fecha(d.fechaRecomendada) }}</td>
                <td>
                  <p-tag [value]="etiquetaEstado[d.estado]" [severity]="severidad[d.estado]" />
                </td>
                <td>
                  @if (registro(d); as r) {
                    {{ fecha(r.fechaAplicacion) }} · lote {{ r.lote }}<br />
                    <small class="text-muted"
                      >{{ r.establecimiento }} · {{ r.responsableNombre }}</small
                    >
                  } @else {
                    <span class="text-muted">—</span>
                  }
                </td>
                @if (permiteRegistrar()) {
                  <td class="acciones">
                    @if (d.estado === 'APLICADA' && d.registroId && permiteAnular()) {
                      <p-button
                        label="Anular"
                        size="small"
                        severity="danger"
                        [text]="true"
                        [ariaLabel]="'Anular la dosis ' + d.numeroDosis + ' de ' + d.vacunaNombre"
                        (onClick)="anular.emit(d)"
                      />
                    } @else if (d.estado !== 'APLICADA') {
                      <p-button
                        label="Registrar"
                        size="small"
                        [outlined]="true"
                        [ariaLabel]="
                          'Registrar la dosis ' + d.numeroDosis + ' de ' + d.vacunaNombre
                        "
                        (onClick)="registrar.emit(d)"
                      />
                    }
                  </td>
                }
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
  styles: `
    .carne {
      display: grid;
      gap: 0.75rem;
    }

    .cabecera {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
    }

    h2,
    p {
      margin: 0;
    }

    .resumen {
      display: flex;
      gap: 1.25rem;
      margin: 0;
    }

    .resumen dt {
      font-size: 0.75rem;
      color: var(--p-surface-600, #475569);
    }

    .resumen dd {
      margin: 0;
      font-size: 1.4rem;
      font-weight: 700;
    }

    .resumen dd.alerta {
      color: var(--p-red-700, #b91c1c);
    }

    .proxima {
      padding: 0.6rem 0.8rem;
      border-radius: 0.5rem;
      background: var(--p-primary-50, #eff6ff);
    }

    .tabla-envoltorio {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.92rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.45rem 0.6rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
      vertical-align: top;
    }

    .acciones {
      white-space: nowrap;
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
export class CarneVista {
  readonly carne = input.required<CarneVacunacion>();
  readonly permiteRegistrar = input(false);
  readonly permiteAnular = input(false);
  readonly registrar = output<DosisCarne>();
  readonly anular = output<DosisCarne>();

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_DOSIS;
  protected readonly severidad = SEVERIDAD_ESTADO_DOSIS;
  protected readonly fecha = fechaLegible;
  protected readonly edad = edadLegible;

  protected registro(d: DosisCarne): RegistroVacuna | undefined {
    return d.registroId ? this.carne().registros.find((r) => r.id === d.registroId) : undefined;
  }
}
