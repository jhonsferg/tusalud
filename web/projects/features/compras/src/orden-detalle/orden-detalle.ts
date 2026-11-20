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
import {
  FormBuilder,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AuthService,
  ComprasApiService,
  ComprasAuditoria,
  ETIQUETA_ESTADO_ORDEN_COMPRA,
  NotificationService,
  OrdenCompraDetalle,
  SEVERIDAD_ESTADO_ORDEN_COMPRA,
  toIsoDate,
} from '@tusalud/core';
import { BotonDocumento } from '@tusalud/ui/boton-documento';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

function lineaRecepcion(fb: NonNullableFormBuilder, ordenItemId: string, pendiente: number) {
  return fb.group({
    ordenItemId: [ordenItemId],
    cantidadRecibida: [pendiente, [Validators.min(0), Validators.max(pendiente)]],
    cantidadRechazada: [0, [Validators.min(0)]],
    numeroLote: [''],
    fechaVencimiento: [null as Date | null],
    ubicacion: [''],
    motivoDiscrepancia: [''],
  });
}

@Component({
  selector: 'app-orden-detalle',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    TableModule,
    Tag,
    BotonDocumento,
    PageHeader,
  ],
  template: `
    <app-page-header
      [title]="detalle()?.resumen?.codigo ?? 'Orden de compra en borrador'"
      subtitle="Detalle, aprobación y recepción de la orden"
      icon="pi-shopping-cart"
      backLink="/compras/ordenes"
      backLabel="Órdenes de compra"
    />

    @if (detalle(); as d) {
      <section class="panel">
        <dl class="ficha">
          <div>
            <dt>Estado</dt>
            <dd>
              <p-tag
                [value]="etiquetas[d.resumen.estado]"
                [severity]="severidades[d.resumen.estado]"
              />
            </dd>
          </div>
          <div>
            <dt>Proveedor</dt>
            <dd>
              {{ d.resumen.proveedorNombre
              }}<small class="block text-muted">RUC {{ d.proveedorRuc }}</small>
            </dd>
          </div>
          <div>
            <dt>Creada por</dt>
            <dd>
              {{ d.resumen.creadaPorNombre
              }}<small class="block text-muted">{{
                d.resumen.creadaEn | date: 'd MMM y, HH:mm'
              }}</small>
            </dd>
          </div>
          <div>
            <dt>Aprobación</dt>
            <dd>
              @if (d.aprobadaPorNombre) {
                {{ d.aprobadaPorNombre
                }}<small class="block text-muted">{{
                  d.resumen.aprobadaEn | date: 'd MMM y, HH:mm'
                }}</small>
              } @else if (d.resumen.nivelAprobacion) {
                Pendiente de
                {{ d.resumen.nivelAprobacion === 'GERENCIA' ? 'gerencia' : 'finanzas' }}
              } @else {
                Sin enviar
              }
            </dd>
          </div>
          <div>
            <dt>Pago</dt>
            <dd>{{ d.condicionPagoDias === 0 ? 'Contado' : d.condicionPagoDias + ' días' }}</dd>
          </div>
          <div>
            <dt>Entrega estimada</dt>
            <dd>
              {{
                d.fechaEntregaEstimada ? (d.fechaEntregaEstimada | date: 'd MMM y') : 'Sin fecha'
              }}
            </dd>
          </div>
          @if (d.resumen.requisicionCodigo) {
            <div>
              <dt>Requisición</dt>
              <dd>
                <a [routerLink]="['/compras/requisiciones', d.requisicionId]">{{
                  d.resumen.requisicionCodigo
                }}</a>
              </dd>
            </div>
          }
          @if (d.motivoResolucion) {
            <div>
              <dt>Resolución</dt>
              <dd>{{ d.motivoResolucion }}</dd>
            </div>
          }
          @if (d.observaciones) {
            <div>
              <dt>Observaciones</dt>
              <dd>{{ d.observaciones }}</dd>
            </div>
          }
        </dl>

        <p-table [value]="d.items" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Producto</th>
              <th class="text-right">Cantidad</th>
              <th class="text-right">Precio</th>
              <th class="text-right">Subtotal</th>
              <th class="text-right">Recibido</th>
              <th class="text-right">Rechazado</th>
              <th class="text-right">Pendiente</th>
            </tr>
          </ng-template>
          <ng-template #body let-i>
            <tr>
              <td>
                {{ i.productoNombre }}<small class="block text-muted">{{ i.productoCodigo }}</small>
              </td>
              <td class="text-right">{{ i.cantidad }} {{ i.unidad }}</td>
              <td class="text-right">{{ i.precioUnitario | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ i.subtotal | currency: 'PEN' : 'symbol-narrow' }}</td>
              <td class="text-right">{{ i.recibida }}</td>
              <td class="text-right">{{ i.rechazada }}</td>
              <td class="text-right">{{ i.pendiente }}</td>
            </tr>
          </ng-template>
        </p-table>

        <div class="totales">
          <div>
            <span>Subtotal</span><span>{{ d.subtotal | currency: 'PEN' : 'symbol-narrow' }}</span>
          </div>
          <div>
            <span>IGV 18 %</span><span>{{ d.igv | currency: 'PEN' : 'symbol-narrow' }}</span>
          </div>
          <div class="total">
            <span>Total</span><span>{{ d.total | currency: 'PEN' : 'symbol-narrow' }}</span>
          </div>
          @if (puedeVerFinanzas() && d.facturado > 0) {
            <div>
              <span>Facturado</span
              ><span>{{ d.facturado | currency: 'PEN' : 'symbol-narrow' }}</span>
            </div>
          }
        </div>

        @if (d.hash) {
          <p class="text-muted">
            <small>Huella de la orden aprobada: {{ d.hash }}</small>
          </p>
        }

        <div class="acciones">
          <app-boton-documento
            tipo="orden-compra"
            [registro]="d.resumen.id"
            etiqueta="Descargar PDF"
          />
          @if (esAlmacen() && d.resumen.estado === 'BORRADOR') {
            <p-button
              label="Editar"
              icon="pi pi-pencil"
              severity="secondary"
              [outlined]="true"
              [routerLink]="['editar']"
            />
            <p-button
              label="Enviar a aprobación"
              icon="pi pi-send"
              [loading]="ocupado()"
              (onClick)="enviar()"
            />
          }
          @if (esAprobador() && d.resumen.estado === 'PENDIENTE_APROBACION') {
            <p-button
              label="Aprobar orden"
              icon="pi pi-check"
              [loading]="ocupado()"
              [disabled]="!puedeAprobar()"
              (onClick)="aprobar()"
            />
            <p-button
              label="Rechazar"
              icon="pi pi-times"
              severity="danger"
              [outlined]="true"
              (onClick)="rechazar()"
            />
            @if (!puedeAprobar()) {
              <small class="text-muted"
                >Esta orden supera el límite de finanzas y la aprueba la gerencia.</small
              >
            }
          }
          @if (esAlmacen() && puedeRecibir()) {
            <p-button
              label="Registrar recepción"
              icon="pi pi-download"
              [loading]="ocupado()"
              (onClick)="abrirRecepcion()"
            />
          }
          @if (esAlmacen() && d.resumen.estado === 'PARCIAL') {
            <p-button
              label="Cerrar la orden"
              icon="pi pi-lock"
              severity="secondary"
              [outlined]="true"
              (onClick)="cerrar()"
            />
          }
          @if (puedeAnular()) {
            <p-button
              label="Anular"
              icon="pi pi-ban"
              severity="danger"
              [outlined]="true"
              (onClick)="anular()"
            />
          }
          @if (esFinanzas() && puedeFacturar()) {
            <p-button
              label="Registrar comprobante del proveedor"
              icon="pi pi-receipt"
              severity="secondary"
              [outlined]="true"
              [routerLink]="['/compras/cuentas-por-pagar']"
              [queryParams]="{ ordenId: d.resumen.id }"
            />
          }
        </div>
      </section>

      @if (d.recepciones.length > 0) {
        <section class="panel">
          <h3 class="subseccion">Recepciones</h3>
          <p-table [value]="d.recepciones" responsiveLayout="scroll">
            <ng-template #header>
              <tr>
                <th>Recepción</th>
                <th>Guía</th>
                <th>Recibida por</th>
                <th>Fecha</th>
                <th>Discrepancias</th>
              </tr>
            </ng-template>
            <ng-template #body let-r>
              <tr>
                <td>{{ r.codigo }}</td>
                <td>{{ r.guiaRemision ?? '—' }}</td>
                <td>{{ r.recibidaPorNombre }}</td>
                <td>{{ r.recibidaEn | date: 'd MMM y, HH:mm' }}</td>
                <td>
                  <p-tag
                    [value]="r.conDiscrepancia ? 'Con discrepancias' : 'Conforme'"
                    [severity]="r.conDiscrepancia ? 'warn' : 'success'"
                  />
                </td>
              </tr>
            </ng-template>
          </p-table>
        </section>
      }

      @if (auditoria().length > 0) {
        <section class="panel">
          <h3 class="subseccion">Historial de la orden</h3>
          <ul class="historial">
            @for (a of auditoria(); track a.id) {
              <li>
                <strong>{{ a.accion }}</strong>
                <span>{{ a.usuarioNombre }}</span>
                <span class="text-muted">{{ a.fecha | date: 'd MMM y, HH:mm' }}</span>
                @if (a.detalle) {
                  <span class="text-muted">{{ a.detalle }}</span>
                }
              </li>
            }
          </ul>
        </section>
      }
    }

    <p-dialog
      header="Registrar recepción"
      [visible]="dialogoRecepcion()"
      (visibleChange)="dialogoRecepcion.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '48rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formRecepcion"
        (submit)="$event.preventDefault(); guardarRecepcion()"
        novalidate
      >
        <div class="dos">
          <label for="rec-guia"
            >Guía de remisión
            <input
              pInputText
              id="rec-guia"
              formControlName="guiaRemision"
              maxlength="40"
              class="w-full"
            />
          </label>
          <label for="rec-obs"
            >Observaciones
            <input
              pInputText
              id="rec-obs"
              formControlName="observaciones"
              maxlength="300"
              class="w-full"
            />
          </label>
        </div>
        <div formArrayName="items">
          @for (fila of lineasRecepcion.controls; track fila; let i = $index) {
            <fieldset class="panel" [formGroupName]="i">
              <legend>
                {{ pendientes()[i]?.productoNombre }}, pendiente {{ pendientes()[i]?.pendiente }}
              </legend>
              <div class="dos">
                <label
                  >Cantidad recibida
                  <p-inputnumber
                    formControlName="cantidadRecibida"
                    [min]="0"
                    [useGrouping]="false"
                    [inputId]="'rec-recibida-' + i"
                    [ariaLabel]="'Cantidad recibida de ' + pendientes()[i]?.productoNombre"
                  />
                </label>
                <label
                  >Cantidad rechazada
                  <p-inputnumber
                    formControlName="cantidadRechazada"
                    [min]="0"
                    [useGrouping]="false"
                    [inputId]="'rec-rechazada-' + i"
                    [ariaLabel]="'Cantidad rechazada de ' + pendientes()[i]?.productoNombre"
                  />
                </label>
              </div>
              <div class="dos">
                <label
                  >Número de lote
                  <input
                    pInputText
                    formControlName="numeroLote"
                    maxlength="40"
                    class="w-full"
                    [attr.aria-label]="'Lote de ' + pendientes()[i]?.productoNombre"
                  />
                </label>
                <label
                  >Vencimiento
                  <p-datepicker
                    formControlName="fechaVencimiento"
                    dateFormat="dd/mm/yy"
                    [minDate]="manana"
                    appendTo="body"
                    [ariaLabel]="'Vencimiento de ' + pendientes()[i]?.productoNombre"
                  />
                </label>
              </div>
              <div class="dos">
                <label
                  >Ubicación
                  <input
                    pInputText
                    formControlName="ubicacion"
                    maxlength="60"
                    class="w-full"
                    [attr.aria-label]="'Ubicación de ' + pendientes()[i]?.productoNombre"
                  />
                </label>
                <label
                  >Motivo de la discrepancia
                  <input
                    pInputText
                    formControlName="motivoDiscrepancia"
                    maxlength="200"
                    class="w-full"
                    [attr.aria-label]="
                      'Motivo de discrepancia de ' + pendientes()[i]?.productoNombre
                    "
                  />
                </label>
              </div>
            </fieldset>
          }
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoRecepcion.set(false)"
        />
        <p-button
          label="Confirmar recepción"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarRecepcion()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdenDetalleVista implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(ComprasApiService);
  private readonly auth = inject(AuthService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly etiquetas = ETIQUETA_ESTADO_ORDEN_COMPRA;
  protected readonly severidades = SEVERIDAD_ESTADO_ORDEN_COMPRA;
  protected readonly manana = (() => {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 1);
    return fecha;
  })();

  protected readonly detalle = signal<OrdenCompraDetalle | null>(null);
  protected readonly auditoria = signal<ComprasAuditoria[]>([]);
  protected readonly ocupado = signal(false);
  protected readonly dialogoRecepcion = signal(false);
  protected readonly pendientes = signal<OrdenCompraDetalle['items']>([]);

  protected readonly esAlmacen = computed(() => this.auth.isAlmacen());
  protected readonly esFinanzas = computed(() => this.auth.isFinanzas());
  protected readonly esAprobador = computed(() => this.auth.isFinanzas() || this.auth.isGerencia());
  protected readonly puedeVerFinanzas = computed(
    () => this.auth.isFinanzas() || this.auth.isGerencia() || this.auth.isAdmin(),
  );
  protected readonly puedeAprobar = computed(() => {
    const nivel = this.detalle()?.resumen.nivelAprobacion;
    return nivel !== 'GERENCIA' || this.auth.isGerencia();
  });
  protected readonly puedeRecibir = computed(() => {
    const estado = this.detalle()?.resumen.estado;
    return estado === 'APROBADA' || estado === 'PARCIAL';
  });
  protected readonly puedeAnular = computed(() => {
    const d = this.detalle();
    if (!d) {
      return false;
    }
    const estado = d.resumen.estado;
    const sinRecepciones = d.recepciones.length === 0;
    const rol = this.auth.isAlmacen() || this.auth.isFinanzas() || this.auth.isGerencia();
    return (
      rol &&
      sinRecepciones &&
      (estado === 'BORRADOR' || estado === 'PENDIENTE_APROBACION' || estado === 'APROBADA')
    );
  });
  protected readonly puedeFacturar = computed(() => {
    const d = this.detalle();
    return !!d && d.resumen.estado !== 'APROBADA' ? d.recepciones.length > 0 : false;
  });

  protected readonly formRecepcion = this.fb.group({
    guiaRemision: [''],
    observaciones: [''],
    items: this.fb.array<ReturnType<typeof lineaRecepcion>>([]),
  });

  protected get lineasRecepcion() {
    return this.formRecepcion.controls.items;
  }

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.orden(this.id()).subscribe((d) => {
      this.detalle.set(d);
      if (this.auth.isFinanzas() || this.auth.isGerencia() || this.auth.isAdmin()) {
        this.api.auditoriaOrden(this.id()).subscribe((a) => this.auditoria.set(a));
      }
    });
  }

  private aplicar(d: OrdenCompraDetalle, mensaje: string): void {
    this.ocupado.set(false);
    this.detalle.set(d);
    this.notificaciones.success(mensaje);
    if (this.auth.isFinanzas() || this.auth.isGerencia() || this.auth.isAdmin()) {
      this.api.auditoriaOrden(this.id()).subscribe((a) => this.auditoria.set(a));
    }
  }

  protected enviar(): void {
    this.ocupado.set(true);
    this.api.enviarAprobacion(this.id()).subscribe({
      next: (d) => this.aplicar(d, 'La orden fue enviada a aprobación.'),
      error: () => this.ocupado.set(false),
    });
  }

  protected aprobar(): void {
    this.ocupado.set(true);
    this.api.aprobarOrden(this.id()).subscribe({
      next: (d) => this.aplicar(d, `La orden ${d.resumen.codigo} fue aprobada.`),
      error: () => this.ocupado.set(false),
    });
  }

  protected rechazar(): void {
    this.motivos
      .solicitar({
        titulo: 'Rechazar orden de compra',
        etiqueta: 'Motivo del rechazo',
        confirmar: 'Rechazar orden',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.api
          .rechazarOrden(this.id(), motivo)
          .subscribe((d) => this.aplicar(d, 'La orden fue rechazada.')),
      );
  }

  protected anular(): void {
    this.motivos
      .solicitar({
        titulo: 'Anular orden de compra',
        descripcion: 'Una orden anulada conserva su número y deja de poder recibirse.',
        etiqueta: 'Motivo de la anulación',
        confirmar: 'Anular orden',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.api
          .anularOrden(this.id(), motivo)
          .subscribe((d) => this.aplicar(d, 'La orden fue anulada.')),
      );
  }

  protected cerrar(): void {
    this.motivos
      .solicitar({
        titulo: 'Cerrar orden de compra',
        descripcion: 'No se esperan más entregas de lo pendiente.',
        etiqueta: 'Motivo del cierre',
        confirmar: 'Cerrar orden',
        severidad: 'primary',
      })
      .subscribe((motivo) =>
        this.api
          .cerrarOrden(this.id(), motivo)
          .subscribe((d) => this.aplicar(d, 'La orden fue cerrada.')),
      );
  }

  protected abrirRecepcion(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    const pendientes = d.items.filter((i) => i.pendiente > 0);
    this.pendientes.set(pendientes);
    this.formRecepcion.reset({ guiaRemision: '', observaciones: '' });
    this.lineasRecepcion.clear();
    for (const item of pendientes) {
      this.lineasRecepcion.push(lineaRecepcion(this.fb, item.id, item.pendiente));
    }
    this.dialogoRecepcion.set(true);
  }

  protected guardarRecepcion(): void {
    const v = this.formRecepcion.getRawValue();
    const items = v.items
      .filter((i) => (i.cantidadRecibida ?? 0) + (i.cantidadRechazada ?? 0) > 0)
      .map((i) => ({
        ordenItemId: i.ordenItemId as string,
        cantidadRecibida: i.cantidadRecibida ?? 0,
        cantidadRechazada: i.cantidadRechazada ?? 0,
        numeroLote: (i.numeroLote ?? '').trim() || null,
        fechaVencimiento: i.fechaVencimiento ? toIsoDate(i.fechaVencimiento as Date) : null,
        ubicacion: (i.ubicacion ?? '').trim() || null,
        motivoDiscrepancia: (i.motivoDiscrepancia ?? '').trim() || null,
      }));
    if (items.length === 0) {
      this.notificaciones.warning('Indica al menos una cantidad recibida o rechazada.');
      return;
    }
    this.ocupado.set(true);
    this.api
      .recibir(this.id(), {
        guiaRemision: v.guiaRemision.trim() || null,
        observaciones: v.observaciones.trim() || null,
        items,
      })
      .subscribe({
        next: (r) => {
          this.dialogoRecepcion.set(false);
          this.ocupado.set(false);
          this.notificaciones.success(`Se registró la recepción ${r.resumen.codigo}.`);
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected volver(): void {
    void this.router.navigate(['/compras/ordenes']);
  }
}
