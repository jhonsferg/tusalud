import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';

import { AlertaReceta } from '@tusalud/core';
import {
  ETIQUETA_SEVERIDAD_ALERTA,
  ETIQUETA_TIPO_ALERTA,
  SEVERIDAD_ALERTA,
  ordenarAlertas,
} from '../recetas.utils';

@Component({
  selector: 'app-panel-alertas',
  imports: [FormsModule, Message, Tag, Textarea],
  template: `
    <section class="panel-alertas" aria-labelledby="titulo-alertas" aria-live="polite">
      <h3 id="titulo-alertas">Alertas de prescripción</h3>
      @if (cargando()) {
        <p class="text-muted" role="status">Revisando alergias, interacciones y dosis…</p>
      } @else if (ordenadas().length === 0) {
        <p-message severity="success">Sin alertas para esta prescripción</p-message>
      } @else {
        <p class="text-muted nota">
          Datos de demostración, por validar con un químico farmacéutico. Las alertas graves y
          contraindicadas exigen una justificación que queda registrada.
        </p>
        <ul class="alertas">
          @for (a of ordenadas(); track a.clave) {
            <li class="alerta" [attr.data-severidad]="a.severidad">
              <div class="cabecera">
                <p-tag [value]="severidad[a.severidad]" [severity]="color[a.severidad]" />
                <strong>{{ tipo[a.tipo] }}</strong>
                <span class="text-muted">{{ a.medicamento }}</span>
              </div>
              <p>{{ a.mensaje }}</p>
              @if (a.recomendacion) {
                <p class="text-muted">{{ a.recomendacion }}</p>
              }
              @if (a.requiereJustificacion) {
                <label class="justificacion" [for]="'justificacion-' + $index">
                  Justificación para continuar, mínimo {{ a.minimoJustificacion }} caracteres
                  <textarea
                    pTextarea
                    rows="2"
                    maxlength="500"
                    [id]="'justificacion-' + $index"
                    [ngModel]="justificaciones()[a.clave] ?? ''"
                    (ngModelChange)="cambio.emit({ clave: a.clave, texto: $event })"
                    [attr.aria-invalid]="falta(a)"
                  ></textarea>
                  <small [class.error]="falta(a)">
                    {{ (justificaciones()[a.clave] ?? '').trim().length }} de
                    {{ a.minimoJustificacion }}
                  </small>
                </label>
              }
            </li>
          }
        </ul>
      }
    </section>
  `,
  styles: `
    .panel-alertas {
      display: grid;
      gap: 0.75rem;
    }

    h3 {
      margin: 0;
    }

    .nota {
      margin: 0;
      font-size: 0.85rem;
    }

    .alertas {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.75rem;
    }

    .alerta {
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-left-width: 0.35rem;
      border-radius: 0.5rem;
      padding: 0.6rem 0.8rem;
      display: grid;
      gap: 0.3rem;
    }

    .alerta[data-severidad='CONTRAINDICADA'] {
      border-left-color: #7f1d1d;
      background: #fef2f2;
    }

    .alerta[data-severidad='GRAVE'] {
      border-left-color: #dc2626;
    }

    .alerta[data-severidad='MODERADA'] {
      border-left-color: #d97706;
    }

    .alerta[data-severidad='INFORMATIVA'] {
      border-left-color: #64748b;
    }

    .cabecera {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    p {
      margin: 0;
    }

    .justificacion {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
    }

    textarea {
      width: 100%;
    }

    .error {
      color: #b91c1c;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PanelAlertas {
  readonly alertas = input.required<readonly AlertaReceta[]>();
  readonly justificaciones = input<Readonly<Record<string, string>>>({});
  readonly cargando = input(false);

  readonly cambio = output<{ clave: string; texto: string }>();

  protected readonly severidad = ETIQUETA_SEVERIDAD_ALERTA;
  protected readonly color = SEVERIDAD_ALERTA;
  protected readonly tipo = ETIQUETA_TIPO_ALERTA;
  protected readonly ordenadas = computed(() => ordenarAlertas(this.alertas()));

  protected falta(alerta: AlertaReceta): boolean {
    return (this.justificaciones()[alerta.clave] ?? '').trim().length < alerta.minimoJustificacion;
  }
}
