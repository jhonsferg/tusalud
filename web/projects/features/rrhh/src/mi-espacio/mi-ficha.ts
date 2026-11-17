import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_AREA_LABORAL,
  ETIQUETA_CONTRATO,
  ETIQUETA_ESTADO_EMPLEADO,
  RrhhStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { severidadEmpleado } from '../shared/ausencia';

@Component({
  selector: 'app-mi-ficha',
  imports: [CurrencyPipe, DatePipe, Tag, EmptyState],
  template: `
    @if (sinFicha()) {
      <div class="panel">
        <app-empty-state
          icon="pi-id-card"
          title="Aún no tienes una ficha laboral"
          message="Recursos humanos debe registrar tu ficha. Consúltalo con ellos si crees que falta."
        />
      </div>
    } @else if (store.miFicha.value(); as f) {
      <section class="panel">
        <h2>{{ f.nombreCompleto }}</h2>
        <p class="text-muted">{{ f.cargo }} · {{ area[f.area] }}</p>
        <dl>
          <div>
            <dt>Correo</dt>
            <dd>{{ f.email }}</dd>
          </div>
          <div>
            <dt>Contrato</dt>
            <dd>{{ contrato[f.tipoContrato] }}</dd>
          </div>
          <div>
            <dt>Ingreso</dt>
            <dd>{{ f.fechaIngreso | date: 'd MMM y' }}</dd>
          </div>
          @if (f.fechaFinContrato) {
            <div>
              <dt>Fin de contrato</dt>
              <dd>{{ f.fechaFinContrato | date: 'd MMM y' }}</dd>
            </div>
          }
          <div>
            <dt>Estado</dt>
            <dd><p-tag [value]="estado[f.estado]" [severity]="severidad(f.estado)" /></dd>
          </div>
          <div>
            <dt>Vacaciones anuales</dt>
            <dd>{{ f.diasVacacionesAnuales }} días</dd>
          </div>
          @if (f.sueldo != null) {
            <div>
              <dt>Sueldo</dt>
              <dd>{{ f.sueldo | currency: 'PEN' : 'symbol-narrow' }}</dd>
            </div>
          }
          @if (f.contactoEmergenciaNombre) {
            <div>
              <dt>Contacto de emergencia</dt>
              <dd>{{ f.contactoEmergenciaNombre }} {{ f.contactoEmergenciaTelefono }}</dd>
            </div>
          }
        </dl>
      </section>
    }
  `,
  styles: `
    h2 {
      margin: 0;
    }

    dl {
      display: grid;
      gap: 0.5rem;
      margin: 1rem 0 0;
      max-width: 36rem;

      div {
        display: flex;
        justify-content: space-between;
        gap: 1rem;
        padding: 0.55rem 0.8rem;
        border-radius: 10px;
        background: var(--p-content-hover-background);
      }

      dt {
        color: var(--p-text-muted-color);
      }

      dd {
        margin: 0;
        font-weight: 600;
        text-align: right;
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MiFicha implements OnInit {
  protected readonly store = inject(RrhhStore);
  protected readonly area = ETIQUETA_AREA_LABORAL;
  protected readonly contrato = ETIQUETA_CONTRATO;
  protected readonly estado = ETIQUETA_ESTADO_EMPLEADO;
  protected readonly severidad = severidadEmpleado;
  protected readonly sinFicha = computed(() => this.store.miFicha.error()?.status === 404);

  ngOnInit(): void {
    this.store.cargarMiFicha();
  }
}
