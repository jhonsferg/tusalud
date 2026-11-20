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
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  BalanceComprobacionResponse,
  ContabilidadApiService,
  CuentaContableResponse,
  EstadoResultadosResponse,
  EstadoSituacionResponse,
  LibroMayorResponse,
  toIsoDate,
} from '@tusalud/core';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_CONTABILIDAD } from '../contabilidad.estilos';
import { inicioDeMes } from '../contabilidad.utils';

type Vista = 'balance' | 'mayor' | 'situacion' | 'resultados';

@Component({
  selector: 'app-libros-contables',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Select,
    SelectButton,
    TableModule,
    Tag,
    BotonExportar,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Libros y estados financieros"
      subtitle="Balance de comprobación, libro mayor, situación financiera y resultados"
      icon="pi-chart-bar"
    />

    <section class="panel">
      <div class="filtros">
        <p-selectbutton
          [options]="vistas"
          optionLabel="label"
          optionValue="value"
          [ngModel]="vista()"
          (ngModelChange)="cambiarVista($event)"
          [allowEmpty]="false"
          ariaLabel="Elegir el libro o estado"
        />
      </div>
      <div class="filtros">
        @if (vista() !== 'situacion') {
          <p-datepicker
            [ngModel]="desde()"
            (ngModelChange)="desde.set($event)"
            dateFormat="dd/mm/yy"
            placeholder="Desde"
            ariaLabel="Fecha inicial"
            appendTo="body"
          />
        }
        <p-datepicker
          [ngModel]="hasta()"
          (ngModelChange)="hasta.set($event)"
          dateFormat="dd/mm/yy"
          [placeholder]="vista() === 'situacion' ? 'Al' : 'Hasta'"
          [ariaLabel]="vista() === 'situacion' ? 'Fecha de corte' : 'Fecha final'"
          appendTo="body"
        />
        @if (vista() === 'mayor') {
          <p-select
            [options]="cuentasOpciones()"
            optionLabel="etiqueta"
            optionValue="id"
            [ngModel]="cuentaId()"
            (ngModelChange)="cuentaId.set($event)"
            [filter]="true"
            filterBy="etiqueta"
            placeholder="Elige la cuenta"
            ariaLabel="Cuenta del libro mayor"
            appendTo="body"
          />
        }
        <p-button
          label="Consultar"
          icon="pi pi-search"
          [loading]="cargando()"
          (onClick)="consultar()"
        />
        @switch (vista()) {
          @case ('balance') {
            <app-boton-exportar
              tipo="balance-comprobacion"
              formato="XLSX"
              [parametros]="parametros()"
            />
          }
          @case ('mayor') {
            <app-boton-exportar
              tipo="libro-mayor"
              formato="XLSX"
              [parametros]="parametros()"
              [deshabilitado]="!cuentaId()"
            />
          }
          @default {
            <app-boton-exportar tipo="reporte-contable" formato="PDF" [parametros]="parametros()" />
            <app-boton-exportar
              tipo="reporte-contable"
              formato="XLSX"
              [parametros]="parametros()"
            />
          }
        }
        <app-boton-exportar
          tipo="libro-diario"
          formato="XLSX"
          etiqueta="Libro diario"
          [parametros]="parametros()"
        />
      </div>

      @if (error(); as e) {
        <p class="error" role="alert">{{ e }}</p>
      }

      @switch (vista()) {
        @case ('balance') {
          @if (balance(); as b) {
            <p-table [value]="b.filas" [rowHover]="true" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Cuenta</th>
                  <th class="text-right">Inicial deudor</th>
                  <th class="text-right">Inicial acreedor</th>
                  <th class="text-right">Debe</th>
                  <th class="text-right">Haber</th>
                  <th class="text-right">Saldo deudor</th>
                  <th class="text-right">Saldo acreedor</th>
                </tr>
              </ng-template>
              <ng-template #body let-f>
                <tr [class.agregada]="f.agregada">
                  <td>{{ f.codigo }} {{ f.nombre }}</td>
                  <td class="text-right">
                    {{ f.inicialDeudor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right">
                    {{ f.inicialAcreedor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right">{{ f.debe | currency: 'PEN' : 'symbol-narrow' }}</td>
                  <td class="text-right">{{ f.haber | currency: 'PEN' : 'symbol-narrow' }}</td>
                  <td class="text-right">
                    {{ f.finalDeudor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right">
                    {{ f.finalAcreedor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                </tr>
              </ng-template>
              <ng-template #footer>
                <tr>
                  <td class="negrita">
                    Totales
                    <p-tag
                      [value]="b.cuadra ? 'Cuadra' : 'No cuadra'"
                      [severity]="b.cuadra ? 'success' : 'danger'"
                    />
                  </td>
                  <td></td>
                  <td></td>
                  <td class="text-right negrita">
                    {{ b.totalDebe | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right negrita">
                    {{ b.totalHaber | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right negrita">
                    {{ b.totalDeudor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                  <td class="text-right negrita">
                    {{ b.totalAcreedor | currency: 'PEN' : 'symbol-narrow' }}
                  </td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="7">
                    <app-empty-state
                      icon="pi-chart-bar"
                      title="Sin movimientos"
                      message="No hay asientos en este rango."
                    />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
        }
        @case ('mayor') {
          @if (mayor(); as m) {
            <dl class="ficha">
              <div>
                <dt>Cuenta</dt>
                <dd>{{ m.cuenta.codigo }} {{ m.cuenta.nombre }}</dd>
              </div>
              <div>
                <dt>Saldo inicial</dt>
                <dd>{{ m.saldoInicial | currency: 'PEN' : 'symbol-narrow' }}</dd>
              </div>
              <div>
                <dt>Saldo final</dt>
                <dd>{{ m.saldoFinal | currency: 'PEN' : 'symbol-narrow' }}</dd>
              </div>
            </dl>
            <p-table [value]="m.movimientos" [rowHover]="true" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Fecha</th>
                  <th>Asiento</th>
                  <th>Glosa</th>
                  <th class="text-right">Debe</th>
                  <th class="text-right">Haber</th>
                  <th class="text-right">Saldo</th>
                </tr>
              </ng-template>
              <ng-template #body let-f>
                <tr>
                  <td>{{ f.fecha | date: 'd MMM y' }}</td>
                  <td>{{ f.asiento }}</td>
                  <td>{{ f.glosa }}</td>
                  <td class="text-right">{{ f.debe | currency: 'PEN' : 'symbol-narrow' }}</td>
                  <td class="text-right">{{ f.haber | currency: 'PEN' : 'symbol-narrow' }}</td>
                  <td class="text-right">{{ f.saldo | currency: 'PEN' : 'symbol-narrow' }}</td>
                </tr>
              </ng-template>
              <ng-template #emptymessage>
                <tr>
                  <td colspan="6">
                    <app-empty-state
                      icon="pi-book"
                      title="Sin movimientos"
                      message="La cuenta no tiene movimientos en este rango."
                    />
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
        }
        @case ('situacion') {
          @if (situacion(); as s) {
            <div class="columnas">
              <div>
                @for (sec of [s.activo]; track sec.titulo) {
                  <h3 class="subseccion">{{ sec.titulo }}</h3>
                  @for (l of sec.lineas; track l.codigo) {
                    <div class="flex justify-content-between">
                      <span>{{ l.codigo }} {{ l.nombre }}</span>
                      <span>{{ l.importe | currency: 'PEN' : 'symbol-narrow' }}</span>
                    </div>
                  }
                  <div class="flex justify-content-between negrita">
                    <span>Total {{ sec.titulo.toLowerCase() }}</span>
                    <span>{{ sec.total | currency: 'PEN' : 'symbol-narrow' }}</span>
                  </div>
                }
              </div>
              <div>
                @for (sec of [s.pasivo, s.patrimonio]; track sec.titulo) {
                  <h3 class="subseccion">{{ sec.titulo }}</h3>
                  @for (l of sec.lineas; track l.codigo + l.nombre) {
                    <div class="flex justify-content-between">
                      <span>{{ l.codigo }} {{ l.nombre }}</span>
                      <span>{{ l.importe | currency: 'PEN' : 'symbol-narrow' }}</span>
                    </div>
                  }
                  <div class="flex justify-content-between negrita">
                    <span>Total {{ sec.titulo.toLowerCase() }}</span>
                    <span>{{ sec.total | currency: 'PEN' : 'symbol-narrow' }}</span>
                  </div>
                }
                <div class="flex justify-content-between negrita">
                  <span>Pasivo y patrimonio</span>
                  <span>{{ s.totalPasivoPatrimonio | currency: 'PEN' : 'symbol-narrow' }}</span>
                </div>
              </div>
            </div>
            <p class="aviso" role="status">
              {{
                s.cuadra
                  ? 'El activo es igual al pasivo más el patrimonio.'
                  : 'El activo no es igual al pasivo más el patrimonio. Revisa los asientos.'
              }}
            </p>
          }
        }
        @case ('resultados') {
          @if (resultados(); as r) {
            <div class="columnas">
              <div>
                <h3 class="subseccion">Ingresos</h3>
                @for (l of r.ingresos.lineas; track l.codigo) {
                  <div class="flex justify-content-between">
                    <span>{{ l.codigo }} {{ l.nombre }}</span>
                    <span>{{ l.importe | currency: 'PEN' : 'symbol-narrow' }}</span>
                  </div>
                }
                <div class="flex justify-content-between negrita">
                  <span>Total ingresos</span>
                  <span>{{ r.ingresos.total | currency: 'PEN' : 'symbol-narrow' }}</span>
                </div>
              </div>
              <div>
                <h3 class="subseccion">Gastos</h3>
                @for (l of r.gastos.lineas; track l.codigo) {
                  <div class="flex justify-content-between">
                    <span>{{ l.codigo }} {{ l.nombre }}</span>
                    <span>{{ l.importe | currency: 'PEN' : 'symbol-narrow' }}</span>
                  </div>
                }
                <div class="flex justify-content-between negrita">
                  <span>Total gastos</span>
                  <span>{{ r.gastos.total | currency: 'PEN' : 'symbol-narrow' }}</span>
                </div>
              </div>
            </div>
            <p class="aviso negrita" role="status">
              Resultado del periodo: {{ r.resultado | currency: 'PEN' : 'symbol-narrow' }}
            </p>
          }
        }
      }
    </section>
  `,
  styles: ESTILOS_CONTABILIDAD,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LibrosContables implements OnInit {
  private readonly api = inject(ContabilidadApiService);

  protected readonly vistas = [
    { label: 'Balance de comprobación', value: 'balance' },
    { label: 'Libro mayor', value: 'mayor' },
    { label: 'Situación financiera', value: 'situacion' },
    { label: 'Resultados', value: 'resultados' },
  ];
  protected readonly vista = signal<Vista>('balance');
  protected readonly desde = signal<Date>(inicioDeMes());
  protected readonly hasta = signal<Date>(new Date());
  protected readonly cuentaId = signal<string | null>(null);
  protected readonly cuentas = signal<CuentaContableResponse[]>([]);
  protected readonly cuentasOpciones = computed(() =>
    this.cuentas()
      .filter((c) => c.aceptaMovimientos)
      .map((c) => ({ id: c.id, etiqueta: `${c.codigo} ${c.nombre}` })),
  );
  protected readonly balance = signal<BalanceComprobacionResponse | null>(null);
  protected readonly mayor = signal<LibroMayorResponse | null>(null);
  protected readonly situacion = signal<EstadoSituacionResponse | null>(null);
  protected readonly resultados = signal<EstadoResultadosResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly parametros = computed(() => ({
    desde: toIsoDate(this.desde()),
    hasta: toIsoDate(this.hasta()),
    cuentaId: this.vista() === 'mayor' ? (this.cuentaId() ?? '') : '',
  }));

  ngOnInit(): void {
    this.api.cuentas(true).subscribe((c) => this.cuentas.set(c));
    this.consultar();
  }

  protected cambiarVista(vista: Vista): void {
    this.vista.set(vista);
    this.error.set(null);
    this.consultar();
  }

  protected consultar(): void {
    this.error.set(null);
    const desde = toIsoDate(this.desde());
    const hasta = toIsoDate(this.hasta());
    if (this.desde() > this.hasta() && this.vista() !== 'situacion') {
      this.error.set('La fecha inicial no puede ser posterior a la final.');
      return;
    }
    const fin = () => this.cargando.set(false);
    this.cargando.set(true);
    switch (this.vista()) {
      case 'balance':
        this.api.balanceComprobacion(desde, hasta).subscribe({
          next: (b) => {
            this.balance.set(b);
            fin();
          },
          error: fin,
        });
        break;
      case 'mayor': {
        const cuenta = this.cuentaId();
        if (!cuenta) {
          this.cargando.set(false);
          return;
        }
        this.api.mayor(cuenta, desde, hasta).subscribe({
          next: (m) => {
            this.mayor.set(m);
            fin();
          },
          error: fin,
        });
        break;
      }
      case 'situacion':
        this.api.estadoSituacion(hasta).subscribe({
          next: (s) => {
            this.situacion.set(s);
            fin();
          },
          error: fin,
        });
        break;
      case 'resultados':
        this.api.estadoResultados(desde, hasta).subscribe({
          next: (r) => {
            this.resultados.set(r);
            fin();
          },
          error: fin,
        });
        break;
    }
  }
}
