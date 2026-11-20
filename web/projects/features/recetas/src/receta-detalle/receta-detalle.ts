import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { Tag } from 'primeng/tag';

import {
  ApiError,
  AuthService,
  DescargaService,
  NotificationService,
  RecetaElectronica,
  RecetasApiService,
} from '@tusalud/core';
import { DialogoMotivo } from '@tusalud/ui/dialogo-motivo';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_RECETA,
  ETIQUETA_SEVERIDAD_ALERTA,
  ETIQUETA_TIPO_ALERTA,
  SEVERIDAD_ALERTA,
  SEVERIDAD_ESTADO_RECETA,
  textoVigencia,
} from '../recetas.utils';

@Component({
  selector: 'app-receta-detalle',
  imports: [DatePipe, RouterLink, Button, Message, Tag, DialogoMotivo, PageHeader],
  template: `
    <app-page-header
      title="Receta electrónica"
      [subtitle]="receta()?.numero ?? 'Cargando'"
      icon="pi-file-edit"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    } @else if (receta(); as r) {
      @if (r.certificadoDePrueba) {
        <p-message severity="warn" class="aviso"
          >La firma es electrónica y de prueba: no equivale a una firma digital con certificado de
          una entidad acreditada ante INDECOPI.</p-message
        >
      }
      @if (r.estado === 'ANULADA') {
        <p-message severity="error" class="aviso"
          >Anulada el {{ r.anuladaEn | date: 'd MMM y, HH:mm' }}: {{ r.motivoAnulacion }}</p-message
        >
      }
      <div class="panel resumen">
        <div class="linea">
          <p-tag [value]="etiqueta[r.estado]" [severity]="severidad[r.estado]" />
          <span>{{ vigencia() }}</span>
          @if (r.controlEspecial) {
            <p-tag value="Control especial" severity="warn" />
          }
        </div>
        <dl>
          <div>
            <dt>Paciente</dt>
            <dd>{{ r.pacienteNombre }} · DNI {{ r.pacienteDni }}</dd>
          </div>
          <div>
            <dt>Médico</dt>
            <dd>{{ r.medicoNombre }} · CMP {{ r.medicoColegiatura }}</dd>
          </div>
          <div>
            <dt>Emisión</dt>
            <dd>{{ r.emitidaEn | date: 'd MMM y, HH:mm' }}</dd>
          </div>
          <div>
            <dt>Vigente hasta</dt>
            <dd>{{ r.vigenteHasta | date: 'd MMM y, HH:mm' }}</dd>
          </div>
          @if (r.diagnosticoCodigo) {
            <div>
              <dt>Diagnóstico</dt>
              <dd>{{ r.diagnosticoCodigo }} · {{ r.diagnosticoDescripcion }}</dd>
            </div>
          }
          @if (r.reemplazaNumero) {
            <div>
              <dt>Reemplaza a</dt>
              <dd>{{ r.reemplazaNumero }}</dd>
            </div>
          }
        </dl>
        <div class="acciones">
          <p-button
            label="Descargar PDF"
            icon="pi pi-download"
            [loading]="descargando()"
            (onClick)="descargar(r)"
          />
          @if (medico && r.estado === 'EMITIDA') {
            <p-button
              label="Anular receta"
              icon="pi pi-ban"
              severity="danger"
              [outlined]="true"
              (onClick)="anulando.set(true)"
            />
          }
          @if (medico && r.estado === 'ANULADA') {
            <p-button
              label="Emitir un reemplazo"
              icon="pi pi-refresh"
              severity="secondary"
              [outlined]="true"
              [routerLink]="['/recetas/emitir', r.citaId]"
              [queryParams]="{ reemplaza: r.id }"
            />
          }
        </div>
      </div>

      <div class="panel">
        <h3>Medicamentos</h3>
        <div class="tabla">
          <table>
            <caption class="sr-only">
              Medicamentos de la receta
            </caption>
            <thead>
              <tr>
                <th scope="col">Medicamento</th>
                <th scope="col">Indicación</th>
                <th scope="col">Cantidad</th>
                <th scope="col">Dispensado</th>
              </tr>
            </thead>
            <tbody>
              @for (i of r.items; track i.id) {
                <tr>
                  <th scope="row">
                    {{ i.medicamento }}
                    @if (i.controlEspecial) {
                      <small class="control">Control especial</small>
                    }
                  </th>
                  <td>
                    {{ [i.dosis, i.frecuencia, i.duracion, i.via].filter(presente).join(' · ') }}
                  </td>
                  <td>{{ i.cantidad }}</td>
                  <td>{{ i.cantidadDispensada }} de {{ i.cantidad }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (r.indicaciones) {
          <p><strong>Indicaciones:</strong> {{ r.indicaciones }}</p>
        }
      </div>

      <div class="panel">
        <h3>Firma y verificación</h3>
        <dl>
          <div>
            <dt>Estado de la firma</dt>
            <dd>
              <p-tag
                [value]="r.firmaValida ? 'Válida' : 'No verificable'"
                [severity]="r.firmaValida ? 'success' : 'danger'"
              />
            </dd>
          </div>
          <div>
            <dt>Algoritmo</dt>
            <dd>{{ r.firmaAlgoritmo }}</dd>
          </div>
          <div>
            <dt>Certificado</dt>
            <dd>{{ r.certificadoSujeto }}</dd>
          </div>
          <div>
            <dt>Código de verificación pública</dt>
            <dd>
              <code>{{ r.codigoVerificacion }}</code>
            </dd>
          </div>
        </dl>
      </div>

      @if (medico && r.alertas.length > 0) {
        <div class="panel">
          <h3>Alertas al prescribir</h3>
          <ul class="alertas">
            @for (a of r.alertas; track a.clave) {
              <li>
                <p-tag
                  [value]="severidadAlerta[a.severidad]"
                  [severity]="colorAlerta[a.severidad]"
                />
                <strong>{{ tipoAlerta[a.tipo] }}</strong
                >: {{ a.mensaje }}
                @if (a.justificacion) {
                  <small class="block text-muted"
                    >Justificación de {{ a.justificadaPor }}: {{ a.justificacion }}</small
                  >
                }
              </li>
            }
          </ul>
        </div>
      }

      <app-dialogo-motivo
        [(visible)]="anulando"
        titulo="Anular receta"
        descripcion="La receta anulada no se puede dispensar. Se conserva el registro."
        confirmarLabel="Anular receta"
        severidad="danger"
        [cargando]="procesando()"
        (confirmar)="anular($event)"
      />
    }
  `,
  styles: `
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      white-space: nowrap;
      border: 0;
    }
    .aviso {
      display: block;
      margin-bottom: 1rem;
    }

    .panel {
      margin-bottom: 1rem;
    }

    h3 {
      margin-top: 0;
    }

    .linea {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-bottom: 0.75rem;
    }

    dl {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
      gap: 0.6rem 1.5rem;
      margin: 0 0 1rem;
    }

    dt {
      font-size: 0.8rem;
      color: var(--p-text-muted-color);
    }

    dd {
      margin: 0;
      font-weight: 600;
    }

    .acciones {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .tabla {
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
    }

    .control {
      display: block;
      color: #b45309;
    }

    .alertas {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.6rem;
    }

    .block {
      display: block;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecetaDetalle implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(RecetasApiService);
  private readonly descargas = inject(DescargaService);
  private readonly notifications = inject(NotificationService);

  protected readonly medico = inject(AuthService).isMedico();
  protected readonly etiqueta = ETIQUETA_ESTADO_RECETA;
  protected readonly severidad = SEVERIDAD_ESTADO_RECETA;
  protected readonly severidadAlerta = ETIQUETA_SEVERIDAD_ALERTA;
  protected readonly colorAlerta = SEVERIDAD_ALERTA;
  protected readonly tipoAlerta = ETIQUETA_TIPO_ALERTA;
  protected readonly receta = signal<RecetaElectronica | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly descargando = signal(false);
  protected readonly anulando = signal(false);
  protected readonly procesando = signal(false);
  protected readonly presente = (texto: string | null): texto is string => !!texto;

  ngOnInit(): void {
    this.cargar();
  }

  protected vigencia(): string {
    const r = this.receta();
    return r ? (r.estado === 'ANULADA' ? 'Anulada' : textoVigencia(r.vigente, r.vigenteHasta)) : '';
  }

  private cargar(): void {
    this.api.obtener(this.id()).subscribe({
      next: (r) => this.receta.set(r),
      error: () => this.error.set('No se pudo cargar la receta'),
    });
  }

  protected descargar(r: RecetaElectronica): void {
    this.descargando.set(true);
    this.api.pdf(r.id).subscribe({
      next: (archivo) => {
        this.descargando.set(false);
        this.descargas.guardar(archivo);
      },
      error: () => this.descargando.set(false),
    });
  }

  protected anular(motivo: string): void {
    this.procesando.set(true);
    this.api.anular(this.id(), motivo).subscribe({
      next: (r) => {
        this.procesando.set(false);
        this.anulando.set(false);
        this.receta.set(r);
        this.notifications.success(`La receta ${r.numero} fue anulada.`);
      },
      error: (error: ApiError) => {
        this.procesando.set(false);
        this.notifications.error(error.message ?? 'No se pudo anular la receta');
      },
    });
  }
}
