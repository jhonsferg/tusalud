import { CurrencyPipe, DatePipe } from '@angular/common';
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
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ArchivoPlanillaResponse,
  AuthService,
  BoletaPagoResponse,
  BoletaResumenResponse,
  ETIQUETA_ESTADO_PLANILLA,
  ETIQUETA_TIPO_CONCEPTO,
  NotificationService,
  PlanillaApiService,
  PlanillaDetalleResponse,
  toIsoDate,
} from '@tusalud/core';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { BotonExportar } from '@tusalud/ui/boton-exportar';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_PLANILLA } from '../planilla.estilos';
import { descargarTexto, etiquetaPeriodo } from '../planilla.utils';
import { severidadPlanilla } from './planillas';

@Component({
  selector: 'app-planilla-detalle',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    Dialog,
    InputText,
    TableModule,
    Tag,
    BotonDocumento,
    BotonExportar,
    EmptyState,
    PageHeader,
  ],
  template: `
    @if (detalle(); as d) {
      <app-page-header
        [title]="'Planilla ' + periodo()"
        [subtitle]="d.planilla.codigo"
        icon="pi-users"
      >
        <p-tag [value]="etiqueta(d.planilla.estado)" [severity]="severidad(d.planilla.estado)" />
        @if (puedeCalcular()) {
          <p-button
            label="Recalcular"
            icon="pi pi-refresh"
            [outlined]="true"
            (onClick)="recalcular()"
          />
        }
        @if (puedeAprobar()) {
          <p-button label="Aprobar" icon="pi pi-check" (onClick)="aprobar()" />
          <p-button
            label="Devolver"
            icon="pi pi-undo"
            severity="secondary"
            [outlined]="true"
            (onClick)="devolver()"
          />
        }
        @if (puedePagar()) {
          <p-button label="Registrar pago" icon="pi pi-wallet" (onClick)="abrirPago()" />
        }
        @if (puedeCerrar()) {
          <p-button
            label="Cerrar"
            icon="pi pi-lock"
            severity="secondary"
            [outlined]="true"
            (onClick)="cerrar()"
          />
        }
      </app-page-header>

      <section class="resumen">
        <div>
          <strong>{{ d.planilla.cantidadBoletas }}</strong>
          <span>Boletas</span>
        </div>
        <div>
          <strong>{{ d.planilla.totalIngresos | currency: 'PEN' : 'S/ ' }}</strong>
          <span>Total ingresos</span>
        </div>
        <div>
          <strong>{{ d.planilla.totalDescuentos | currency: 'PEN' : 'S/ ' }}</strong>
          <span>Total descuentos</span>
        </div>
        <div>
          <strong>{{ d.planilla.totalNeto | currency: 'PEN' : 'S/ ' }}</strong>
          <span>Neto a pagar</span>
        </div>
        <div>
          <strong>{{ d.planilla.totalAporteEmpleador | currency: 'PEN' : 'S/ ' }}</strong>
          <span>EsSalud del empleador</span>
        </div>
        <div>
          <strong>{{ d.planilla.totalRenta | currency: 'PEN' : 'S/ ' }}</strong>
          <span>Renta de quinta</span>
        </div>
      </section>

      <section class="panel">
        <dl class="ficha">
          <div>
            <dt>Calculada por</dt>
            <dd>{{ d.planilla.calculadaPor ?? 'Sin calcular' }}</dd>
          </div>
          <div>
            <dt>Aprobada por</dt>
            <dd>
              {{ d.planilla.aprobadaPor ?? 'Pendiente' }}
              @if (d.planilla.aprobadaEn) {
                <small class="block text-muted">{{
                  d.planilla.aprobadaEn | date: 'd MMM y HH:mm'
                }}</small>
              }
            </dd>
          </div>
          <div>
            <dt>Pagada por</dt>
            <dd>
              {{ d.planilla.pagadaPor ?? 'Pendiente' }}
              @if (d.planilla.referenciaPago) {
                <small class="block text-muted">{{ d.planilla.referenciaPago }}</small>
              }
            </dd>
          </div>
          <div>
            <dt>Huella de la planilla</dt>
            <dd class="huella">{{ d.planilla.hash ?? 'Sin huella' }}</dd>
          </div>
        </dl>
        <div class="acciones">
          @if (puedeVerBoletas()) {
            <app-boton-exportar
              tipo="reporte-planilla"
              formato="XLSX"
              [parametros]="{ planillaId: d.planilla.id }"
              etiqueta="Exportar a Excel"
            />
          }
          @if (esFinanzas() && aprobada()) {
            <p-button
              label="Archivo bancario"
              icon="pi pi-download"
              severity="secondary"
              [outlined]="true"
              (onClick)="descargarBancario()"
            />
          }
          @if (esRrhh() && aprobada()) {
            <p-button
              label="Archivo PLAME"
              icon="pi pi-download"
              severity="secondary"
              [outlined]="true"
              (onClick)="descargarPlame()"
            />
          }
        </div>
      </section>

      @if (puedeVerBoletas()) {
        <section class="panel">
          <h3 class="subseccion">Boletas de pago</h3>
          <p-table
            [value]="d.boletas"
            [rowHover]="true"
            [paginator]="d.boletas.length > 20"
            [rows]="20"
            responsiveLayout="scroll"
          >
            <ng-template #header>
              <tr>
                <th>Boleta</th>
                <th>Empleado</th>
                <th>Cargo</th>
                <th class="text-right">Ingresos</th>
                <th class="text-right">Descuentos</th>
                <th class="text-right">Neto</th>
                <th style="width: 13rem"></th>
              </tr>
            </ng-template>
            <ng-template #body let-b>
              <tr>
                <td>{{ b.numero }}</td>
                <td class="font-semibold">{{ b.empleadoNombre }}</td>
                <td>{{ b.cargo }}</td>
                <td class="text-right">{{ b.totalIngresos | currency: 'PEN' : 'S/ ' }}</td>
                <td class="text-right">{{ b.totalDescuentos | currency: 'PEN' : 'S/ ' }}</td>
                <td class="text-right negrita">{{ b.neto | currency: 'PEN' : 'S/ ' }}</td>
                <td class="derecha">
                  <p-button
                    icon="pi pi-eye"
                    [rounded]="true"
                    [text]="true"
                    [ariaLabel]="'Ver la boleta ' + b.numero"
                    (onClick)="verBoleta(b)"
                  />
                  <app-boton-documento
                    tipo="boleta-pago"
                    [registro]="b.id"
                    [soloIcono]="true"
                    [ariaEtiqueta]="'Descargar la boleta ' + b.numero"
                  />
                </td>
              </tr>
            </ng-template>
            <ng-template #emptymessage>
              <tr>
                <td colspan="7">
                  <app-empty-state
                    icon="pi-users"
                    title="Sin boletas"
                    message="La planilla aún no tiene boletas."
                  />
                </td>
              </tr>
            </ng-template>
          </p-table>
        </section>
      } @else {
        <p class="aviso">
          Gerencia ve los totales de la planilla. El detalle individual solo llega a recursos
          humanos y finanzas.
        </p>
      }
    }

    <p-dialog
      header="Registrar el pago de la planilla"
      [visible]="dialogoPago()"
      (visibleChange)="dialogoPago.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '28rem', maxWidth: '96vw' }"
    >
      <div class="formulario">
        <label for="pg-fecha"
          >Fecha de pago
          <p-datepicker
            inputId="pg-fecha"
            [ngModel]="fechaPago()"
            (ngModelChange)="fechaPago.set($event)"
            [maxDate]="hoy"
            dateFormat="dd/mm/yy"
            appendTo="body"
          />
        </label>
        <label for="pg-ref"
          >Referencia de la transferencia
          <input
            pInputText
            id="pg-ref"
            maxlength="60"
            [ngModel]="referencia()"
            (ngModelChange)="referencia.set($event)"
          />
        </label>
      </div>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoPago.set(false)"
        />
        <p-button
          label="Registrar pago"
          icon="pi pi-check"
          [loading]="ocupado()"
          [disabled]="!referencia().trim() || !fechaPago()"
          (onClick)="pagar()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="boleta() ? 'Boleta ' + boleta()?.numero : 'Boleta'"
      [visible]="!!boleta()"
      (visibleChange)="$event ? null : boleta.set(null)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '44rem', maxWidth: '96vw' }"
    >
      @if (boleta(); as b) {
        <dl class="ficha">
          <div>
            <dt>Trabajador</dt>
            <dd>{{ b.empleadoNombre }}</dd>
          </div>
          <div>
            <dt>Cargo</dt>
            <dd>{{ b.cargo }}</dd>
          </div>
          <div>
            <dt>Días laborados</dt>
            <dd>{{ b.diasLaborados }}</dd>
          </div>
          <div>
            <dt>Faltas</dt>
            <dd>{{ b.diasFalta }}</dd>
          </div>
          <div>
            <dt>Cuenta</dt>
            <dd>{{ b.banco ?? 'Sin banco' }} {{ b.cuentaEnmascarada }}</dd>
          </div>
        </dl>
        <p-table [value]="b.lineas" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Concepto</th>
              <th>Tipo</th>
              <th class="text-right">Base</th>
              <th class="text-right">Monto</th>
            </tr>
          </ng-template>
          <ng-template #body let-l>
            <tr>
              <td>{{ l.nombre }}</td>
              <td>{{ tipoConcepto(l.tipo) }}</td>
              <td class="text-right">
                @if (l.base !== null && l.base !== undefined) {
                  {{ l.base | currency: 'PEN' : 'S/ ' }}
                }
              </td>
              <td class="text-right">{{ l.monto | currency: 'PEN' : 'S/ ' }}</td>
            </tr>
          </ng-template>
        </p-table>
        <div class="cuadre">
          <span>Ingresos {{ b.totalIngresos | currency: 'PEN' : 'S/ ' }}</span>
          <span>Descuentos {{ b.totalDescuentos | currency: 'PEN' : 'S/ ' }}</span>
          <span class="ok">Neto {{ b.neto | currency: 'PEN' : 'S/ ' }}</span>
        </div>
        <p class="aviso">
          Provisiones del empleador, informativas y por validar con el contador: gratificación
          {{ b.provisionGratificacion | currency: 'PEN' : 'S/ ' }}, CTS
          {{ b.provisionCts | currency: 'PEN' : 'S/ ' }}, vacaciones
          {{ b.provisionVacaciones | currency: 'PEN' : 'S/ ' }}.
        </p>
      }
    </p-dialog>
  `,
  styles: [
    ESTILOS_PLANILLA,
    `
      .huella {
        font-family: monospace;
        font-size: 0.7rem;
        word-break: break-all;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanillaDetalle implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(PlanillaApiService);
  private readonly auth = inject(AuthService);
  private readonly notificaciones = inject(NotificationService);
  private readonly motivos = inject(MotivoService);
  private readonly confirmacion = inject(ConfirmationService);

  protected readonly hoy = new Date();
  protected readonly detalle = signal<PlanillaDetalleResponse | null>(null);
  protected readonly boleta = signal<BoletaPagoResponse | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly dialogoPago = signal(false);
  protected readonly fechaPago = signal<Date | null>(new Date());
  protected readonly referencia = signal('');
  protected readonly esRrhh = computed(() => this.auth.isRrhh());
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly estado = computed(() => this.detalle()?.planilla.estado ?? 'BORRADOR');
  protected readonly aprobada = computed(() =>
    ['APROBADA', 'PAGADA', 'CERRADA'].includes(this.estado()),
  );
  protected readonly puedeVerBoletas = computed(() => this.esRrhh() || this.esFinanzas());
  protected readonly puedeCalcular = computed(
    () => this.esRrhh() && (this.estado() === 'BORRADOR' || this.estado() === 'CALCULADA'),
  );
  protected readonly puedeAprobar = computed(
    () => this.esFinanzas() && this.estado() === 'CALCULADA',
  );
  protected readonly puedePagar = computed(() => this.esFinanzas() && this.estado() === 'APROBADA');
  protected readonly puedeCerrar = computed(
    () => (this.esRrhh() || this.auth.isGerencia()) && this.estado() === 'PAGADA',
  );
  protected readonly periodo = computed(() => {
    const p = this.detalle()?.planilla;
    return p ? etiquetaPeriodo(p.anio, p.mes) : '';
  });

  ngOnInit(): void {
    this.cargar();
  }

  protected etiqueta(estado: keyof typeof ETIQUETA_ESTADO_PLANILLA): string {
    return ETIQUETA_ESTADO_PLANILLA[estado];
  }

  protected severidad(estado: keyof typeof ETIQUETA_ESTADO_PLANILLA) {
    return severidadPlanilla(estado);
  }

  protected tipoConcepto(tipo: keyof typeof ETIQUETA_TIPO_CONCEPTO): string {
    return ETIQUETA_TIPO_CONCEPTO[tipo];
  }

  private cargar(): void {
    this.api.planillaDetalle(this.id()).subscribe((d) => this.detalle.set(d));
  }

  protected verBoleta(b: BoletaResumenResponse): void {
    this.api.boleta(b.id).subscribe((x) => this.boleta.set(x));
  }

  protected recalcular(): void {
    const p = this.detalle()?.planilla;
    if (!p) {
      return;
    }
    this.ocupado.set(true);
    this.api.calcular(p.anio, p.mes).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.notificaciones.success('La planilla se recalculó.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected aprobar(): void {
    this.confirmacion.confirm({
      header: 'Aprobar planilla',
      message:
        'Al aprobar se verifica la huella de cada boleta y se genera el asiento contable. Después ya no se podrá recalcular.',
      icon: 'pi pi-check',
      acceptLabel: 'Aprobar',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.api.aprobar(this.id()).subscribe(() => {
          this.notificaciones.success('La planilla fue aprobada.');
          this.cargar();
        }),
    });
  }

  protected devolver(): void {
    this.motivos
      .solicitar({
        titulo: 'Devolver la planilla',
        descripcion: 'Recursos humanos recibirá el motivo para corregir y volver a calcular.',
        etiqueta: 'Motivo de la devolución',
        confirmar: 'Devolver',
        severidad: 'danger',
        minimo: 5,
      })
      .subscribe((motivo) =>
        this.api.devolver(this.id(), motivo).subscribe(() => {
          this.notificaciones.success('La planilla volvió a borrador.');
          this.cargar();
        }),
      );
  }

  protected abrirPago(): void {
    this.referencia.set('');
    this.fechaPago.set(new Date());
    this.dialogoPago.set(true);
  }

  protected pagar(): void {
    const fecha = this.fechaPago();
    if (!fecha) {
      return;
    }
    this.ocupado.set(true);
    this.api.pagar(this.id(), toIsoDate(fecha), this.referencia().trim()).subscribe({
      next: () => {
        this.ocupado.set(false);
        this.dialogoPago.set(false);
        this.notificaciones.success('El pago quedó registrado y cada empleado fue avisado.');
        this.cargar();
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected cerrar(): void {
    this.confirmacion.confirm({
      header: 'Cerrar planilla',
      message: 'La planilla cerrada queda como definitiva.',
      icon: 'pi pi-lock',
      acceptLabel: 'Cerrar planilla',
      rejectLabel: 'Cancelar',
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.api.cerrar(this.id()).subscribe(() => {
          this.notificaciones.success('La planilla fue cerrada.');
          this.cargar();
        }),
    });
  }

  protected descargarBancario(): void {
    this.api.archivoBancario(this.id()).subscribe((a) => this.guardar(a));
  }

  protected descargarPlame(): void {
    this.api.plame(this.id()).subscribe((a) => {
      if (a.porValidar) {
        this.notificaciones.info('El formato PLAME está por validar con el contador.');
      }
      this.guardar(a);
    });
  }

  private guardar(a: ArchivoPlanillaResponse): void {
    descargarTexto(a.nombre, a.contenido);
    this.notificaciones.success(`Se descargó ${a.nombre} con ${a.filas} filas.`);
  }
}
