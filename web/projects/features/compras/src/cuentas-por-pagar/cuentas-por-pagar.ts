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
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AlmacenApiService,
  AuthService,
  ComprasApiService,
  ComprobanteProveedorResponse,
  DevolucionResponse,
  ETIQUETA_ESTADO_COMPROBANTE_PROVEEDOR,
  ETIQUETA_METODO_PAGO_PROVEEDOR,
  ETIQUETA_TIPO_COMPROBANTE_PROVEEDOR,
  EstadoComprobanteProveedor,
  NotificationService,
  OrdenCompraResumen,
  ProveedorResponse,
  ResumenCuentasPorPagar,
  SEVERIDAD_ESTADO_COMPROBANTE_PROVEEDOR,
  toIsoDate,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';
import { importe } from '../compras.utils';

@Component({
  selector: 'app-cuentas-por-pagar',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      title="Cuentas por pagar"
      subtitle="Comprobantes de proveedores, vencimientos y pagos"
      icon="pi-wallet"
    >
      @if (esFinanzas()) {
        <p-button label="Registrar comprobante" icon="pi pi-plus" (onClick)="abrirRegistro()" />
      }
    </app-page-header>

    @if (resumen(); as r) {
      <div class="resumen" aria-label="Resumen de cuentas por pagar">
        <div>
          <strong>{{ r.totalPendiente | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Por pagar, {{ r.cantidadPendientes }} comprobantes</span>
        </div>
        <div [class.alerta]="r.cantidadVencidas > 0">
          <strong>{{ r.totalVencido | currency: 'PEN' : 'symbol-narrow' }}</strong>
          <span>Vencido, {{ r.cantidadVencidas }} comprobantes</span>
        </div>
        @for (t of r.tramos; track t.etiqueta) {
          <div>
            <strong>{{ t.monto | currency: 'PEN' : 'symbol-narrow' }}</strong>
            <span>{{ t.etiqueta }}</span>
          </div>
        }
      </div>
    }

    <section class="panel">
      <div class="filtros">
        <p-select
          [options]="opcionesEstado"
          optionLabel="label"
          optionValue="value"
          [ngModel]="estado()"
          (ngModelChange)="estado.set($event); cargar()"
          placeholder="Todos los estados"
          [showClear]="true"
          ariaLabel="Filtrar por estado"
          appendTo="body"
          styleClass="w-full md:w-14rem"
        />
        <p-button
          label="Solo vencidos"
          icon="pi pi-exclamation-circle"
          size="small"
          [outlined]="!soloVencidos()"
          severity="danger"
          (onClick)="soloVencidos.set(!soloVencidos()); cargar()"
        />
      </div>
      <p-table
        [value]="lista()"
        [loading]="cargando()"
        [paginator]="lista().length > 12"
        [rows]="12"
        [rowHover]="true"
        responsiveLayout="scroll"
      >
        <ng-template #header>
          <tr>
            <th>Comprobante</th>
            <th>Proveedor</th>
            <th>Vence</th>
            <th class="text-right">Total</th>
            <th class="text-right">Saldo</th>
            <th>Estado</th>
            <th style="width: 16rem"></th>
          </tr>
        </ng-template>
        <ng-template #body let-c>
          <tr>
            <td>
              <span class="font-semibold">{{ c.numero }}</span>
              <small class="block text-muted"
                >{{ tipos[c.tipo] }}{{ c.ordenCodigo ? ' · ' + c.ordenCodigo : '' }}</small
              >
            </td>
            <td>
              {{ c.proveedorNombre }}<small class="block text-muted">{{ c.proveedorRuc }}</small>
            </td>
            <td>
              {{ c.fechaVencimiento | date: 'd MMM y' }}
              @if (c.vencido) {
                <small class="block" style="color: var(--p-red-600)"
                  >{{ c.diasVencido }} días de atraso</small
                >
              }
            </td>
            <td class="text-right">{{ c.total | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td class="text-right">{{ c.saldo | currency: 'PEN' : 'symbol-narrow' }}</td>
            <td><p-tag [value]="etiquetas[c.estado]" [severity]="severidades[c.estado]" /></td>
            <td class="derecha">
              <p-button
                icon="pi pi-eye"
                size="small"
                [text]="true"
                [rounded]="true"
                [ariaLabel]="'Ver los pagos de ' + c.numero"
                (onClick)="verPagos(c)"
              />
              @if (esFinanzas() && (c.estado === 'PENDIENTE' || c.estado === 'PARCIAL')) {
                <p-button
                  label="Pagar"
                  icon="pi pi-money-bill"
                  size="small"
                  [ariaLabel]="'Registrar un pago de ' + c.numero"
                  (onClick)="abrirPago(c)"
                />
                <p-button
                  icon="pi pi-percentage"
                  size="small"
                  severity="secondary"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Aplicar el crédito de una devolución a ' + c.numero"
                  (onClick)="abrirCredito(c)"
                />
              }
              @if (
                esFinanzas() &&
                c.estado === 'PENDIENTE' &&
                c.montoPagado === 0 &&
                c.montoCredito === 0
              ) {
                <p-button
                  icon="pi pi-ban"
                  size="small"
                  severity="danger"
                  [text]="true"
                  [rounded]="true"
                  [ariaLabel]="'Anular el comprobante ' + c.numero"
                  (onClick)="anular(c)"
                />
              }
            </td>
          </tr>
        </ng-template>
        <ng-template #emptymessage>
          <tr>
            <td colspan="7">
              <app-empty-state
                icon="pi-wallet"
                title="Sin comprobantes"
                message="No hay comprobantes de proveedores con estos filtros."
              />
            </td>
          </tr>
        </ng-template>
      </p-table>
    </section>

    @if (resumen(); as r) {
      @if (r.porProveedor.length > 0) {
        <section class="panel">
          <h3 class="subseccion">Saldo por proveedor</h3>
          <p-table [value]="r.porProveedor" responsiveLayout="scroll">
            <ng-template #header>
              <tr>
                <th>Proveedor</th>
                <th class="text-right">Comprobantes</th>
                <th class="text-right">Saldo</th>
                <th class="text-right">Vencido</th>
              </tr>
            </ng-template>
            <ng-template #body let-p>
              <tr>
                <td>{{ p.proveedorNombre }}</td>
                <td class="text-right">{{ p.comprobantes }}</td>
                <td class="text-right">{{ p.saldo | currency: 'PEN' : 'symbol-narrow' }}</td>
                <td class="text-right">{{ p.vencido | currency: 'PEN' : 'symbol-narrow' }}</td>
              </tr>
            </ng-template>
          </p-table>
        </section>
      }
    }

    <p-dialog
      [header]="'Pagos de ' + (detallePagos()?.numero ?? '')"
      [visible]="dialogoPagos()"
      (visibleChange)="dialogoPagos.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      @if (detallePagos(); as c) {
        <dl class="ficha">
          <div>
            <dt>Total</dt>
            <dd>{{ c.total | currency: 'PEN' : 'symbol-narrow' }}</dd>
          </div>
          <div>
            <dt>Pagado</dt>
            <dd>{{ c.montoPagado | currency: 'PEN' : 'symbol-narrow' }}</dd>
          </div>
          <div>
            <dt>Crédito por devoluciones</dt>
            <dd>{{ c.montoCredito | currency: 'PEN' : 'symbol-narrow' }}</dd>
          </div>
          <div>
            <dt>Saldo</dt>
            <dd>{{ c.saldo | currency: 'PEN' : 'symbol-narrow' }}</dd>
          </div>
        </dl>
        @if (c.pagos.length === 0) {
          <p class="text-muted">Todavía no se registraron pagos.</p>
        } @else {
          <ul class="historial">
            @for (p of c.pagos; track p.id) {
              <li>
                <strong>{{ p.monto | currency: 'PEN' : 'symbol-narrow' }}</strong>
                <span>{{ metodos[p.metodo] }}{{ p.referencia ? ' · ' + p.referencia : '' }}</span>
                <span class="text-muted"
                  >{{ p.fecha | date: 'd MMM y' }} · {{ p.registradoPorNombre }}</span
                >
              </li>
            }
          </ul>
        }
      }
    </p-dialog>

    <p-dialog
      [header]="'Registrar pago de ' + (comprobanteActivo()?.numero ?? '')"
      [visible]="dialogoPago()"
      (visibleChange)="dialogoPago.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '30rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formPago"
        (submit)="$event.preventDefault(); guardarPago()"
        novalidate
      >
        <label for="pago-monto"
          >Monto, saldo de {{ comprobanteActivo()?.saldo | currency: 'PEN' : 'symbol-narrow' }}
          <p-inputnumber
            inputId="pago-monto"
            formControlName="monto"
            mode="currency"
            currency="PEN"
            locale="es-PE"
            [min]="0.01"
          />
        </label>
        <label for="pago-metodo"
          >Método de pago
          <p-select
            inputId="pago-metodo"
            formControlName="metodo"
            [options]="opcionesMetodo"
            optionLabel="label"
            optionValue="value"
            appendTo="body"
          />
        </label>
        <label for="pago-ref"
          >Referencia de la operación
          <input
            pInputText
            id="pago-ref"
            formControlName="referencia"
            maxlength="60"
            class="w-full"
          />
        </label>
        <label for="pago-fecha"
          >Fecha del pago
          <p-datepicker
            inputId="pago-fecha"
            formControlName="fecha"
            dateFormat="dd/mm/yy"
            [maxDate]="hoy"
            appendTo="body"
          />
        </label>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoPago.set(false)"
        />
        <p-button
          label="Confirmar pago"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarPago()"
        />
      </ng-template>
    </p-dialog>

    <p-dialog
      [header]="'Aplicar crédito a ' + (comprobanteActivo()?.numero ?? '')"
      [visible]="dialogoCredito()"
      (visibleChange)="dialogoCredito.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '34rem', maxWidth: '96vw' }"
    >
      @if (devolucionesSinCredito().length === 0) {
        <p class="text-muted">No hay devoluciones de este proveedor pendientes de aplicar.</p>
      } @else {
        <ul class="historial">
          @for (d of devolucionesSinCredito(); track d.id) {
            <li>
              <strong>{{ d.codigo }}</strong>
              <span>{{ d.monto | currency: 'PEN' : 'symbol-narrow' }} · {{ d.motivo }}</span>
              <p-button
                label="Aplicar"
                size="small"
                [outlined]="true"
                [ariaLabel]="'Aplicar la devolución ' + d.codigo"
                (onClick)="aplicarCredito(d)"
              />
            </li>
          }
        </ul>
      }
    </p-dialog>

    <p-dialog
      header="Registrar comprobante del proveedor"
      [visible]="dialogoRegistro()"
      (visibleChange)="dialogoRegistro.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '42rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formRegistro"
        (submit)="$event.preventDefault(); guardarRegistro()"
        novalidate
      >
        <label for="cp-proveedor"
          >Proveedor
          <p-select
            inputId="cp-proveedor"
            formControlName="proveedorId"
            [options]="proveedores()"
            optionLabel="razonSocial"
            optionValue="id"
            [filter]="true"
            filterBy="razonSocial,ruc"
            placeholder="Elige un proveedor"
            appendTo="body"
            (onChange)="alElegirProveedor($event.value)"
          />
        </label>
        <label for="cp-orden"
          >Orden de compra relacionada
          <p-select
            inputId="cp-orden"
            formControlName="ordenId"
            [options]="ordenesProveedor()"
            optionLabel="codigo"
            optionValue="id"
            [showClear]="true"
            placeholder="Sin orden de compra"
            appendTo="body"
          />
        </label>
        <div class="dos">
          <label for="cp-tipo"
            >Tipo
            <p-select
              inputId="cp-tipo"
              formControlName="tipo"
              [options]="opcionesTipo"
              optionLabel="label"
              optionValue="value"
              appendTo="body"
            />
          </label>
          <label for="cp-ruc"
            >RUC del emisor
            <input
              pInputText
              id="cp-ruc"
              formControlName="rucEmisor"
              maxlength="11"
              class="w-full"
              inputmode="numeric"
            />
          </label>
        </div>
        <div class="dos">
          <label for="cp-serie"
            >Serie
            <input pInputText id="cp-serie" formControlName="serie" maxlength="10" class="w-full" />
          </label>
          <label for="cp-numero"
            >Número
            <input
              pInputText
              id="cp-numero"
              formControlName="numero"
              maxlength="20"
              class="w-full"
              inputmode="numeric"
            />
          </label>
        </div>
        <div class="dos">
          <label for="cp-emision"
            >Fecha de emisión
            <p-datepicker
              inputId="cp-emision"
              formControlName="fechaEmision"
              dateFormat="dd/mm/yy"
              [maxDate]="hoy"
              appendTo="body"
            />
          </label>
          <label for="cp-vence"
            >Fecha de vencimiento
            <p-datepicker
              inputId="cp-vence"
              formControlName="fechaVencimiento"
              dateFormat="dd/mm/yy"
              appendTo="body"
            />
          </label>
        </div>
        <div class="dos">
          <label for="cp-subtotal"
            >Subtotal sin IGV
            <p-inputnumber
              inputId="cp-subtotal"
              formControlName="subtotal"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0.01"
              (onInput)="calcularIgv()"
            />
          </label>
          <label for="cp-igv"
            >IGV
            <p-inputnumber
              inputId="cp-igv"
              formControlName="igv"
              mode="currency"
              currency="PEN"
              locale="es-PE"
              [min]="0"
            />
          </label>
        </div>
        <label for="cp-obs"
          >Observaciones
          <input
            pInputText
            id="cp-obs"
            formControlName="observaciones"
            maxlength="300"
            class="w-full"
          />
        </label>
        @if (errorRegistro(); as mensaje) {
          <p class="text-red-600" role="alert">{{ mensaje }}</p>
        }
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoRegistro.set(false)"
        />
        <p-button
          label="Registrar comprobante"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarRegistro()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CuentasPorPagar implements OnInit {
  readonly ordenId = input<string>();

  private readonly api = inject(ComprasApiService);
  private readonly almacen = inject(AlmacenApiService);
  private readonly auth = inject(AuthService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly etiquetas: Record<string, string> = ETIQUETA_ESTADO_COMPROBANTE_PROVEEDOR;
  protected readonly severidades: Record<
    string,
    'success' | 'info' | 'warn' | 'danger' | 'secondary'
  > = SEVERIDAD_ESTADO_COMPROBANTE_PROVEEDOR;
  protected readonly tipos: Record<string, string> = ETIQUETA_TIPO_COMPROBANTE_PROVEEDOR;
  protected readonly metodos: Record<string, string> = ETIQUETA_METODO_PAGO_PROVEEDOR;
  protected readonly opcionesEstado = Object.entries(ETIQUETA_ESTADO_COMPROBANTE_PROVEEDOR).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly opcionesMetodo = Object.entries(ETIQUETA_METODO_PAGO_PROVEEDOR).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly opcionesTipo = Object.entries(ETIQUETA_TIPO_COMPROBANTE_PROVEEDOR).map(
    ([value, label]) => ({ label, value }),
  );
  protected readonly hoy = new Date();

  protected readonly lista = signal<ComprobanteProveedorResponse[]>([]);
  protected readonly resumen = signal<ResumenCuentasPorPagar | null>(null);
  protected readonly proveedores = signal<ProveedorResponse[]>([]);
  protected readonly ordenesProveedor = signal<OrdenCompraResumen[]>([]);
  protected readonly devolucionesSinCredito = signal<DevolucionResponse[]>([]);
  protected readonly comprobanteActivo = signal<ComprobanteProveedorResponse | null>(null);
  protected readonly detallePagos = signal<ComprobanteProveedorResponse | null>(null);
  protected readonly cargando = signal(false);
  protected readonly ocupado = signal(false);
  protected readonly estado = signal<EstadoComprobanteProveedor | null>(null);
  protected readonly soloVencidos = signal(false);
  protected readonly dialogoPagos = signal(false);
  protected readonly dialogoPago = signal(false);
  protected readonly dialogoCredito = signal(false);
  protected readonly dialogoRegistro = signal(false);
  protected readonly errorRegistro = signal<string | null>(null);
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());

  protected readonly formPago = this.fb.group({
    monto: [0, [Validators.required, Validators.min(0.01)]],
    metodo: ['TRANSFERENCIA', [Validators.required]],
    referencia: [''],
    fecha: [new Date() as Date | null],
  });

  protected readonly formRegistro = this.fb.group({
    proveedorId: ['', [Validators.required]],
    ordenId: [null as string | null],
    tipo: ['FACTURA', [Validators.required]],
    rucEmisor: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    serie: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9]{1,10}$/)]],
    numero: ['', [Validators.required, Validators.pattern(/^\d{1,20}$/)]],
    fechaEmision: [new Date() as Date | null, [Validators.required]],
    fechaVencimiento: [null as Date | null],
    subtotal: [null as number | null, [Validators.required, Validators.min(0.01)]],
    igv: [0],
    observaciones: [''],
  });

  ngOnInit(): void {
    this.cargar();
    if (this.esFinanzas()) {
      this.almacen.listarProveedores({ activo: true }).subscribe((l) => {
        this.proveedores.set(l);
        const orden = this.ordenId();
        if (orden) {
          this.abrirRegistroDeOrden(orden);
        }
      });
    }
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.comprobantes({ estado: this.estado(), soloVencidos: this.soloVencidos() }).subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
    this.api.resumenCuentasPorPagar().subscribe((r) => this.resumen.set(r));
  }

  protected verPagos(c: ComprobanteProveedorResponse): void {
    this.api.comprobante(c.id).subscribe((detalle) => {
      this.detallePagos.set(detalle);
      this.dialogoPagos.set(true);
    });
  }

  protected abrirPago(c: ComprobanteProveedorResponse): void {
    this.comprobanteActivo.set(c);
    this.formPago.reset({
      monto: c.saldo,
      metodo: 'TRANSFERENCIA',
      referencia: '',
      fecha: new Date(),
    });
    this.dialogoPago.set(true);
  }

  protected guardarPago(): void {
    const c = this.comprobanteActivo();
    if (!c || this.formPago.invalid) {
      this.formPago.markAllAsTouched();
      return;
    }
    const v = this.formPago.getRawValue();
    this.ocupado.set(true);
    this.api
      .pagar(c.id, {
        monto: v.monto,
        metodo: v.metodo as 'TRANSFERENCIA',
        referencia: v.referencia.trim() || null,
        fecha: v.fecha ? toIsoDate(v.fecha) : null,
      })
      .subscribe({
        next: (r) => {
          this.ocupado.set(false);
          this.dialogoPago.set(false);
          this.notificaciones.success(
            r.estado === 'PAGADO'
              ? `El comprobante ${r.numero} quedó pagado.`
              : 'El pago fue registrado.',
          );
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected anular(c: ComprobanteProveedorResponse): void {
    this.motivos
      .solicitar({
        titulo: 'Anular comprobante del proveedor',
        descripcion: `Se anulará el comprobante ${c.numero}. Solo se anula si no tiene pagos ni créditos.`,
        etiqueta: 'Motivo de la anulación',
        confirmar: 'Anular comprobante',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.api.anularComprobante(c.id, motivo).subscribe(() => {
          this.notificaciones.success('El comprobante fue anulado.');
          this.cargar();
        }),
      );
  }

  protected abrirCredito(c: ComprobanteProveedorResponse): void {
    this.comprobanteActivo.set(c);
    this.api.devoluciones(c.proveedorId).subscribe((lista) => {
      this.devolucionesSinCredito.set(lista.filter((d) => !d.comprobanteId));
      this.dialogoCredito.set(true);
    });
  }

  protected aplicarCredito(d: DevolucionResponse): void {
    const c = this.comprobanteActivo();
    if (!c) {
      return;
    }
    this.api.aplicarCredito(d.id, c.id).subscribe(() => {
      this.dialogoCredito.set(false);
      this.notificaciones.success(
        `Se aplicó la devolución ${d.codigo} al comprobante ${c.numero}.`,
      );
      this.cargar();
    });
  }

  protected abrirRegistro(): void {
    this.errorRegistro.set(null);
    this.formRegistro.reset({
      proveedorId: '',
      ordenId: null,
      tipo: 'FACTURA',
      rucEmisor: '',
      serie: '',
      numero: '',
      fechaEmision: new Date(),
      fechaVencimiento: null,
      subtotal: null,
      igv: 0,
      observaciones: '',
    });
    this.ordenesProveedor.set([]);
    this.dialogoRegistro.set(true);
  }

  private abrirRegistroDeOrden(ordenId: string): void {
    this.api.orden(ordenId).subscribe((orden) => {
      this.abrirRegistro();
      this.formRegistro.patchValue({
        proveedorId: orden.proveedorId,
        rucEmisor: orden.proveedorRuc,
      });
      this.cargarOrdenes(orden.proveedorId, ordenId);
    });
  }

  protected alElegirProveedor(proveedorId: string): void {
    const proveedor = this.proveedores().find((p) => p.id === proveedorId);
    this.formRegistro.patchValue({ rucEmisor: proveedor?.ruc ?? '', ordenId: null });
    this.cargarOrdenes(proveedorId, null);
  }

  private cargarOrdenes(proveedorId: string, seleccionar: string | null): void {
    this.api.ordenes({ proveedorId }).subscribe((lista) => {
      this.ordenesProveedor.set(
        lista.filter((o) => ['APROBADA', 'PARCIAL', 'RECIBIDA', 'CERRADA'].includes(o.estado)),
      );
      if (seleccionar) {
        this.formRegistro.patchValue({ ordenId: seleccionar });
      }
    });
  }

  protected calcularIgv(): void {
    const subtotal = this.formRegistro.controls.subtotal.value ?? 0;
    this.formRegistro.controls.igv.setValue(importe(subtotal * 0.18));
  }

  protected guardarRegistro(): void {
    this.errorRegistro.set(null);
    if (this.formRegistro.invalid) {
      this.formRegistro.markAllAsTouched();
      this.errorRegistro.set(
        'Revisa el proveedor, el RUC de 11 dígitos, la serie, el número y el subtotal.',
      );
      return;
    }
    const v = this.formRegistro.getRawValue();
    this.ocupado.set(true);
    this.api
      .registrarComprobante({
        proveedorId: v.proveedorId,
        ordenId: v.ordenId,
        tipo: v.tipo as 'FACTURA',
        serie: v.serie.trim(),
        numero: v.numero.trim(),
        rucEmisor: v.rucEmisor.trim(),
        fechaEmision: toIsoDate(v.fechaEmision as Date),
        fechaVencimiento: v.fechaVencimiento ? toIsoDate(v.fechaVencimiento) : null,
        subtotal: v.subtotal ?? 0,
        igv: v.igv,
        observaciones: v.observaciones.trim() || null,
      })
      .subscribe({
        next: (c) => {
          this.ocupado.set(false);
          this.dialogoRegistro.set(false);
          this.notificaciones.success(`Se registró el comprobante ${c.numero}.`);
          this.cargar();
        },
        error: (e: { message?: string }) => {
          this.ocupado.set(false);
          this.errorRegistro.set(e.message ?? 'No se pudo registrar el comprobante.');
        },
      });
  }
}
