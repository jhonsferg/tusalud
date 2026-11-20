import { DatePipe } from '@angular/common';
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
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Tag } from 'primeng/tag';

import {
  ApiError,
  DescargaService,
  DispensacionResponse,
  ETIQUETA_METODO_FARMACIA,
  METODOS_PAGO_FARMACIA,
  MetodoPagoFarmacia,
  NotificationService,
  RecetaFarmacia,
  RecetasApiService,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import {
  ETIQUETA_ESTADO_RECETA,
  SEVERIDAD_ESTADO_RECETA,
  cantidadPendiente,
  textoVigencia,
} from '../recetas.utils';

@Component({
  selector: 'app-dispensacion-receta',
  imports: [
    DatePipe,
    FormsModule,
    Button,
    InputNumber,
    InputText,
    Message,
    Select,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Recetas electrónicas"
      subtitle="Busca una receta vigente y dispénsala, total o parcialmente"
      icon="pi-file-edit"
    />
    <div class="panel">
      <form class="busqueda" (submit)="$event.preventDefault(); buscar()">
        <label for="busqueda-receta">
          Número de receta, DNI o nombre del paciente
          <input pInputText id="busqueda-receta" name="q" [(ngModel)]="consulta" />
        </label>
        <p-button type="submit" label="Buscar" icon="pi pi-search" [loading]="cargando()" />
      </form>
      @if (error()) {
        <p-message severity="error">{{ error() }}</p-message>
      }
    </div>

    @if (resultado(); as r) {
      <div class="panel" aria-live="polite">
        <p-message severity="success"
          >Dispensación {{ r.comprobante }} registrada por S/ {{ r.total }}</p-message
        >
      </div>
    }

    <div class="columnas">
      <div class="panel lista">
        <h3>Recetas vigentes</h3>
        @if (recetas().length === 0 && !cargando()) {
          <app-empty-state
            icon="pi-file-edit"
            title="Sin recetas para dispensar"
            message="Escribe un número, un DNI o un nombre para buscar."
          />
        }
        <ul>
          @for (r of recetas(); track r.id) {
            <li>
              <button
                type="button"
                class="item"
                [class.activo]="seleccionada()?.id === r.id"
                (click)="seleccionar(r)"
                [attr.aria-label]="'Abrir la receta ' + r.numero + ' de ' + r.pacienteNombre"
              >
                <strong>{{ r.numero }}</strong>
                <span>{{ r.pacienteNombre }}</span>
                <small class="text-muted"
                  >{{ pendientes(r) }} por dispensar · {{ vigencia(r) }}</small
                >
                @if (r.controlEspecial) {
                  <small class="control">Control especial</small>
                }
              </button>
            </li>
          }
        </ul>
      </div>

      @if (seleccionada(); as s) {
        <form class="panel detalle" (submit)="$event.preventDefault(); dispensar()">
          <header>
            <div>
              <h3>{{ s.numero }}</h3>
              <p class="text-muted">
                {{ s.pacienteNombre }} · DNI {{ s.pacienteDni }}<br />
                {{ s.medicoNombre }} · CMP {{ s.medicoColegiatura }}<br />
                Emitida el {{ s.emitidaEn | date: 'd MMM y, HH:mm' }} · {{ vigencia(s) }}
              </p>
            </div>
            <p-tag [value]="etiqueta[s.estado]" [severity]="severidad[s.estado]" />
          </header>
          @if (s.controlEspecial) {
            <p-message severity="warn"
              >Receta con medicamentos de control especial: verifique la identidad del paciente y el
              documento antes de dispensar.</p-message
            >
          }
          <table>
            <caption class="sr-only">
              Medicamentos de la receta
            </caption>
            <thead>
              <tr>
                <th scope="col">Medicamento</th>
                <th scope="col">Indicación</th>
                <th scope="col">Pendiente</th>
                <th scope="col">Dispensar ahora</th>
              </tr>
            </thead>
            <tbody>
              @for (i of s.items; track i.id) {
                <tr>
                  <th scope="row">
                    {{ i.medicamento }}
                    @if (!i.productoId) {
                      <small class="control">Sin producto de farmacia vinculado</small>
                    }
                  </th>
                  <td>{{ [i.dosis, i.frecuencia, i.duracion].filter(presente).join(' · ') }}</td>
                  <td>{{ i.pendiente }} de {{ i.cantidad }}</td>
                  <td>
                    <p-inputnumber
                      [inputId]="'cantidad-' + i.id"
                      [ngModel]="cantidades()[i.id] ?? 0"
                      (ngModelChange)="cambiar(i.id, $event)"
                      [name]="'cantidad-' + i.id"
                      [min]="0"
                      [max]="i.pendiente"
                      [disabled]="i.pendiente === 0 || !i.productoId"
                      [useGrouping]="false"
                      [ariaLabel]="'Cantidad a dispensar de ' + i.medicamento"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
          <div class="pago">
            <label for="metodo-pago-receta">
              Método de pago
              <p-select
                inputId="metodo-pago-receta"
                name="metodo"
                [options]="metodos"
                optionLabel="etiqueta"
                optionValue="valor"
                [(ngModel)]="metodoPago"
              />
            </label>
            <p-button
              type="submit"
              label="Dispensar"
              icon="pi pi-check"
              [loading]="dispensando()"
              [disabled]="total() === 0"
            />
            <p-button
              label="Ver PDF"
              icon="pi pi-file-pdf"
              severity="secondary"
              [outlined]="true"
              (onClick)="pdf(s)"
            />
          </div>
          @if (errorDispensacion()) {
            <p-message severity="error">{{ errorDispensacion() }}</p-message>
          }
        </form>
      }
    </div>
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
    .busqueda {
      display: flex;
      align-items: end;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    label {
      display: grid;
      gap: 0.25rem;
      font-weight: 600;
    }

    .columnas {
      display: grid;
      gap: 1rem;
      grid-template-columns: minmax(16rem, 22rem) 1fr;
      margin-top: 1rem;
    }

    @media (max-width: 900px) {
      .columnas {
        grid-template-columns: 1fr;
      }
    }

    h3 {
      margin: 0 0 0.5rem;
    }

    ul {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      gap: 0.5rem;
    }

    .item {
      width: 100%;
      text-align: left;
      display: grid;
      gap: 0.15rem;
      padding: 0.6rem 0.75rem;
      border: 1px solid var(--p-surface-200, #e2e8f0);
      border-radius: 0.5rem;
      background: transparent;
      cursor: pointer;
      color: inherit;
    }

    .item.activo {
      border-color: var(--p-primary-color);
      background: var(--p-primary-50, #f0fdfa);
    }

    .control {
      color: #b45309;
      font-weight: 600;
    }

    .detalle {
      display: grid;
      gap: 0.75rem;
    }

    .detalle header {
      display: flex;
      justify-content: space-between;
      gap: 0.75rem;
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

    .pago {
      display: flex;
      align-items: end;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DispensacionReceta implements OnInit {
  private readonly api = inject(RecetasApiService);
  private readonly descargas = inject(DescargaService);
  private readonly notifications = inject(NotificationService);

  protected readonly etiqueta = ETIQUETA_ESTADO_RECETA;
  protected readonly severidad = SEVERIDAD_ESTADO_RECETA;
  protected readonly metodos = METODOS_PAGO_FARMACIA.map((valor) => ({
    valor,
    etiqueta: ETIQUETA_METODO_FARMACIA[valor],
  }));
  protected readonly presente = (texto: string | null): texto is string => !!texto;
  protected consulta = '';
  protected metodoPago: MetodoPagoFarmacia = 'EFECTIVO';

  protected readonly recetas = signal<RecetaFarmacia[]>([]);
  protected readonly seleccionada = signal<RecetaFarmacia | null>(null);
  protected readonly cantidades = signal<Record<string, number>>({});
  protected readonly cargando = signal(false);
  protected readonly dispensando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errorDispensacion = signal<string | null>(null);
  protected readonly resultado = signal<DispensacionResponse | null>(null);

  protected readonly total = computed(() =>
    Object.values(this.cantidades()).reduce((suma, cantidad) => suma + (cantidad || 0), 0),
  );

  ngOnInit(): void {
    this.buscar();
  }

  protected pendientes(r: RecetaFarmacia): number {
    return cantidadPendiente(r);
  }

  protected vigencia(r: RecetaFarmacia): string {
    return textoVigencia(true, r.vigenteHasta);
  }

  protected buscar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.api.paraDispensar(this.consulta).subscribe({
      next: (lista) => {
        this.recetas.set(lista);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron buscar las recetas');
        this.cargando.set(false);
      },
    });
  }

  protected seleccionar(r: RecetaFarmacia): void {
    this.seleccionada.set(r);
    this.cantidades.set({});
    this.errorDispensacion.set(null);
  }

  protected cambiar(itemId: string, cantidad: number | null): void {
    this.cantidades.update((actual) => ({ ...actual, [itemId]: cantidad ?? 0 }));
  }

  protected dispensar(): void {
    const receta = this.seleccionada();
    if (!receta || this.total() === 0) {
      return;
    }
    const items = Object.entries(this.cantidades())
      .filter(([, cantidad]) => cantidad > 0)
      .map(([itemId, cantidad]) => ({ itemId, cantidad }));
    this.dispensando.set(true);
    this.errorDispensacion.set(null);
    this.api.dispensar(receta.id, { items, metodoPago: this.metodoPago }).subscribe({
      next: (dispensacion) => {
        this.dispensando.set(false);
        this.resultado.set(dispensacion);
        this.notifications.success(`Dispensación ${dispensacion.comprobante} registrada.`);
        this.cantidades.set({});
        this.api.detalleFarmacia(receta.id).subscribe({
          next: (actual) => {
            this.seleccionada.set(actual.estado === 'DISPENSADA' ? null : actual);
            this.buscar();
          },
          error: () => this.buscar(),
        });
      },
      error: (error: ApiError) => {
        this.dispensando.set(false);
        this.errorDispensacion.set(error.message ?? 'No se pudo dispensar la receta');
      },
    });
  }

  protected pdf(r: RecetaFarmacia): void {
    this.api.pdf(r.id).subscribe({ next: (archivo) => this.descargas.guardar(archivo) });
  }
}
