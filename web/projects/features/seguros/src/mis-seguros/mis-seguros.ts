import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_CATEGORIA_SERVICIO,
  ETIQUETA_ESTADO_POLIZA,
  ETIQUETA_ESTADO_PREAUTORIZACION,
  SegurosStore,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { SEVERIDAD_POLIZA, SEVERIDAD_PREAUTORIZACION, textoCobertura } from '../seguros.utils';

@Component({
  selector: 'app-mis-seguros',
  imports: [CurrencyPipe, DatePipe, Tag, EmptyState, PageHeader],
  template: `
    <app-page-header
      title="Mis seguros"
      subtitle="Tu cobertura y las autorizaciones de tu seguro"
      icon="pi-shield"
    />

    <section class="grupo" aria-label="Mis pólizas">
      @for (p of store.misPolizas.value() ?? []; track p.id) {
        <article class="panel poliza">
          <header>
            <div>
              <h3>{{ p.aseguradoraNombre }}</h3>
              <small class="text-muted"
                >{{ p.convenioNombre }}{{ p.plan ? ' · ' + p.plan : '' }}</small
              >
            </div>
            <p-tag [value]="etiquetaEstado[p.estado]" [severity]="severidad[p.estado]" />
          </header>
          <dl>
            <div>
              <dt>Número de póliza</dt>
              <dd class="sensible">{{ p.numero }}</dd>
            </div>
            <div>
              <dt>Titular</dt>
              <dd class="sensible">{{ p.titularNombre }} · {{ p.parentesco }}</dd>
            </div>
            <div>
              <dt>Vigencia</dt>
              <dd>
                {{ p.vigenciaDesde | date: 'd MMM y' }} al {{ p.vigenciaHasta | date: 'd MMM y' }}
              </dd>
            </div>
          </dl>
          @if (p.diasParaVencer != null && p.diasParaVencer <= 30) {
            <p class="aviso" role="status">
              Tu póliza vence en {{ p.diasParaVencer }} días. Renueva tu seguro para seguir usando
              la cobertura.
            </p>
          }
          @if (p.estado === 'VENCIDA') {
            <p class="aviso" role="status">
              Tu póliza está vencida. Los servicios se cobran como particulares hasta que la
              renueves.
            </p>
          }
          @if (p.estado === 'SUSPENDIDA') {
            <p class="aviso" role="status">
              Tu póliza está suspendida. Consulta con tu aseguradora.
            </p>
          }
          <table aria-label="Cobertura por categoría">
            <thead>
              <tr>
                <th>Servicio</th>
                <th>Cobertura</th>
                <th>Tope por servicio</th>
                <th>Preautorización</th>
              </tr>
            </thead>
            <tbody>
              @for (c of p.coberturas; track c.categoria) {
                <tr>
                  <td>{{ etiquetaCategoria[c.categoria] }}</td>
                  <td>{{ texto(c.tipo, c.valor) }}</td>
                  <td>
                    {{
                      c.topePorServicio
                        ? (c.topePorServicio | currency: 'PEN' : 'symbol-narrow')
                        : 'Sin tope'
                    }}
                  </td>
                  <td>{{ c.requierePreautorizacion ? 'La solicita tu médico' : 'No la exige' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </article>
      } @empty {
        <div class="panel">
          <app-empty-state
            icon="pi-shield"
            title="Sin seguros registrados"
            message="Si tienes un seguro, pide en recepción que registren tu póliza."
          />
        </div>
      }
    </section>

    @if ((store.misPreautorizaciones.value() ?? []).length > 0) {
      <section class="panel" aria-label="Mis autorizaciones">
        <h3>Autorizaciones de tu seguro</h3>
        <ul class="autorizaciones">
          @for (a of store.misPreautorizaciones.value() ?? []; track a.id) {
            <li>
              <div>
                <strong>{{ a.descripcion }}</strong>
                <small class="text-muted block"
                  >{{ etiquetaCategoria[a.categoria] }} ·
                  {{ a.solicitadaEn | date: 'd MMM y' }}</small
                >
                @if (a.codigoAutorizacion) {
                  <small class="text-muted block"
                    >Código {{ a.codigoAutorizacion }}
                    @if (a.vigenteHasta) {
                      · válido hasta {{ a.vigenteHasta | date: 'd MMM y' }}
                    }
                  </small>
                }
                @if (a.motivoRechazo) {
                  <small class="text-muted block">{{ a.motivoRechazo }}</small>
                }
              </div>
              <p-tag
                [value]="etiquetaPreautorizacion[a.estado]"
                [severity]="severidadPreautorizacion[a.estado]"
              />
            </li>
          }
        </ul>
      </section>
    }
  `,
  styles: `
    .grupo {
      display: grid;
      gap: var(--sp-4);
    }
    .poliza header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-3);
    }
    h3 {
      margin: 0;
      font-size: 1.05rem;
    }
    dl {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
      gap: var(--sp-3);
      margin: var(--sp-3) 0;
    }
    dt {
      color: var(--p-text-muted-color);
      font-size: 0.8rem;
    }
    dd {
      margin: 0;
      font-weight: 600;
    }
    .aviso {
      padding: var(--sp-2) var(--sp-3);
      border-radius: var(--radio-sm);
      background: var(--estado-aviso-fondo);
      color: var(--estado-aviso);
      font-weight: 600;
    }
    table {
      width: 100%;
      border-collapse: collapse;
    }
    th {
      padding: 0.4rem;
      text-align: left;
      font-size: 0.78rem;
      color: var(--p-text-muted-color);
    }
    td {
      padding: 0.5rem 0.4rem;
      border-top: 1px solid var(--p-content-border-color);
    }
    .panel + .panel {
      margin-top: var(--sp-4);
    }
    .autorizaciones {
      display: flex;
      flex-direction: column;
      gap: var(--sp-2);
      margin: var(--sp-3) 0 0;
      padding: 0;
      list-style: none;
    }
    .autorizaciones li {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: var(--sp-3);
      padding: var(--sp-2) 0;
      border-top: 1px solid var(--p-content-border-color);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisSeguros implements OnInit {
  protected readonly store = inject(SegurosStore);
  protected readonly etiquetaCategoria: Readonly<Record<string, string>> =
    ETIQUETA_CATEGORIA_SERVICIO;
  protected readonly etiquetaEstado: Readonly<Record<string, string>> = ETIQUETA_ESTADO_POLIZA;
  protected readonly severidad = SEVERIDAD_POLIZA;
  protected readonly etiquetaPreautorizacion: Readonly<Record<string, string>> =
    ETIQUETA_ESTADO_PREAUTORIZACION;
  protected readonly severidadPreautorizacion = SEVERIDAD_PREAUTORIZACION;
  protected readonly texto = textoCobertura;

  ngOnInit(): void {
    this.store.cargarMisPolizas();
    this.store.cargarMisPreautorizaciones();
  }
}
