import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ContabilidadApiService,
  CuentaTesoreriaResponse,
  ETIQUETA_ESTADO_LINEA_EXTRACTO,
  ExtractoLineaResponse,
  ExtractoResponse,
  MovimientoTesoreriaResponse,
  NotificationService,
  ResumenConciliacion,
  SugerenciaConciliacion,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';

@Component({
  selector: 'app-conciliacion',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Conciliación bancaria"
      subtitle="Importa el extracto en CSV, revisa las coincidencias sugeridas y concilia contra tus movimientos."
      icon="pi-check-square"
    />

    <section class="panel">
      <div class="filtros">
        <p-select
          [options]="opcionesCuenta()"
          optionLabel="label"
          optionValue="value"
          [ngModel]="cuentaId()"
          (ngModelChange)="elegirCuenta($event)"
          placeholder="Elige una cuenta bancaria"
          ariaLabel="Cuenta bancaria a conciliar"
          appendTo="body"
        />
        @if (esFinanzas() && cuentaId()) {
          <label class="archivo" for="cn-archivo">
            <span class="p-button p-button-outlined p-button-sm">
              <i class="pi pi-upload" aria-hidden="true"></i>
              Importar extracto CSV
            </span>
            <input
              id="cn-archivo"
              type="file"
              accept=".csv,text/csv,text/plain"
              class="oculto"
              (change)="importar($event)"
            />
          </label>
        }
      </div>
      <p class="aviso" role="note">
        El CSV lleva una línea por movimiento con las columnas fecha, descripción, referencia y
        monto. Los abonos son positivos y los cargos negativos. Se acepta coma o punto y coma, y
        fechas aaaa-mm-dd o dd/mm/aaaa.
      </p>
      @if (mensaje(); as m) {
        <p class="aviso" role="status">{{ m }}</p>
      }
      @if (error(); as e) {
        <p class="error" role="alert">{{ e }}</p>
      }
    </section>

    @if (resumen(); as r) {
      <div class="resumen" aria-label="Resumen de la conciliación">
        <div>
          <strong>{{ r.saldoLibros | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Saldo en libros</span>
        </div>
        <div [class.alerta]="r.lineasPendientes > 0">
          <strong>{{ r.lineasPendientes }}</strong>
          <span>Líneas del extracto pendientes</span>
        </div>
        <div [class.alerta]="r.movimientosSinConciliar > 0">
          <strong>{{ r.movimientosSinConciliar }}</strong>
          <span>Movimientos sin conciliar</span>
        </div>
        <div [class.alerta]="r.diferencia !== 0">
          <strong>{{ r.diferencia | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Diferencia pendiente</span>
        </div>
      </div>
    }

    @if (cuentaId()) {
      <section class="panel">
        <h3 class="subseccion">Coincidencias sugeridas</h3>
        <p-table [value]="sugerencias()" [rowHover]="true" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Extracto</th>
              <th>Movimiento</th>
              <th class="text-right">Monto</th>
              <th>Confianza</th>
              <th style="width: 9rem"></th>
            </tr>
          </ng-template>
          <ng-template #body let-s>
            <tr>
              <td>
                {{ s.linea.fecha | date: 'd MMM y' }}
                <small class="block text-muted">{{ s.linea.descripcion }}</small>
              </td>
              <td>
                {{ s.movimiento.fecha | date: 'd MMM y' }}
                <small class="block text-muted">{{ s.movimiento.descripcion }}</small>
              </td>
              <td class="text-right">{{ s.linea.monto | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td>
                <p-tag
                  [value]="s.puntaje >= 110 ? 'Alta' : 'Media'"
                  [severity]="s.puntaje >= 110 ? 'success' : 'info'"
                />
              </td>
              <td class="derecha">
                @if (esFinanzas()) {
                  <p-button
                    label="Conciliar"
                    icon="pi pi-check"
                    size="small"
                    [loading]="ocupado() === s.linea.id"
                    [ariaLabel]="'Conciliar ' + s.linea.descripcion"
                    (onClick)="conciliar(s)"
                  />
                }
              </td>
            </tr>
          </ng-template>
          <ng-template #emptymessage>
            <tr>
              <td colspan="5">
                <app-empty-state
                  icon="pi-link"
                  title="Sin coincidencias"
                  message="No hay coincidencias de importe y fecha para sugerir."
                />
              </td>
            </tr>
          </ng-template>
        </p-table>
      </section>

      <div class="columnas">
        <section class="panel">
          <h3 class="subseccion">Líneas del extracto</h3>
          <p-table
            [value]="lineas()"
            [paginator]="lineas().length > 8"
            [rows]="8"
            [rowHover]="true"
            responsiveLayout="scroll"
          >
            <ng-template #header>
              <tr>
                <th>Fecha</th>
                <th>Descripción</th>
                <th class="text-right">Monto</th>
                <th>Estado</th>
                <th style="width: 7rem"></th>
              </tr>
            </ng-template>
            <ng-template #body let-l>
              <tr>
                <td>{{ l.fecha | date: 'd MMM y' }}</td>
                <td>
                  {{ l.descripcion }}
                  @if (l.referencia) {
                    <small class="block text-muted">{{ l.referencia }}</small>
                  }
                </td>
                <td class="text-right">{{ l.monto | currency: 'PEN' : 'symbol-narrow' }}</td>
                <td>
                  <p-tag
                    [value]="estados[l.estado]"
                    [severity]="
                      l.estado === 'CONCILIADA'
                        ? 'success'
                        : l.estado === 'IGNORADA'
                          ? 'secondary'
                          : 'warn'
                    "
                  />
                </td>
                <td class="derecha">
                  @if (esFinanzas()) {
                    @if (l.estado === 'PENDIENTE') {
                      <p-button
                        icon="pi pi-eye-slash"
                        size="small"
                        [text]="true"
                        [rounded]="true"
                        severity="secondary"
                        [ariaLabel]="'Ignorar ' + l.descripcion"
                        (onClick)="ignorar(l)"
                      />
                    } @else {
                      <p-button
                        icon="pi pi-undo"
                        size="small"
                        [text]="true"
                        [rounded]="true"
                        severity="secondary"
                        [ariaLabel]="'Deshacer ' + l.descripcion"
                        (onClick)="deshacer(l)"
                      />
                    }
                  }
                </td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr>
                <td colspan="5">
                  <app-empty-state
                    icon="pi-file"
                    title="Sin líneas"
                    message="Importa un extracto para empezar."
                  />
                </td>
              </tr>
            </ng-template>
          </p-table>
        </section>

        <section class="panel">
          <h3 class="subseccion">Movimientos sin conciliar</h3>
          <p-table
            [value]="sinConciliar()"
            [paginator]="sinConciliar().length > 8"
            [rows]="8"
            [rowHover]="true"
            responsiveLayout="scroll"
          >
            <ng-template #header>
              <tr>
                <th>Fecha</th>
                <th>Descripción</th>
                <th class="text-right">Monto</th>
              </tr>
            </ng-template>
            <ng-template #body let-m>
              <tr>
                <td>{{ m.fecha | date: 'd MMM y' }}</td>
                <td>{{ m.descripcion }}</td>
                <td class="text-right">
                  {{ m.tipo === 'EGRESO' ? '-' : ''
                  }}{{ m.monto | currency: 'PEN' : 'symbol-narrow' }}
                </td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr>
                <td colspan="3">
                  <app-empty-state
                    icon="pi-check-circle"
                    title="Todo conciliado"
                    message="No quedan movimientos por conciliar."
                  />
                </td>
              </tr>
            </ng-template>
          </p-table>
        </section>
      </div>

      @if (extractos().length > 0) {
        <section class="panel">
          <h3 class="subseccion">Extractos importados</h3>
          <ul class="historial">
            @for (e of extractos(); track e.id) {
              <li>
                <strong>{{ e.archivo }}</strong>
                <span>{{ e.lineas }} líneas · {{ e.importadoPor }}</span>
                <span class="text-muted">{{ e.importadoEn | date: 'd MMM y HH:mm' }}</span>
              </li>
            }
          </ul>
        </section>
      }
    }
  `,
  styles: [
    ESTILOS_CONTABILIDAD,
    `
      .oculto {
        position: absolute;
        width: 1px;
        height: 1px;
        opacity: 0;
        overflow: hidden;
      }

      .archivo {
        cursor: pointer;
        position: relative;
      }

      .archivo:focus-within span {
        outline: 2px solid var(--p-primary-color);
        outline-offset: 2px;
      }

      .historial {
        list-style: none;
        margin: 0;
        padding: 0;
        display: grid;
        gap: 0.5rem;
      }

      .historial li {
        display: flex;
        gap: 0.75rem;
        padding: 0.4rem 0;
        border-bottom: 1px solid var(--p-content-border-color);
        font-size: 0.9rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Conciliacion implements OnInit {
  private readonly api = inject(ContabilidadApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);

  protected readonly estados: Record<string, string> = ETIQUETA_ESTADO_LINEA_EXTRACTO;
  protected readonly cuentas = signal<CuentaTesoreriaResponse[]>([]);
  protected readonly cuentaId = signal<string | null>(null);
  protected readonly resumen = signal<ResumenConciliacion | null>(null);
  protected readonly sugerencias = signal<SugerenciaConciliacion[]>([]);
  protected readonly lineas = signal<ExtractoLineaResponse[]>([]);
  protected readonly sinConciliar = signal<MovimientoTesoreriaResponse[]>([]);
  protected readonly extractos = signal<ExtractoResponse[]>([]);
  protected readonly ocupado = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly opcionesCuenta = computed(() =>
    this.cuentas()
      .filter((c) => c.tipo === 'BANCO')
      .map((c) => ({ label: c.nombre, value: c.id })),
  );

  ngOnInit(): void {
    this.api.cuentasTesoreria(true).subscribe((c) => {
      this.cuentas.set(c);
      const primera = c.find((x) => x.tipo === 'BANCO');
      if (primera) {
        this.elegirCuenta(primera.id);
      }
    });
  }

  protected elegirCuenta(id: string | null): void {
    this.cuentaId.set(id);
    this.mensaje.set(null);
    this.error.set(null);
    if (id) {
      this.cargar();
    }
  }

  private cargar(): void {
    const id = this.cuentaId();
    if (!id) {
      return;
    }
    this.api.resumenConciliacion(id).subscribe((r) => this.resumen.set(r));
    this.api.sugerencias(id).subscribe((s) => this.sugerencias.set(s));
    this.api.lineasExtracto(id).subscribe((l) => this.lineas.set(l));
    this.api.movimientosSinConciliar(id).subscribe((m) => this.sinConciliar.set(m));
    this.api.extractos(id).subscribe((e) => this.extractos.set(e));
  }

  protected importar(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    const id = this.cuentaId();
    this.error.set(null);
    this.mensaje.set(null);
    if (!archivo || !id) {
      return;
    }
    if (archivo.size > 900_000) {
      this.error.set('El archivo supera el máximo de 900 KB.');
      entrada.value = '';
      return;
    }
    archivo.text().then((contenido) => {
      this.api.importarExtracto(id, archivo.name, contenido).subscribe({
        next: (e) => {
          entrada.value = '';
          this.mensaje.set(
            `Se importaron ${e.lineas} líneas${e.duplicadas > 0 ? ` y se omitieron ${e.duplicadas} repetidas` : ''}.`,
          );
          this.cargar();
        },
        error: () => {
          entrada.value = '';
        },
      });
    });
  }

  protected conciliar(s: SugerenciaConciliacion): void {
    this.ocupado.set(s.linea.id);
    this.api.conciliar(s.linea.id, s.movimiento.id).subscribe({
      next: () => {
        this.ocupado.set(null);
        this.notificaciones.success('La línea fue conciliada.');
        this.cargar();
      },
      error: () => this.ocupado.set(null),
    });
  }

  protected ignorar(l: ExtractoLineaResponse): void {
    this.api.ignorarLinea(l.id).subscribe(() => this.cargar());
  }

  protected deshacer(l: ExtractoLineaResponse): void {
    this.api.deshacerConciliacion(l.id).subscribe(() => this.cargar());
  }
}
