import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { ItemPlanOdontologico, PlanOdontologico } from '@tusalud/core';
import {
  ETIQUETA_ESTADO_ITEM,
  ETIQUETA_ESTADO_PLAN,
  ETIQUETA_SUPERFICIE,
  SEVERIDAD_ESTADO_PLAN,
  fechaLegible,
  soles,
} from '../odontologia.utils';

@Component({
  selector: 'app-plan-detalle',
  imports: [Button, Tag],
  template: `
    @let p = plan();
    <section class="plan" [attr.aria-label]="'Plan odontológico de ' + p.pacienteNombre">
      <header>
        <div>
          <h3>Plan de {{ fecha(p.creadoEn) }}</h3>
          <p class="text-muted">Odontólogo: {{ p.medicoNombre }}</p>
        </div>
        <p-tag [value]="etiquetaPlan[p.estado]" [severity]="severidadPlan[p.estado]" />
      </header>
      @if (p.notas) {
        <p>{{ p.notas }}</p>
      }
      @if (p.motivoRechazo) {
        <p class="rechazo">Motivo: {{ p.motivoRechazo }}</p>
      }
      <dl class="totales">
        <div>
          <dt>Presupuesto</dt>
          <dd>{{ soles(p.total) }}</dd>
        </div>
        <div>
          <dt>Pagado</dt>
          <dd>{{ soles(p.pagado) }}</dd>
        </div>
        <div>
          <dt>Saldo</dt>
          <dd>{{ soles(p.saldo) }}</dd>
        </div>
      </dl>
      @if (p.items.length) {
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Procedimientos del plan
            </caption>
            <thead>
              <tr>
                <th scope="col">Procedimiento</th>
                <th scope="col">Pieza</th>
                <th scope="col">Precio</th>
                <th scope="col">Estado</th>
                @if (editable()) {
                  <th scope="col"><span class="sr-only">Acciones</span></th>
                }
              </tr>
            </thead>
            <tbody>
              @for (i of p.items; track i.id) {
                <tr>
                  <th scope="row">
                    {{ i.nombre }}
                    @if (i.requiereConsentimiento) {
                      <small class="text-muted block">
                        Consentimiento: {{ i.consentimientoId ? 'vinculado' : 'pendiente' }}
                      </small>
                    }
                  </th>
                  <td>{{ pieza(i) }}</td>
                  <td>{{ soles(i.precio) }}</td>
                  <td>{{ etiquetaItem[i.estado] }}</td>
                  @if (editable()) {
                    <td class="acciones">
                      @if (p.estado === 'BORRADOR') {
                        <p-button
                          label="Quitar"
                          size="small"
                          severity="danger"
                          [text]="true"
                          [ariaLabel]="'Quitar ' + i.nombre + ' del plan'"
                          (onClick)="quitar.emit(i)"
                        />
                      }
                      @if (
                        i.requiereConsentimiento && i.estado === 'PENDIENTE' && !i.consentimientoId
                      ) {
                        <p-button
                          label="Vincular consentimiento"
                          size="small"
                          [text]="true"
                          [ariaLabel]="'Vincular un consentimiento a ' + i.nombre"
                          (onClick)="vincular.emit(i)"
                        />
                      }
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <p class="text-muted">El plan aún no tiene procedimientos.</p>
      }
      @if (p.sesiones.length) {
        <h4>Sesiones</h4>
        <ul class="lista">
          @for (s of p.sesiones; track s.id) {
            <li>
              <strong>{{ fecha(s.fecha) }}</strong> · {{ s.medicoNombre }} · {{ s.itemIds.length }}
              {{ s.itemIds.length === 1 ? 'procedimiento' : 'procedimientos' }}
              @if (s.notas) {
                <small class="text-muted block">{{ s.notas }}</small>
              }
            </li>
          }
        </ul>
      }
      @if (p.pagos.length) {
        <h4>Pagos</h4>
        <ul class="lista">
          @for (g of p.pagos; track g.id) {
            <li>
              <strong>{{ soles(g.monto) }}</strong> · {{ g.metodo }} · {{ fecha(g.registradoEn) }}
              <small class="text-muted"> · {{ g.registradoPor }}</small>
            </li>
          }
        </ul>
      }
      <ng-content />
    </section>
  `,
  styles: `
    .plan {
      display: grid;
      gap: 0.6rem;
    }

    header {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
    }

    h3,
    h4,
    p {
      margin: 0;
    }

    .rechazo {
      color: var(--p-red-700, #b91c1c);
    }

    .totales {
      display: flex;
      gap: 1.5rem;
      margin: 0;
    }

    .totales dt {
      font-size: 0.75rem;
      color: var(--p-surface-600, #475569);
    }

    .totales dd {
      margin: 0;
      font-weight: 700;
      font-size: 1.1rem;
    }

    .tabla {
      overflow-x: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.9rem;
    }

    th,
    td {
      text-align: left;
      padding: 0.4rem 0.5rem;
      border-bottom: 1px solid var(--p-surface-200, #e2e8f0);
      vertical-align: top;
    }

    .block {
      display: block;
    }

    .acciones {
      white-space: nowrap;
    }

    .lista {
      margin: 0;
      padding-left: 1.1rem;
      display: grid;
      gap: 0.25rem;
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
export class PlanDetalle {
  readonly plan = input.required<PlanOdontologico>();
  readonly editable = input(false);
  readonly quitar = output<ItemPlanOdontologico>();
  readonly vincular = output<ItemPlanOdontologico>();

  protected readonly etiquetaPlan = ETIQUETA_ESTADO_PLAN;
  protected readonly severidadPlan = SEVERIDAD_ESTADO_PLAN;
  protected readonly etiquetaItem = ETIQUETA_ESTADO_ITEM;
  protected readonly fecha = fechaLegible;
  protected readonly soles = soles;

  protected pieza(i: ItemPlanOdontologico): string {
    if (i.pieza === null) {
      return 'Boca completa';
    }
    return i.superficie ? `${i.pieza} · ${ETIQUETA_SUPERFICIE[i.superficie]}` : String(i.pieza);
  }
}
