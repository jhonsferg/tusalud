import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Tag } from 'primeng/tag';

import {
  AlertaEsterilizacion,
  AuthService,
  CicloEsterilizacion,
  EstadoEfectivoLote,
  Esterilizador,
  EsterilizacionApiService,
  LoteEsteril,
  NotificationService,
  ResultadoCiclo,
  ResultadoIndicador,
  SetInstrumental,
  TrazabilidadLote,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';

type Vista = 'ciclos' | 'lotes' | 'alertas';
type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

const ETIQUETA_LOTE: Readonly<Record<EstadoEfectivoLote, string>> = {
  ESTERIL: 'Estéril',
  EN_ESPERA_DE_INDICADOR: 'Esperando indicador',
  EN_USO: 'En uso',
  USADO: 'Usado',
  BLOQUEADO: 'Bloqueado',
  VENCIDO: 'Vencido',
};

const SEVERIDAD_LOTE: Readonly<Record<EstadoEfectivoLote, Severidad>> = {
  ESTERIL: 'success',
  EN_ESPERA_DE_INDICADOR: 'info',
  EN_USO: 'warn',
  USADO: 'secondary',
  BLOQUEADO: 'danger',
  VENCIDO: 'warn',
};

const ETIQUETA_CICLO: Readonly<Record<ResultadoCiclo, string>> = {
  EN_PROCESO: 'En proceso',
  CONFORME: 'Conforme',
  NO_CONFORME: 'No conforme',
};

const SEVERIDAD_CICLO: Readonly<Record<ResultadoCiclo, Severidad>> = {
  EN_PROCESO: 'info',
  CONFORME: 'success',
  NO_CONFORME: 'danger',
};

const ETIQUETA_INDICADOR: Readonly<Record<ResultadoIndicador, string>> = {
  PENDIENTE: 'Pendiente',
  CONFORME: 'Conforme',
  NO_CONFORME: 'No conforme',
};

const SEVERIDAD_ALERTA: Readonly<Record<AlertaEsterilizacion['severidad'], Severidad>> = {
  CRITICA: 'danger',
  ALTA: 'danger',
  MEDIA: 'warn',
  BAJA: 'info',
};

function aIsoLocal(fecha: Date): string {
  const dos = (n: number) => String(n).padStart(2, '0');
  return `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(fecha.getHours())}:${dos(fecha.getMinutes())}:00`;
}

function fechaHora(valor: string | null | undefined): string {
  return valor
    ? new Intl.DateTimeFormat('es-PE', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(valor))
    : '—';
}

@Component({
  selector: 'app-esterilizacion-panel',
  imports: [
    FormsModule,
    Button,
    DatePicker,
    InputNumber,
    InputText,
    Message,
    Select,
    SelectButton,
    Tag,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Esterilización"
      subtitle="Ciclos de autoclave, lotes de instrumental y su trazabilidad"
      icon="pi-shield"
    />
    @if (error()) {
      <p-message severity="error">{{ error() }}</p-message>
    }
    <div class="panel barra">
      <p-selectbutton
        [options]="vistas"
        optionLabel="etiqueta"
        optionValue="valor"
        [ngModel]="vista()"
        (ngModelChange)="vista.set($event ?? 'ciclos')"
        [allowEmpty]="false"
        ariaLabel="Vista de esterilización"
      />
      @if (alertas().length > 0) {
        <p-tag [value]="alertas().length + ' alertas'" severity="danger" />
      }
    </div>

    @if (vista() === 'ciclos') {
      @if (esEnfermeria()) {
        <form class="panel formulario" (submit)="$event.preventDefault(); crearCiclo()">
          <h2>Registrar un ciclo</h2>
          <div class="campos">
            <label for="ciclo-autoclave">
              Esterilizador
              <p-select
                inputId="ciclo-autoclave"
                name="autoclave"
                [options]="esterilizadores()"
                optionLabel="nombre"
                optionValue="id"
                [(ngModel)]="esterilizadorId"
                placeholder="Elija un equipo"
              />
            </label>
            <label for="ciclo-inicio">
              Inicio del ciclo
              <p-datepicker
                inputId="ciclo-inicio"
                name="inicio"
                [(ngModel)]="iniciadoEn"
                [showTime]="true"
                dateFormat="dd/mm/yy"
                hourFormat="24"
                [maxDate]="ahora"
                appendTo="body"
              />
            </label>
            <label for="ciclo-temp">
              Temperatura en grados
              <p-inputnumber
                inputId="ciclo-temp"
                name="temp"
                [(ngModel)]="temperatura"
                [min]="40"
                [max]="200"
                [minFractionDigits]="1"
                [maxFractionDigits]="1"
              />
            </label>
            <label for="ciclo-presion">
              Presión en kPa
              <p-inputnumber
                inputId="ciclo-presion"
                name="presion"
                [(ngModel)]="presion"
                [min]="0"
                [max]="1000"
              />
            </label>
            <label for="ciclo-duracion">
              Duración en minutos
              <p-inputnumber
                inputId="ciclo-duracion"
                name="duracion"
                [(ngModel)]="duracion"
                [min]="1"
                [max]="600"
              />
            </label>
          </div>
          <fieldset>
            <legend>Sets que entran al ciclo</legend>
            @for (fila of filasSets(); track $index) {
              <div class="fila">
                <p-select
                  [options]="sets()"
                  optionLabel="nombre"
                  optionValue="id"
                  [ngModel]="fila.setId"
                  [ngModelOptions]="{ standalone: true }"
                  (ngModelChange)="cambiarSet($index, 'setId', $event)"
                  placeholder="Elija un set"
                  [attr.aria-label]="'Set ' + ($index + 1)"
                />
                <p-inputnumber
                  [ngModel]="fila.cantidad"
                  [ngModelOptions]="{ standalone: true }"
                  (ngModelChange)="cambiarSet($index, 'cantidad', $event)"
                  [min]="1"
                  [max]="50"
                  [attr.aria-label]="'Cantidad del set ' + ($index + 1)"
                />
                <p-button
                  icon="pi pi-trash"
                  severity="danger"
                  [text]="true"
                  ariaLabel="Quitar set"
                  (onClick)="quitarSet($index)"
                />
              </div>
            }
            <p-button
              label="Agregar set"
              icon="pi pi-plus"
              [text]="true"
              (onClick)="agregarSet()"
            />
          </fieldset>
          <label>
            Observaciones
            <input pInputText name="obs" maxlength="300" [(ngModel)]="observaciones" />
          </label>
          <div class="barra">
            <p-button
              type="submit"
              label="Registrar ciclo"
              icon="pi pi-save"
              [loading]="trabajando()"
              [disabled]="!cicloValido()"
            />
          </div>
        </form>
      }
      <div class="panel tabla">
        <table>
          <caption class="sr-only">
            Ciclos de esterilización recientes
          </caption>
          <thead>
            <tr>
              <th scope="col">Ciclo</th>
              <th scope="col">Equipo</th>
              <th scope="col">Inicio</th>
              <th scope="col">Parámetros</th>
              <th scope="col">Químico</th>
              <th scope="col">Biológico</th>
              <th scope="col">Resultado</th>
              <th scope="col"><span class="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (c of ciclos(); track c.id) {
              <tr>
                <th scope="row">{{ c.numero }}</th>
                <td>{{ c.esterilizador }}</td>
                <td>{{ fechaHora(c.iniciadoEn) }}</td>
                <td>{{ c.temperaturaC }} grados, {{ c.duracionMin }} min</td>
                <td>{{ indicadores[c.indicadorQuimico] }}</td>
                <td>{{ indicadores[c.indicadorBiologico] }}</td>
                <td>
                  <p-tag
                    [value]="etiquetaCiclo[c.resultado]"
                    [severity]="severidadCiclo[c.resultado]"
                  />
                </td>
                <td>
                  @if (esEnfermeria() && c.resultado === 'EN_PROCESO') {
                    <p-button
                      label="Indicadores"
                      size="small"
                      [text]="true"
                      [ariaLabel]="'Registrar indicadores del ciclo ' + c.numero"
                      (onClick)="abrirIndicadores(c)"
                    />
                  }
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="8" class="text-muted">Todavía no hay ciclos registrados.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      @if (indicadoresCiclo(); as c) {
        <form class="panel formulario" (submit)="$event.preventDefault(); guardarIndicadores(c)">
          <h2>Indicadores del ciclo {{ c.numero }}</h2>
          <div class="campos">
            <label for="ind-quimico">
              Indicador químico
              <p-select
                inputId="ind-quimico"
                name="quimico"
                [options]="opcionesIndicador"
                optionLabel="etiqueta"
                optionValue="valor"
                [(ngModel)]="quimico"
              />
            </label>
            <label for="ind-biologico">
              Indicador biológico
              <p-select
                inputId="ind-biologico"
                name="biologico"
                [options]="opcionesIndicador"
                optionLabel="etiqueta"
                optionValue="valor"
                [(ngModel)]="biologico"
              />
            </label>
          </div>
          <label>
            Observaciones
            <input pInputText name="indObs" maxlength="300" [(ngModel)]="observacionesIndicador" />
          </label>
          <p class="text-muted">
            Un indicador no conforme cierra el ciclo y bloquea todos sus lotes. El resultado
            conforme exige los dos indicadores conformes. Un ciclo cerrado ya no se modifica.
          </p>
          <div class="barra">
            <p-button
              type="submit"
              label="Guardar indicadores"
              icon="pi pi-check"
              [loading]="trabajando()"
            />
            <p-button
              label="Cancelar"
              severity="secondary"
              [outlined]="true"
              (onClick)="indicadoresCiclo.set(null)"
            />
          </div>
        </form>
      }
    }

    @if (vista() === 'lotes') {
      <div class="panel tabla">
        <table>
          <caption class="sr-only">
            Lotes de instrumental esterilizado
          </caption>
          <thead>
            <tr>
              <th scope="col">Lote</th>
              <th scope="col">Set</th>
              <th scope="col">Ciclo</th>
              <th scope="col">Esterilizado</th>
              <th scope="col">Vence</th>
              <th scope="col">Estado</th>
              <th scope="col"><span class="sr-only">Acciones</span></th>
            </tr>
          </thead>
          <tbody>
            @for (l of lotes(); track l.id) {
              <tr>
                <th scope="row">{{ l.codigo }}</th>
                <td>{{ l.setNombre }}</td>
                <td>{{ l.cicloNumero }}</td>
                <td>{{ fechaHora(l.esterilizadoEn) }}</td>
                <td>{{ fechaHora(l.venceEn) }}</td>
                <td>
                  <p-tag [value]="etiquetaLote[l.estado]" [severity]="severidadLote[l.estado]" />
                </td>
                <td>
                  <p-button
                    label="Trazabilidad"
                    size="small"
                    [text]="true"
                    [ariaLabel]="'Ver la trazabilidad del lote ' + l.codigo"
                    (onClick)="verTrazabilidad(l)"
                  />
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="7" class="text-muted">No hay lotes vigentes.</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      @if (trazabilidad(); as t) {
        <section class="panel" aria-labelledby="traz-titulo">
          <h2 id="traz-titulo">Trazabilidad del lote {{ t.lote.codigo }}</h2>
          <p>
            Ciclo {{ t.lote.cicloNumero }}:
            <p-tag
              [value]="etiquetaCiclo[t.resultadoCiclo]"
              [severity]="severidadCiclo[t.resultadoCiclo]"
            />
          </p>
          @if (t.usos.length === 0) {
            <p class="text-muted">Este lote no se ha usado en ninguna cirugía.</p>
          } @else {
            <ul class="lista">
              @for (u of t.usos; track u.cirugiaId) {
                <li>
                  Cirugía {{ u.cirugiaCodigo }} el {{ fechaHora(u.fecha) }}
                  @if (u.quirofano) {
                    en {{ u.quirofano }}
                  }
                </li>
              }
            </ul>
          }
          <p-button
            label="Cerrar"
            severity="secondary"
            [outlined]="true"
            (onClick)="trazabilidad.set(null)"
          />
        </section>
      }
    }

    @if (vista() === 'alertas') {
      <section class="panel" aria-labelledby="alertas-titulo">
        <h2 id="alertas-titulo">Alertas de esterilización</h2>
        @if (alertas().length === 0) {
          <p class="text-muted">No hay alertas pendientes.</p>
        } @else {
          <ul class="lista">
            @for (a of alertas(); track a.tipo + a.referencia) {
              <li>
                <p-tag [value]="a.severidad" [severity]="severidadAlerta[a.severidad]" />
                {{ a.descripcion }}
                <small class="text-muted"> · {{ fechaHora(a.fecha) }}</small>
              </li>
            }
          </ul>
        }
      </section>
    }
  `,
  styles: `
    .panel {
      margin-bottom: 1rem;
    }

    h2 {
      margin: 0;
      font-size: 1.05rem;
    }

    .barra {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      align-items: center;
    }

    .formulario {
      display: grid;
      gap: 0.75rem;
    }

    .campos {
      display: grid;
      gap: 0.75rem;
      grid-template-columns: repeat(auto-fit, minmax(13rem, 1fr));
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
      display: grid;
      gap: 0.5rem;
    }

    legend {
      font-weight: 700;
      padding: 0 0.4rem;
    }

    .fila {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      align-items: center;
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

    .lista {
      margin: 0 0 0.75rem;
      padding-left: 1.1rem;
      display: grid;
      gap: 0.4rem;
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
export class EsterilizacionPanel implements OnInit {
  private readonly api = inject(EsterilizacionApiService);
  private readonly avisos = inject(NotificationService);
  private readonly auth = inject(AuthService);

  protected readonly vista = signal<Vista>('ciclos');
  protected readonly ciclos = signal<CicloEsterilizacion[]>([]);
  protected readonly lotes = signal<LoteEsteril[]>([]);
  protected readonly alertas = signal<AlertaEsterilizacion[]>([]);
  protected readonly esterilizadores = signal<Esterilizador[]>([]);
  protected readonly sets = signal<SetInstrumental[]>([]);
  protected readonly filasSets = signal<{ setId: string | null; cantidad: number }[]>([
    { setId: null, cantidad: 1 },
  ]);
  protected readonly indicadoresCiclo = signal<CicloEsterilizacion | null>(null);
  protected readonly trazabilidad = signal<TrazabilidadLote | null>(null);
  protected readonly trabajando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly esEnfermeria = computed(() => this.auth.isEnfermeria());

  protected readonly vistas: { valor: Vista; etiqueta: string }[] = [
    { valor: 'ciclos', etiqueta: 'Ciclos' },
    { valor: 'lotes', etiqueta: 'Lotes' },
    { valor: 'alertas', etiqueta: 'Alertas' },
  ];
  protected readonly opcionesIndicador = (
    Object.keys(ETIQUETA_INDICADOR) as ResultadoIndicador[]
  ).map((valor) => ({ valor, etiqueta: ETIQUETA_INDICADOR[valor] }));
  protected readonly etiquetaLote = ETIQUETA_LOTE;
  protected readonly severidadLote = SEVERIDAD_LOTE;
  protected readonly etiquetaCiclo = ETIQUETA_CICLO;
  protected readonly severidadCiclo = SEVERIDAD_CICLO;
  protected readonly severidadAlerta = SEVERIDAD_ALERTA;
  protected readonly indicadores = ETIQUETA_INDICADOR;
  protected readonly fechaHora = fechaHora;
  protected readonly ahora = new Date();

  protected esterilizadorId: string | null = null;
  protected iniciadoEn: Date | null = null;
  protected temperatura: number | null = 134;
  protected presion: number | null = null;
  protected duracion: number | null = 30;
  protected observaciones = '';
  protected quimico: ResultadoIndicador = 'CONFORME';
  protected biologico: ResultadoIndicador = 'CONFORME';
  protected observacionesIndicador = '';

  ngOnInit(): void {
    this.recargar();
    if (this.esEnfermeria()) {
      this.api.esterilizadores().subscribe({
        next: (e) => this.esterilizadores.set(e.filter((x) => x.activo)),
      });
      this.api.sets().subscribe({ next: (s) => this.sets.set(s.filter((x) => x.activo)) });
    }
  }

  protected agregarSet(): void {
    this.filasSets.update((f) => [...f, { setId: null, cantidad: 1 }]);
  }

  protected quitarSet(indice: number): void {
    this.filasSets.update((f) => f.filter((_, i) => i !== indice));
  }

  protected cambiarSet(indice: number, campo: 'setId' | 'cantidad', valor: string | number): void {
    this.filasSets.update((f) =>
      f.map((fila, i) => (i === indice ? { ...fila, [campo]: valor } : fila)),
    );
  }

  protected cicloValido(): boolean {
    const filas = this.filasSets();
    return (
      !!this.esterilizadorId &&
      !!this.iniciadoEn &&
      !!this.temperatura &&
      !!this.duracion &&
      filas.length > 0 &&
      filas.every((f) => !!f.setId && f.cantidad >= 1)
    );
  }

  protected crearCiclo(): void {
    if (!this.cicloValido() || !this.esterilizadorId || !this.iniciadoEn) {
      return;
    }
    this.trabajando.set(true);
    this.error.set(null);
    this.api
      .crearCiclo({
        esterilizadorId: this.esterilizadorId,
        iniciadoEn: aIsoLocal(this.iniciadoEn),
        temperaturaC: this.temperatura ?? 134,
        presionKpa: this.presion,
        duracionMin: this.duracion ?? 30,
        observaciones: this.observaciones.trim() || null,
        sets: this.filasSets().map((f) => ({ setId: f.setId ?? '', cantidad: f.cantidad })),
      })
      .subscribe({
        next: (c) => {
          this.trabajando.set(false);
          this.avisos.success(
            `El ciclo ${c.numero} quedó registrado con ${c.lotes.length} lote(s)`,
          );
          this.filasSets.set([{ setId: null, cantidad: 1 }]);
          this.observaciones = '';
          this.recargar();
        },
        error: (e: { error?: { message?: string } }) => {
          this.trabajando.set(false);
          this.error.set(e.error?.message ?? 'No pudimos registrar el ciclo');
        },
      });
  }

  protected abrirIndicadores(c: CicloEsterilizacion): void {
    this.indicadoresCiclo.set(c);
    this.quimico = c.indicadorQuimico === 'PENDIENTE' ? 'CONFORME' : c.indicadorQuimico;
    this.biologico = c.indicadorBiologico === 'PENDIENTE' ? 'CONFORME' : c.indicadorBiologico;
    this.observacionesIndicador = '';
  }

  protected guardarIndicadores(c: CicloEsterilizacion): void {
    this.trabajando.set(true);
    this.error.set(null);
    this.api
      .indicadores(c.id, {
        quimico: this.quimico,
        biologico: this.biologico,
        observaciones: this.observacionesIndicador.trim() || null,
      })
      .subscribe({
        next: (r) => {
          this.trabajando.set(false);
          this.indicadoresCiclo.set(null);
          this.avisos.success(
            `El ciclo ${r.numero} quedó ${ETIQUETA_CICLO[r.resultado].toLowerCase()}`,
          );
          this.recargar();
        },
        error: (e: { error?: { message?: string } }) => {
          this.trabajando.set(false);
          this.error.set(e.error?.message ?? 'No pudimos guardar los indicadores');
        },
      });
  }

  protected verTrazabilidad(l: LoteEsteril): void {
    this.api.trazabilidadLote(l.id).subscribe({
      next: (t) => this.trazabilidad.set(t),
      error: () => this.error.set('No pudimos cargar la trazabilidad'),
    });
  }

  private recargar(): void {
    this.api.ciclos().subscribe({
      next: (c) => this.ciclos.set(c),
      error: () => this.error.set('No pudimos cargar los ciclos'),
    });
    this.api.lotes().subscribe({ next: (l) => this.lotes.set(l) });
    this.api.alertas().subscribe({ next: (a) => this.alertas.set(a) });
  }
}
