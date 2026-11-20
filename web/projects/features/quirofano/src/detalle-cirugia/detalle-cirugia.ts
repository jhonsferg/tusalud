import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';
import { Observable } from 'rxjs';

import {
  AuthService,
  CirugiaDetalle,
  EsterilizacionApiService,
  InsumoDisponible,
  LoteEsteril,
  MiembroEquipo,
  MomentoChecklist,
  NotificationService,
  Quirofano,
  QuirofanoApiService,
  RolQuirurgico,
} from '@tusalud/core';
import { DialogoMotivo } from '@tusalud/ui/dialogo-motivo';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ANESTESIA,
  ETIQUETA_CONSENTIMIENTO,
  ETIQUETA_ESTADO_CIRUGIA,
  ETIQUETA_ITEM,
  ETIQUETA_MOMENTO,
  ETIQUETA_ROL,
  ETIQUETA_TIPO_CIRUGIA,
  ITEMS_CHECKLIST,
  MOMENTOS,
  SEVERIDAD_ESTADO_CIRUGIA,
  aIsoLocal,
  esRolMedico,
  faltantesChecklist,
  fechaHora,
  momentoDisponible,
  puedeCancelarse,
  puedeProgramarse,
  puedeSuspenderse,
} from '../quirofano.utils';

interface FilaMiembro {
  rol: RolQuirurgico;
  usuarioId: string | null;
}

@Component({
  selector: 'app-detalle-cirugia',
  imports: [
    FormsModule,
    Button,
    DatePicker,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    DialogoMotivo,
    PageHeader,
  ],
  template: `
    @if (detalle(); as d) {
      <app-page-header
        [title]="d.resumen.codigo"
        [subtitle]="d.resumen.procedimiento ?? 'Cirugía reservada'"
        icon="pi-calendar-plus"
        backLink="/quirofano"
      />
      @if (error()) {
        <p-message severity="error">{{ error() }}</p-message>
      }
      <section class="panel resumen" aria-labelledby="res-titulo">
        <h2 id="res-titulo" class="sr-only">Resumen de la cirugía</h2>
        <dl>
          <div>
            <dt>Estado</dt>
            <dd>
              <p-tag
                [value]="estados[d.resumen.estado]"
                [severity]="severidades[d.resumen.estado]"
              />
            </dd>
          </div>
          <div>
            <dt>Tipo</dt>
            <dd>{{ tipos[d.resumen.tipo] }}</dd>
          </div>
          <div>
            <dt>Paciente</dt>
            <dd>{{ d.resumen.pacienteNombre ?? 'Reservado' }}</dd>
          </div>
          <div>
            <dt>Quirófano</dt>
            <dd>{{ d.resumen.quirofanoNombre ?? 'Sin asignar' }}</dd>
          </div>
          <div>
            <dt>Programada</dt>
            <dd>{{ fechaHora(d.resumen.inicio) }} a {{ fechaHora(d.resumen.fin) }}</dd>
          </div>
          <div>
            <dt>Anestesia</dt>
            <dd>{{ anestesias[d.tipoAnestesia] }}</dd>
          </div>
          @if (d.clinicoVisible) {
            <div>
              <dt>Diagnóstico</dt>
              <dd>{{ d.diagnosticoCodigo }} {{ d.diagnosticoDescripcion }}</dd>
            </div>
            @if (d.indicacionesPreoperatorias) {
              <div>
                <dt>Indicaciones preoperatorias</dt>
                <dd>{{ d.indicacionesPreoperatorias }}</dd>
              </div>
            }
          }
          @if (d.ingresoCodigo) {
            <div>
              <dt>Ingreso hospitalario</dt>
              <dd>{{ d.ingresoCodigo }}</dd>
            </div>
          }
          @if (d.motivo) {
            <div>
              <dt>Motivo</dt>
              <dd>{{ d.motivo }}</dd>
            </div>
          }
        </dl>
        <div class="acciones">
          @if (puedeGestionar() && d.resumen.estado === 'PROGRAMADA') {
            <p-button
              label="Pasar a preparación"
              icon="pi pi-play"
              [loading]="trabajando()"
              (onClick)="ejecutar(api.preparar(d.resumen.id), 'La cirugía está en preparación')"
            />
          }
          @if (soyCirujano() && d.resumen.estado === 'EN_PREPARACION') {
            <p-button
              label="Iniciar cirugía"
              icon="pi pi-bolt"
              [loading]="trabajando()"
              (onClick)="ejecutar(api.iniciar(d.resumen.id), 'La cirugía está en curso')"
            />
          }
          @if (soyCirujano() && d.resumen.estado === 'EN_CURSO') {
            <p-button
              label="Finalizar cirugía"
              icon="pi pi-check"
              [loading]="trabajando()"
              (onClick)="ejecutar(api.finalizar(d.resumen.id), 'La cirugía quedó finalizada')"
            />
          }
          @if (puedeGestionar() && puedeSuspender(d.resumen.estado)) {
            <p-button
              label="Suspender"
              icon="pi pi-pause"
              severity="warn"
              [outlined]="true"
              (onClick)="motivoAccion.set('suspender')"
            />
          }
          @if (puedeGestionar() && puedeCancelar(d.resumen.estado)) {
            <p-button
              label="Cancelar cirugía"
              icon="pi pi-times"
              severity="danger"
              [outlined]="true"
              (onClick)="motivoAccion.set('cancelar')"
            />
          }
        </div>
        @if (d.resumen.estado === 'EN_PREPARACION' && d.pendientesParaIniciar.length > 0) {
          <p-message severity="warn">
            Para iniciar falta: {{ d.pendientesParaIniciar.join('; ') }}.
          </p-message>
        }
        @if (d.resumen.estado === 'EN_CURSO' && d.pendientesParaFinalizar.length > 0) {
          <p-message severity="info">
            Para finalizar falta: {{ d.pendientesParaFinalizar.join('; ') }}.
          </p-message>
        }
      </section>

      @if (puedeGestionar() && puedeProgramar(d.resumen.estado)) {
        <section class="panel" aria-labelledby="prog-titulo">
          <h2 id="prog-titulo">Programación y equipo</h2>
          <form class="formulario" (submit)="$event.preventDefault(); programar(d)">
            <div class="campos">
              <label for="prog-quirofano">
                Quirófano
                <p-select
                  inputId="prog-quirofano"
                  name="quirofano"
                  [options]="quirofanos()"
                  optionLabel="nombre"
                  optionValue="id"
                  [(ngModel)]="quirofanoId"
                  placeholder="Elija un quirófano"
                />
              </label>
              <label for="prog-inicio">
                Inicio
                <p-datepicker
                  inputId="prog-inicio"
                  name="inicio"
                  [(ngModel)]="inicio"
                  [showTime]="true"
                  [stepMinute]="15"
                  dateFormat="dd/mm/yy"
                  hourFormat="24"
                  appendTo="body"
                />
              </label>
              <label for="prog-duracion">
                Duración en minutos
                <p-inputnumber
                  inputId="prog-duracion"
                  name="duracion"
                  [(ngModel)]="duracionMin"
                  [min]="15"
                  [max]="1440"
                  [showButtons]="true"
                  [step]="15"
                />
              </label>
            </div>
            <fieldset>
              <legend>Equipo quirúrgico</legend>
              @for (fila of equipoFilas(); track $index) {
                <div class="fila-equipo">
                  <label>
                    Rol
                    <p-select
                      [options]="roles"
                      optionLabel="etiqueta"
                      optionValue="valor"
                      [ngModel]="fila.rol"
                      [ngModelOptions]="{ standalone: true }"
                      (ngModelChange)="cambiarRol($index, $event)"
                      [attr.aria-label]="'Rol del miembro ' + ($index + 1)"
                    />
                  </label>
                  <label>
                    Persona
                    <p-select
                      [options]="personalDe(fila.rol)"
                      optionLabel="nombre"
                      optionValue="usuarioId"
                      [ngModel]="fila.usuarioId"
                      [ngModelOptions]="{ standalone: true }"
                      (ngModelChange)="cambiarPersona($index, $event)"
                      placeholder="Elija a la persona"
                      [attr.aria-label]="'Persona del miembro ' + ($index + 1)"
                    />
                  </label>
                  <p-button
                    icon="pi pi-trash"
                    severity="danger"
                    [text]="true"
                    ariaLabel="Quitar del equipo"
                    (onClick)="quitarMiembro($index)"
                  />
                </div>
              }
              <p-button
                label="Agregar miembro"
                icon="pi pi-plus"
                [text]="true"
                (onClick)="agregarMiembro()"
              />
            </fieldset>
            <div class="barra">
              <p-button
                type="submit"
                [label]="d.resumen.estado === 'PROGRAMADA' ? 'Reprogramar' : 'Programar'"
                icon="pi pi-calendar-plus"
                [loading]="trabajando()"
                [disabled]="!programacionValida()"
              />
            </div>
          </form>
        </section>
      }

      <section class="panel" aria-labelledby="eq-titulo">
        <h2 id="eq-titulo">Equipo asignado</h2>
        @if (d.equipo.length === 0) {
          <p class="text-muted">Todavía no hay equipo asignado.</p>
        } @else {
          <ul class="lista">
            @for (m of d.equipo; track m.usuarioId + m.rol) {
              <li>
                <strong>{{ rolEtiqueta[m.rol] }}</strong
                >: {{ m.nombre }}
              </li>
            }
          </ul>
        }
      </section>

      <section class="panel" aria-labelledby="cons-titulo">
        <h2 id="cons-titulo">Consentimientos</h2>
        <ul class="lista">
          <li>
            Quirúrgico:
            @if (d.consentimientoQuirurgico; as c) {
              <p-tag
                [value]="consentimientos[c.estado]"
                [severity]="c.estado === 'FIRMADO' ? 'success' : 'warn'"
              />
            } @else {
              <span class="text-muted">sin solicitar</span>
            }
          </li>
          @if (d.tipoAnestesia !== 'NINGUNA') {
            <li>
              Anestésico:
              @if (d.consentimientoAnestesico; as c) {
                <p-tag
                  [value]="consentimientos[c.estado]"
                  [severity]="c.estado === 'FIRMADO' ? 'success' : 'warn'"
                />
              } @else {
                <span class="text-muted">sin solicitar</span>
              }
            </li>
          }
        </ul>
        @if (auth.isMedico() && abierta(d)) {
          <p-button
            label="Solicitar consentimientos al paciente"
            icon="pi pi-file-edit"
            [outlined]="true"
            [loading]="trabajando()"
            (onClick)="
              ejecutar(
                api.solicitarConsentimientos(d.resumen.id),
                'Se solicitó la firma de los consentimientos'
              )
            "
          />
        }
      </section>

      @if (d.equipo.length > 0) {
        <section class="panel" aria-labelledby="chk-titulo">
          <h2 id="chk-titulo">Lista de verificación de cirugía segura</h2>
          @for (m of momentos; track m) {
            <fieldset class="momento">
              <legend>{{ etiquetaMomento[m] }}</legend>
              @if (registroDe(d, m); as reg) {
                <p class="hecho">
                  <i class="pi pi-check-circle" aria-hidden="true"></i>
                  Completada por {{ reg.completadoPor }} el {{ fechaHora(reg.completadoEn) }}.
                </p>
              } @else if (disponible(d, m)) {
                @for (item of itemsDe(m); track item) {
                  <label class="check">
                    <input
                      type="checkbox"
                      [checked]="marcado(m, item)"
                      (change)="alternar(m, item)"
                    />
                    {{ etiquetaItem[item] }}
                  </label>
                }
                <p-button
                  label="Registrar lista"
                  icon="pi pi-check-square"
                  [loading]="trabajando()"
                  [disabled]="!listaCompleta(m)"
                  (onClick)="registrarLista(d, m)"
                />
              } @else {
                <p class="text-muted">Aún no corresponde completar esta lista.</p>
              }
            </fieldset>
          }
        </section>
      }

      @if (
        d.equipo.length > 0 &&
        (d.resumen.estado === 'EN_PREPARACION' || d.resumen.estado === 'EN_CURSO')
      ) {
        <section class="panel" aria-labelledby="mat-titulo">
          <h2 id="mat-titulo">Material e instrumental</h2>
          @if (d.resumen.enMiEquipo) {
            <form class="fila-material" (submit)="$event.preventDefault(); agregarInsumo(d)">
              <label for="ins-producto">
                Insumo
                <p-select
                  inputId="ins-producto"
                  name="producto"
                  [options]="insumos()"
                  optionLabel="nombre"
                  optionValue="id"
                  [(ngModel)]="insumoId"
                  [filter]="true"
                  placeholder="Elija un insumo"
                />
              </label>
              <label for="ins-cantidad">
                Cantidad
                <p-inputnumber
                  inputId="ins-cantidad"
                  name="cantidad"
                  [(ngModel)]="insumoCantidad"
                  [min]="1"
                  [max]="1000"
                />
              </label>
              <p-button
                type="submit"
                label="Descontar del inventario"
                icon="pi pi-minus-circle"
                [loading]="trabajando()"
                [disabled]="!insumoId"
              />
            </form>
            <form class="fila-material" (submit)="$event.preventDefault(); agregarSet(d)">
              <label for="set-lote">
                Set esterilizado
                <p-select
                  inputId="set-lote"
                  name="lote"
                  [options]="lotes()"
                  optionLabel="etiqueta"
                  optionValue="id"
                  [(ngModel)]="loteId"
                  placeholder="Elija un lote vigente"
                />
              </label>
              <p-button
                type="submit"
                label="Registrar uso del set"
                icon="pi pi-box"
                [loading]="trabajando()"
                [disabled]="!loteId"
              />
            </form>
          }
          @if (d.insumos.length > 0) {
            <table>
              <caption class="sr-only">
                Insumos descontados
              </caption>
              <thead>
                <tr>
                  <th scope="col">Insumo</th>
                  <th scope="col">Cantidad</th>
                  <th scope="col">Registró</th>
                  <th scope="col">Fecha</th>
                </tr>
              </thead>
              <tbody>
                @for (i of d.insumos; track i.id) {
                  <tr>
                    <th scope="row">{{ i.producto }}</th>
                    <td>{{ i.cantidad }} {{ i.unidad }}</td>
                    <td>{{ i.registradoPor }}</td>
                    <td>{{ fechaHora(i.registradoEn) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </section>
      }

      @if (d.sets.length > 0) {
        <section class="panel" aria-labelledby="sets-titulo">
          <h2 id="sets-titulo">Sets usados en esta cirugía</h2>
          <table>
            <caption class="sr-only">
              Sets de instrumental usados
            </caption>
            <thead>
              <tr>
                <th scope="col">Lote</th>
                <th scope="col">Set</th>
                <th scope="col">Ciclo</th>
                <th scope="col">Esterilizado</th>
                <th scope="col">Vence</th>
              </tr>
            </thead>
            <tbody>
              @for (s of d.sets; track s.id) {
                <tr>
                  <th scope="row">{{ s.loteCodigo }}</th>
                  <td>{{ s.set }}</td>
                  <td>{{ s.cicloNumero }}</td>
                  <td>{{ fechaHora(s.esterilizadoEn) }}</td>
                  <td>{{ fechaHora(s.venceEn) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </section>
      }

      @if (d.clinicoVisible) {
        <section class="panel" aria-labelledby="nota-titulo">
          <h2 id="nota-titulo">Nota operatoria</h2>
          @if (d.notaOperatoria; as n) {
            <dl>
              <div>
                <dt>Hallazgos</dt>
                <dd>{{ n.hallazgos }}</dd>
              </div>
              <div>
                <dt>Técnica</dt>
                <dd>{{ n.tecnica }}</dd>
              </div>
              @if (n.complicaciones) {
                <div>
                  <dt>Complicaciones</dt>
                  <dd>{{ n.complicaciones }}</dd>
                </div>
              }
              <div>
                <dt>Sangrado</dt>
                <dd>{{ n.sangradoMl ?? '—' }} ml</dd>
              </div>
              <div>
                <dt>Diagnóstico postoperatorio</dt>
                <dd>{{ n.diagnosticoPostoperatorio }}</dd>
              </div>
              @if (n.indicacionesPostoperatorias) {
                <div>
                  <dt>Indicaciones postoperatorias</dt>
                  <dd>{{ n.indicacionesPostoperatorias }}</dd>
                </div>
              }
              <div>
                <dt>Registrada</dt>
                <dd>{{ fechaHora(n.registradaEn) }} por {{ n.registradaPor }}</dd>
              </div>
            </dl>
          }
          @if (soyCirujano() && d.resumen.estado === 'EN_CURSO') {
            <form class="formulario" (submit)="$event.preventDefault(); guardarNota(d)">
              <label>
                Hallazgos
                <input pInputText name="hallazgos" maxlength="4000" [(ngModel)]="hallazgos" />
              </label>
              <label>
                Técnica
                <input pInputText name="tecnica" maxlength="4000" [(ngModel)]="tecnica" />
              </label>
              <label>
                Complicaciones
                <input
                  pInputText
                  name="complicaciones"
                  maxlength="2000"
                  [(ngModel)]="complicaciones"
                />
              </label>
              <label for="nota-sangrado">
                Sangrado en ml
                <p-inputnumber
                  inputId="nota-sangrado"
                  name="sangrado"
                  [(ngModel)]="sangradoMl"
                  [min]="0"
                  [max]="20000"
                />
              </label>
              <label>
                Diagnóstico postoperatorio
                <input pInputText name="dxpost" maxlength="300" [(ngModel)]="diagnosticoPost" />
              </label>
              <label>
                Indicaciones postoperatorias
                <input pInputText name="indpost" maxlength="500" [(ngModel)]="indicacionesPost" />
              </label>
              <div class="barra">
                <p-button
                  type="submit"
                  [label]="d.notaOperatoria ? 'Actualizar nota' : 'Guardar nota operatoria'"
                  icon="pi pi-save"
                  [loading]="trabajando()"
                  [disabled]="!notaValida()"
                />
              </div>
            </form>
          } @else if (!d.notaOperatoria) {
            <p class="text-muted">Aún no se registró la nota operatoria.</p>
          }
        </section>

        <section class="panel" aria-labelledby="anes-titulo">
          <h2 id="anes-titulo">Registro anestésico</h2>
          @if (d.registroAnestesico; as r) {
            <dl>
              <div>
                <dt>Clasificación ASA</dt>
                <dd>{{ r.asa }}</dd>
              </div>
              <div>
                <dt>Anestesia</dt>
                <dd>{{ fechaHora(r.inicio) }} a {{ fechaHora(r.fin) }}</dd>
              </div>
              @if (r.observaciones) {
                <div>
                  <dt>Observaciones</dt>
                  <dd>{{ r.observaciones }}</dd>
                </div>
              }
              <div>
                <dt>Registrado</dt>
                <dd>{{ fechaHora(r.registradoEn) }} por {{ r.registradoPor }}</dd>
              </div>
            </dl>
          }
          @if (soyAnestesiologo() && d.resumen.estado === 'EN_CURSO') {
            <form class="formulario" (submit)="$event.preventDefault(); guardarAnestesia(d)">
              <div class="campos">
                <label for="anes-asa">
                  Clasificación ASA
                  <p-select inputId="anes-asa" name="asa" [options]="clasesAsa" [(ngModel)]="asa" />
                </label>
                <label for="anes-inicio">
                  Inicio
                  <p-datepicker
                    inputId="anes-inicio"
                    name="anesInicio"
                    [(ngModel)]="anesInicio"
                    [showTime]="true"
                    dateFormat="dd/mm/yy"
                    hourFormat="24"
                    appendTo="body"
                  />
                </label>
                <label for="anes-fin">
                  Fin
                  <p-datepicker
                    inputId="anes-fin"
                    name="anesFin"
                    [(ngModel)]="anesFin"
                    [showTime]="true"
                    dateFormat="dd/mm/yy"
                    hourFormat="24"
                    appendTo="body"
                  />
                </label>
              </div>
              <label>
                Observaciones
                <input pInputText name="anesObs" maxlength="4000" [(ngModel)]="anesObservaciones" />
              </label>
              <div class="barra">
                <p-button
                  type="submit"
                  label="Guardar registro anestésico"
                  icon="pi pi-save"
                  [loading]="trabajando()"
                  [disabled]="!anestesiaValida()"
                />
              </div>
            </form>
          } @else if (!d.registroAnestesico) {
            <p class="text-muted">Aún no se registró la anestesia.</p>
          }
        </section>
      }

      <section class="panel" aria-labelledby="hist-titulo">
        <h2 id="hist-titulo">Historial</h2>
        <ol class="lista">
          @for (e of d.eventos; track $index) {
            <li>
              {{ fechaHora(e.fecha) }}: {{ e.accion }} por {{ e.usuario }}
              @if (e.detalle) {
                <span class="text-muted"> · {{ e.detalle }}</span>
              }
            </li>
          }
        </ol>
      </section>

      @if (motivoAccion(); as accion) {
        <app-dialogo-motivo
          [titulo]="accion === 'suspender' ? 'Suspender la cirugía' : 'Cancelar la cirugía'"
          descripcion="El motivo queda registrado en el historial."
          [confirmarLabel]="accion === 'suspender' ? 'Suspender' : 'Cancelar cirugía'"
          severidad="danger"
          [cargando]="trabajando()"
          (confirmar)="confirmarMotivo(d, accion, $event)"
          (cancelar)="motivoAccion.set(null)"
        />
      }
    } @else if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0 0 0.75rem;
      font-size: 1.05rem;
    }

    dl {
      display: grid;
      gap: 0.75rem 1.25rem;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
      margin: 0 0 0.75rem;
    }

    dt {
      font-size: 0.78rem;
      font-weight: 600;
      color: var(--p-text-muted-color, #64748b);
    }

    dd {
      margin: 0;
    }

    .acciones,
    .barra {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-top: 0.5rem;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
      font-size: 0.85rem;
    }

    fieldset {
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-radius: 0.5rem;
      padding: 0.75rem;
      margin: 0 0 0.75rem;
      display: grid;
      gap: 0.5rem;
    }

    legend {
      font-weight: 700;
      padding: 0 0.4rem;
    }

    .fila-equipo,
    .fila-material {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      align-items: end;
      margin-bottom: 0.5rem;
    }

    .check {
      display: flex;
      gap: 0.5rem;
      align-items: center;
      font-weight: 400;
    }

    .hecho {
      margin: 0;
      color: var(--p-green-700, #15803d);
    }

    .lista {
      margin: 0 0 0.5rem;
      padding-left: 1.1rem;
      display: grid;
      gap: 0.3rem;
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
export class DetalleCirugia implements OnInit {
  readonly id = input.required<string>();

  protected readonly api = inject(QuirofanoApiService);
  private readonly esterilizacion = inject(EsterilizacionApiService);
  private readonly avisos = inject(NotificationService);
  protected readonly auth = inject(AuthService);

  protected readonly detalle = signal<CirugiaDetalle | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly trabajando = signal(false);
  protected readonly motivoAccion = signal<'suspender' | 'cancelar' | null>(null);
  protected readonly quirofanos = signal<Quirofano[]>([]);
  protected readonly equipoFilas = signal<FilaMiembro[]>([]);
  protected readonly personalMedico = signal<MiembroEquipo[]>([]);
  protected readonly personalEnfermeria = signal<MiembroEquipo[]>([]);
  protected readonly insumos = signal<InsumoDisponible[]>([]);
  protected readonly lotes = signal<(LoteEsteril & { etiqueta: string })[]>([]);
  protected readonly marcas = signal<Record<MomentoChecklist, ReadonlySet<string>>>({
    ANTES_INDUCCION: new Set(),
    ANTES_INCISION: new Set(),
    ANTES_SALIDA: new Set(),
  });

  protected readonly estados = ETIQUETA_ESTADO_CIRUGIA;
  protected readonly severidades = SEVERIDAD_ESTADO_CIRUGIA;
  protected readonly tipos = ETIQUETA_TIPO_CIRUGIA;
  protected readonly anestesias = ETIQUETA_ANESTESIA;
  protected readonly consentimientos = ETIQUETA_CONSENTIMIENTO;
  protected readonly rolEtiqueta = ETIQUETA_ROL;
  protected readonly etiquetaMomento = ETIQUETA_MOMENTO;
  protected readonly etiquetaItem = ETIQUETA_ITEM;
  protected readonly momentos = MOMENTOS;
  protected readonly fechaHora = fechaHora;
  protected readonly puedeProgramar = puedeProgramarse;
  protected readonly puedeCancelar = puedeCancelarse;
  protected readonly puedeSuspender = puedeSuspenderse;
  protected readonly clasesAsa = [1, 2, 3, 4, 5, 6];
  protected readonly roles: { valor: RolQuirurgico; etiqueta: string }[] = (
    Object.keys(ETIQUETA_ROL) as RolQuirurgico[]
  ).map((valor) => ({ valor, etiqueta: ETIQUETA_ROL[valor] }));

  protected quirofanoId: string | null = null;
  protected inicio: Date | null = null;
  protected duracionMin: number | null = 120;
  protected insumoId: string | null = null;
  protected insumoCantidad: number | null = 1;
  protected loteId: string | null = null;
  protected hallazgos = '';
  protected tecnica = '';
  protected complicaciones = '';
  protected sangradoMl: number | null = null;
  protected diagnosticoPost = '';
  protected indicacionesPost = '';
  protected asa: number | null = 2;
  protected anesInicio: Date | null = null;
  protected anesFin: Date | null = null;
  protected anesObservaciones = '';

  protected readonly miId = computed(() => this.auth.user()?.id ?? null);
  protected readonly misRoles = computed(() =>
    (this.detalle()?.equipo ?? []).filter((m) => m.usuarioId === this.miId()).map((m) => m.rol),
  );
  protected readonly soyCirujano = computed(() => this.misRoles().includes('CIRUJANO'));
  protected readonly soyAnestesiologo = computed(() => this.misRoles().includes('ANESTESIOLOGO'));
  protected readonly puedeGestionar = computed(
    () => this.auth.isEnfermeria() || this.auth.isMedico(),
  );

  ngOnInit(): void {
    this.cargar();
    this.api.quirofanos().subscribe({ next: (q) => this.quirofanos.set(q) });
  }

  protected abierta(d: CirugiaDetalle): boolean {
    return d.resumen.estado !== 'FINALIZADA' && d.resumen.estado !== 'CANCELADA';
  }

  protected personalDe(rol: RolQuirurgico): MiembroEquipo[] {
    return esRolMedico(rol) ? this.personalMedico() : this.personalEnfermeria();
  }

  protected agregarMiembro(): void {
    this.cargarPersonal();
    this.equipoFilas.update((f) => [...f, { rol: 'AYUDANTE', usuarioId: null }]);
  }

  protected quitarMiembro(indice: number): void {
    this.equipoFilas.update((f) => f.filter((_, i) => i !== indice));
  }

  protected cambiarRol(indice: number, rol: RolQuirurgico): void {
    this.equipoFilas.update((f) =>
      f.map((fila, i) => (i === indice ? { rol, usuarioId: null } : fila)),
    );
  }

  protected cambiarPersona(indice: number, usuarioId: string | null): void {
    this.equipoFilas.update((f) =>
      f.map((fila, i) => (i === indice ? { ...fila, usuarioId } : fila)),
    );
  }

  protected programacionValida(): boolean {
    const filas = this.equipoFilas();
    return (
      !!this.quirofanoId &&
      !!this.inicio &&
      !!this.duracionMin &&
      filas.length > 0 &&
      filas.every((f) => !!f.usuarioId) &&
      filas.filter((f) => f.rol === 'CIRUJANO').length === 1
    );
  }

  protected programar(d: CirugiaDetalle): void {
    if (!this.programacionValida() || !this.inicio || !this.quirofanoId) {
      return;
    }
    this.ejecutar(
      this.api.programar(d.resumen.id, {
        quirofanoId: this.quirofanoId,
        inicio: aIsoLocal(this.inicio),
        duracionMin: this.duracionMin ?? 60,
        equipo: this.equipoFilas().map((f) => ({ usuarioId: f.usuarioId ?? '', rol: f.rol })),
      }),
      'La cirugía quedó programada',
    );
  }

  protected itemsDe(momento: MomentoChecklist): readonly string[] {
    return ITEMS_CHECKLIST[momento];
  }

  protected marcado(momento: MomentoChecklist, item: string): boolean {
    return this.marcas()[momento].has(item);
  }

  protected alternar(momento: MomentoChecklist, item: string): void {
    this.marcas.update((actual) => {
      const nuevo = new Set(actual[momento]);
      if (nuevo.has(item)) {
        nuevo.delete(item);
      } else {
        nuevo.add(item);
      }
      return { ...actual, [momento]: nuevo };
    });
  }

  protected listaCompleta(momento: MomentoChecklist): boolean {
    return faltantesChecklist(momento, this.marcas()[momento]).length === 0;
  }

  protected registroDe(d: CirugiaDetalle, momento: MomentoChecklist) {
    return d.checklists.find((c) => c.momento === momento) ?? null;
  }

  protected disponible(d: CirugiaDetalle, momento: MomentoChecklist): boolean {
    return (
      d.resumen.enMiEquipo &&
      momentoDisponible(momento, d.resumen.estado, new Set(d.checklists.map((c) => c.momento)))
    );
  }

  protected registrarLista(d: CirugiaDetalle, momento: MomentoChecklist): void {
    this.ejecutar(
      this.api.checklist(d.resumen.id, { momento, items: [...this.marcas()[momento]] }),
      'La lista de verificación quedó registrada',
    );
  }

  protected agregarInsumo(d: CirugiaDetalle): void {
    if (!this.insumoId) {
      return;
    }
    this.ejecutar(
      this.api.registrarInsumo(d.resumen.id, this.insumoId, this.insumoCantidad ?? 1),
      'El insumo se descontó del inventario',
    );
  }

  protected agregarSet(d: CirugiaDetalle): void {
    if (!this.loteId) {
      return;
    }
    this.ejecutar(this.api.usarSet(d.resumen.id, this.loteId), 'El uso del set quedó registrado');
  }

  protected notaValida(): boolean {
    return (
      this.hallazgos.trim().length > 0 &&
      this.tecnica.trim().length > 0 &&
      this.diagnosticoPost.trim().length > 0
    );
  }

  protected guardarNota(d: CirugiaDetalle): void {
    if (!this.notaValida()) {
      return;
    }
    this.ejecutar(
      this.api.notaOperatoria(d.resumen.id, {
        hallazgos: this.hallazgos.trim(),
        tecnica: this.tecnica.trim(),
        complicaciones: this.complicaciones.trim() || null,
        sangradoMl: this.sangradoMl,
        diagnosticoPostoperatorio: this.diagnosticoPost.trim(),
        indicacionesPostoperatorias: this.indicacionesPost.trim() || null,
      }),
      'La nota operatoria quedó registrada',
    );
  }

  protected anestesiaValida(): boolean {
    return !!this.asa && !!this.anesInicio && !!this.anesFin && this.anesFin > this.anesInicio;
  }

  protected guardarAnestesia(d: CirugiaDetalle): void {
    if (!this.anestesiaValida() || !this.anesInicio || !this.anesFin || !this.asa) {
      return;
    }
    this.ejecutar(
      this.api.registroAnestesico(d.resumen.id, {
        asa: this.asa,
        inicio: aIsoLocal(this.anesInicio),
        fin: aIsoLocal(this.anesFin),
        observaciones: this.anesObservaciones.trim() || null,
      }),
      'El registro anestésico quedó guardado',
    );
  }

  protected confirmarMotivo(
    d: CirugiaDetalle,
    accion: 'suspender' | 'cancelar',
    motivo: string,
  ): void {
    const llamada =
      accion === 'suspender'
        ? this.api.suspender(d.resumen.id, motivo)
        : this.api.cancelar(d.resumen.id, motivo);
    this.ejecutar(
      llamada,
      accion === 'suspender' ? 'La cirugía quedó suspendida' : 'La cirugía quedó cancelada',
    );
    this.motivoAccion.set(null);
  }

  protected ejecutar(llamada: Observable<CirugiaDetalle>, mensaje: string): void {
    this.trabajando.set(true);
    this.error.set(null);
    llamada.subscribe({
      next: (d) => {
        this.aplicar(d);
        this.trabajando.set(false);
        this.avisos.success(mensaje);
      },
      error: (e: { error?: { message?: string } }) => {
        this.trabajando.set(false);
        this.error.set(e.error?.message ?? 'No pudimos completar la operación');
      },
    });
  }

  private cargar(): void {
    this.api.detalle(this.id()).subscribe({
      next: (d) => this.aplicar(d),
      error: (e: { error?: { message?: string } }) =>
        this.error.set(e.error?.message ?? 'No pudimos cargar la cirugía'),
    });
  }

  private aplicar(d: CirugiaDetalle): void {
    this.detalle.set(d);
    this.equipoFilas.set(d.equipo.map((m) => ({ rol: m.rol, usuarioId: m.usuarioId })));
    if (d.equipo.length > 0 && this.personalMedico().length === 0) {
      this.cargarPersonal();
    }
    this.quirofanoId = this.quirofanoId ?? d.resumen.quirofanoId ?? null;
    if (
      d.resumen.enMiEquipo &&
      (d.resumen.estado === 'EN_PREPARACION' || d.resumen.estado === 'EN_CURSO')
    ) {
      this.cargarMaterial();
    }
  }

  private cargarPersonal(): void {
    if (this.personalMedico().length === 0) {
      this.api.personal('CIRUJANO').subscribe({ next: (p) => this.personalMedico.set(p) });
    }
    if (this.personalEnfermeria().length === 0) {
      this.api
        .personal('INSTRUMENTISTA')
        .subscribe({ next: (p) => this.personalEnfermeria.set(p) });
    }
  }

  private cargarMaterial(): void {
    if (this.insumos().length === 0) {
      this.api.insumos().subscribe({ next: (i) => this.insumos.set(i) });
    }
    this.esterilizacion.lotes(true).subscribe({
      next: (lista) =>
        this.lotes.set(
          lista.map((l) => ({
            ...l,
            etiqueta: `${l.codigo} · ${l.setNombre} · vence ${fechaHora(l.venceEn)}`,
          })),
        ),
    });
  }
}
