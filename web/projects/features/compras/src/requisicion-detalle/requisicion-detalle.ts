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
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  AlmacenApiService,
  AuthService,
  ComparativaResponse,
  ComprasApiService,
  CotizacionResponse,
  ETIQUETA_ESTADO_ORDEN_COMPRA,
  ETIQUETA_ESTADO_REQUISICION,
  NotificationService,
  ProveedorResponse,
  RequisicionDetalle,
  SEVERIDAD_ESTADO_ORDEN_COMPRA,
  SEVERIDAD_ESTADO_REQUISICION,
  toIsoDate,
} from '@tusalud/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';

@Component({
  selector: 'app-requisicion-detalle',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    Select,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  template: `
    <app-page-header
      [title]="detalle()?.resumen?.codigo ?? 'Requisición'"
      subtitle="Detalle de la requisición y sus cotizaciones"
      icon="pi-clipboard"
      backLink="/compras/requisiciones"
      backLabel="Requisiciones"
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
            <dt>Solicitante</dt>
            <dd>{{ d.resumen.solicitanteNombre }}</dd>
          </div>
          <div>
            <dt>Prioridad</dt>
            <dd>{{ d.resumen.prioridad === 'URGENTE' ? 'Urgente' : 'Normal' }}</dd>
          </div>
          <div>
            <dt>Creada</dt>
            <dd>{{ d.resumen.creadaEn | date: 'd MMM y, HH:mm' }}</dd>
          </div>
          @if (d.resumen.motivo) {
            <div>
              <dt>Motivo</dt>
              <dd>{{ d.resumen.motivo }}</dd>
            </div>
          }
          @if (d.motivoResolucion) {
            <div>
              <dt>Resolución</dt>
              <dd>{{ d.motivoResolucion }} · {{ d.resueltaPorNombre }}</dd>
            </div>
          }
        </dl>

        <p-table [value]="d.items" responsiveLayout="scroll">
          <ng-template #header>
            <tr>
              <th>Producto</th>
              <th class="text-right">Cantidad</th>
              <th class="text-right">Stock actual</th>
              <th class="text-right">Stock mínimo</th>
            </tr>
          </ng-template>
          <ng-template #body let-i>
            <tr>
              <td>
                {{ i.productoNombre }}
                <small class="block text-muted">{{ i.productoCodigo }}</small>
              </td>
              <td class="text-right">{{ i.cantidad }} {{ i.unidad }}</td>
              <td class="text-right">{{ i.stockActual }}</td>
              <td class="text-right">{{ i.stockMinimo }}</td>
            </tr>
          </ng-template>
        </p-table>

        <div class="acciones">
          @if (esSolicitante() && d.resumen.estado === 'BORRADOR') {
            <p-button
              label="Editar"
              icon="pi pi-pencil"
              severity="secondary"
              [outlined]="true"
              [routerLink]="['editar']"
            />
            <p-button
              label="Enviar requisición"
              icon="pi pi-send"
              [loading]="ocupado()"
              (onClick)="enviar()"
            />
          }
          @if (esSolicitante() && puedeAnular()) {
            <p-button
              label="Anular"
              icon="pi pi-ban"
              severity="danger"
              [outlined]="true"
              (onClick)="anular()"
            />
          }
          @if (puedeRechazar()) {
            <p-button
              label="Rechazar"
              icon="pi pi-times"
              severity="danger"
              [outlined]="true"
              (onClick)="rechazar()"
            />
          }
        </div>
      </section>

      @if (d.ordenes.length > 0) {
        <section class="panel">
          <h3 class="subseccion">Órdenes de compra</h3>
          <ul class="historial">
            @for (o of d.ordenes; track o.id) {
              <li>
                <a [routerLink]="['/compras/ordenes', o.id]">{{ o.codigo }}</a>
                <p-tag [value]="etiquetasOrden[o.estado]" [severity]="severidadesOrden[o.estado]" />
                <span>{{ o.total | currency: 'PEN' : 'symbol-narrow' }}</span>
              </li>
            }
          </ul>
        </section>
      }

      @if (puedeCotizar()) {
        <section class="panel">
          <div class="filtros">
            <h3 class="subseccion">Cotizaciones</h3>
            @if (esAlmacen() && admiteCotizar()) {
              <p-button
                label="Registrar cotización"
                icon="pi pi-plus"
                size="small"
                (onClick)="abrirCotizacion()"
              />
              <p-button
                label="Orden sin cotización"
                icon="pi pi-shopping-cart"
                size="small"
                severity="secondary"
                [outlined]="true"
                [routerLink]="['/compras/ordenes/nueva']"
                [queryParams]="{ requisicionId: d.resumen.id }"
              />
            }
          </div>
          @if (cotizaciones().length === 0) {
            <app-empty-state
              icon="pi-dollar"
              title="Sin cotizaciones"
              message="Registra al menos dos cotizaciones para compararlas."
            />
          } @else {
            <p-table [value]="cotizaciones()" responsiveLayout="scroll">
              <ng-template #header>
                <tr>
                  <th>Proveedor</th>
                  <th class="text-right">Total</th>
                  <th>Entrega</th>
                  <th>Pago</th>
                  <th>Estado</th>
                  <th></th>
                </tr>
              </ng-template>
              <ng-template #body let-c>
                <tr>
                  <td>
                    {{ c.proveedorNombre }}
                    <small class="block text-muted">{{ c.proveedorRuc }}</small>
                  </td>
                  <td class="text-right">{{ c.total | currency: 'PEN' : 'symbol-narrow' }}</td>
                  <td>{{ c.plazoEntregaDias ?? '—' }} días</td>
                  <td>
                    {{ c.condicionPagoDias === 0 ? 'Contado' : c.condicionPagoDias + ' días' }}
                  </td>
                  <td>
                    <p-tag
                      [value]="
                        c.estado === 'SELECCIONADA'
                          ? 'Seleccionada'
                          : c.estado === 'DESCARTADA'
                            ? 'Descartada'
                            : c.vencida
                              ? 'Vencida'
                              : 'Vigente'
                      "
                      [severity]="
                        c.estado === 'SELECCIONADA'
                          ? 'success'
                          : c.estado === 'DESCARTADA' || c.vencida
                            ? 'secondary'
                            : 'info'
                      "
                    />
                  </td>
                  <td class="derecha">
                    @if (
                      esAlmacen() &&
                      d.resumen.estado === 'EN_COTIZACION' &&
                      c.estado !== 'SELECCIONADA' &&
                      !c.vencida
                    ) {
                      <p-button
                        label="Seleccionar"
                        size="small"
                        [outlined]="true"
                        [ariaLabel]="'Seleccionar la cotización de ' + c.proveedorNombre"
                        (onClick)="seleccionar(c)"
                      />
                    }
                    @if (
                      esAlmacen() &&
                      c.estado === 'SELECCIONADA' &&
                      d.resumen.estado === 'EN_COTIZACION'
                    ) {
                      <p-button
                        label="Generar orden"
                        icon="pi pi-shopping-cart"
                        size="small"
                        [loading]="ocupado()"
                        (onClick)="generarOrden(c)"
                      />
                    }
                  </td>
                </tr>
              </ng-template>
            </p-table>

            @if (comparativa(); as comp) {
              <h3 class="subseccion">Cuadro comparativo</h3>
              <p-table
                [value]="comp.filas"
                styleClass="tabla-comparativa"
                responsiveLayout="scroll"
              >
                <ng-template #header>
                  <tr>
                    <th>Producto</th>
                    <th>Cantidad</th>
                    @for (col of comp.columnas; track col.cotizacionId) {
                      <th>{{ col.proveedorNombre }}</th>
                    }
                  </tr>
                </ng-template>
                <ng-template #body let-fila>
                  <tr>
                    <td>{{ fila.productoNombre }}</td>
                    <td>{{ fila.cantidad }}</td>
                    @for (precio of fila.precios; track $index) {
                      <td
                        [class.mejor]="
                          comp.columnas[$index].cotizacionId === fila.mejorCotizacionId
                        "
                      >
                        {{
                          precio === null
                            ? 'No cotizó'
                            : (precio | currency: 'PEN' : 'symbol-narrow')
                        }}
                      </td>
                    }
                  </tr>
                </ng-template>
                <ng-template #footer>
                  <tr>
                    <td colspan="2">Total sin IGV</td>
                    @for (col of comp.columnas; track col.cotizacionId) {
                      <td [class.mejor]="col.mejorTotal">
                        {{ col.total | currency: 'PEN' : 'symbol-narrow' }}
                        @if (!col.cubreTodo) {
                          <small class="block text-muted">Cotización parcial</small>
                        }
                      </td>
                    }
                  </tr>
                </ng-template>
              </p-table>
            }
          }
        </section>
      }
    }

    <p-dialog
      header="Registrar cotización"
      [visible]="dialogoCotizacion()"
      (visibleChange)="dialogoCotizacion.set($event)"
      [modal]="true"
      [draggable]="false"
      [style]="{ width: '40rem', maxWidth: '96vw' }"
    >
      <form
        class="formulario"
        [formGroup]="formCotizacion"
        (submit)="$event.preventDefault(); guardarCotizacion()"
        novalidate
      >
        <label for="cot-proveedor"
          >Proveedor
          <p-select
            inputId="cot-proveedor"
            formControlName="proveedorId"
            [options]="proveedores()"
            optionLabel="razonSocial"
            optionValue="id"
            [filter]="true"
            placeholder="Elige un proveedor"
            appendTo="body"
          />
        </label>
        <div class="dos">
          <label for="cot-vigencia"
            >Vigente hasta
            <p-datepicker
              inputId="cot-vigencia"
              formControlName="vigenciaHasta"
              dateFormat="dd/mm/yy"
              [minDate]="hoy"
              appendTo="body"
            />
          </label>
          <label for="cot-plazo"
            >Plazo de entrega en días
            <p-inputnumber
              inputId="cot-plazo"
              formControlName="plazoEntregaDias"
              [min]="0"
              [max]="365"
              [useGrouping]="false"
            />
          </label>
        </div>
        <label for="cot-pago"
          >Condición de pago en días
          <p-inputnumber
            inputId="cot-pago"
            formControlName="condicionPagoDias"
            [min]="0"
            [max]="365"
            [useGrouping]="false"
          />
        </label>
        <div formArrayName="precios">
          @for (fila of precios.controls; track fila; let i = $index) {
            <label>
              Precio unitario de {{ detalle()?.items?.[i]?.productoNombre }}
              <p-inputnumber
                [formControlName]="i"
                mode="currency"
                currency="PEN"
                locale="es-PE"
                [min]="0"
                [inputId]="'cot-precio-' + i"
                [ariaLabel]="'Precio unitario de ' + detalle()?.items?.[i]?.productoNombre"
              />
            </label>
          }
        </div>
      </form>
      <ng-template #footer>
        <p-button
          label="Cancelar"
          severity="secondary"
          [outlined]="true"
          (onClick)="dialogoCotizacion.set(false)"
        />
        <p-button
          label="Guardar cotización"
          icon="pi pi-check"
          [loading]="ocupado()"
          (onClick)="guardarCotizacion()"
        />
      </ng-template>
    </p-dialog>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RequisicionDetalleVista implements OnInit {
  readonly id = input.required<string>();

  private readonly api = inject(ComprasApiService);
  private readonly almacen = inject(AlmacenApiService);
  private readonly auth = inject(AuthService);
  private readonly motivos = inject(MotivoService);
  private readonly notificaciones = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly etiquetas = ETIQUETA_ESTADO_REQUISICION;
  protected readonly severidades = SEVERIDAD_ESTADO_REQUISICION;
  protected readonly etiquetasOrden = ETIQUETA_ESTADO_ORDEN_COMPRA;
  protected readonly severidadesOrden = SEVERIDAD_ESTADO_ORDEN_COMPRA;
  protected readonly hoy = new Date();

  protected readonly detalle = signal<RequisicionDetalle | null>(null);
  protected readonly cotizaciones = signal<CotizacionResponse[]>([]);
  protected readonly comparativa = signal<ComparativaResponse | null>(null);
  protected readonly proveedores = signal<ProveedorResponse[]>([]);
  protected readonly ocupado = signal(false);
  protected readonly dialogoCotizacion = signal(false);

  protected readonly esAlmacen = computed(() => this.auth.isAlmacen());
  protected readonly esSolicitante = computed(
    () => this.detalle()?.resumen.solicitanteId === this.auth.user()?.id,
  );
  protected readonly puedeCotizar = computed(
    () =>
      this.auth.isAlmacen() ||
      this.auth.isFinanzas() ||
      this.auth.isGerencia() ||
      this.auth.isAdmin(),
  );
  protected readonly admiteCotizar = computed(() => {
    const estado = this.detalle()?.resumen.estado;
    return estado === 'ENVIADA' || estado === 'EN_COTIZACION';
  });
  protected readonly puedeAnular = computed(() => {
    const estado = this.detalle()?.resumen.estado;
    return estado === 'BORRADOR' || estado === 'ENVIADA' || estado === 'EN_COTIZACION';
  });
  protected readonly puedeRechazar = computed(
    () => this.esAlmacen() && !this.esSolicitante() && this.admiteCotizar(),
  );

  protected readonly formCotizacion = this.fb.group({
    proveedorId: ['', [Validators.required]],
    vigenciaHasta: [null as Date | null],
    plazoEntregaDias: [null as number | null],
    condicionPagoDias: [0],
    precios: this.fb.array<number | null>([]),
  });

  protected get precios(): FormArray {
    return this.formCotizacion.controls.precios;
  }

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.api.requisicion(this.id()).subscribe((d) => {
      this.detalle.set(d);
      if (this.puedeCotizar() && d.resumen.estado !== 'BORRADOR') {
        this.cargarCotizaciones();
      }
    });
  }

  private cargarCotizaciones(): void {
    this.api.cotizaciones(this.id()).subscribe((lista) => {
      this.cotizaciones.set(lista);
      if (lista.length > 0) {
        this.api.comparativa(this.id()).subscribe((c) => this.comparativa.set(c));
      } else {
        this.comparativa.set(null);
      }
    });
  }

  protected enviar(): void {
    this.ocupado.set(true);
    this.api.enviarRequisicion(this.id()).subscribe({
      next: (d) => {
        this.ocupado.set(false);
        this.detalle.set(d);
        this.notificaciones.success('La requisición fue enviada.');
      },
      error: () => this.ocupado.set(false),
    });
  }

  protected anular(): void {
    this.motivos
      .solicitar({
        titulo: 'Anular requisición',
        etiqueta: 'Motivo de la anulación',
        confirmar: 'Anular requisición',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.api.anularRequisicion(this.id(), motivo).subscribe((d) => {
          this.detalle.set(d);
          this.notificaciones.success('La requisición fue anulada.');
        }),
      );
  }

  protected rechazar(): void {
    this.motivos
      .solicitar({
        titulo: 'Rechazar requisición',
        etiqueta: 'Motivo del rechazo',
        confirmar: 'Rechazar requisición',
        severidad: 'danger',
      })
      .subscribe((motivo) =>
        this.api.rechazarRequisicion(this.id(), motivo).subscribe((d) => {
          this.detalle.set(d);
          this.notificaciones.success('La requisición fue rechazada.');
        }),
      );
  }

  protected abrirCotizacion(): void {
    const d = this.detalle();
    if (!d) {
      return;
    }
    this.almacen
      .listarProveedores({ activo: true })
      .subscribe((lista) => this.proveedores.set(lista));
    this.formCotizacion.reset({
      proveedorId: '',
      vigenciaHasta: null,
      plazoEntregaDias: null,
      condicionPagoDias: 0,
    });
    this.precios.clear();
    for (const _ of d.items) {
      this.precios.push(
        this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
      );
    }
    this.dialogoCotizacion.set(true);
  }

  protected guardarCotizacion(): void {
    const d = this.detalle();
    if (!d || this.formCotizacion.invalid) {
      this.formCotizacion.markAllAsTouched();
      this.notificaciones.warning('Completa el proveedor y el precio de cada producto.');
      return;
    }
    const v = this.formCotizacion.getRawValue();
    this.ocupado.set(true);
    this.api
      .registrarCotizacion(this.id(), {
        proveedorId: v.proveedorId,
        vigenciaHasta: v.vigenciaHasta ? toIsoDate(v.vigenciaHasta) : null,
        plazoEntregaDias: v.plazoEntregaDias,
        condicionPagoDias: v.condicionPagoDias,
        items: d.items.map((item, i) => ({
          productoId: item.productoId,
          precioUnitario: (v.precios[i] as number) ?? 0,
        })),
      })
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.dialogoCotizacion.set(false);
          this.notificaciones.success('La cotización fue registrada.');
          this.cargar();
        },
        error: () => this.ocupado.set(false),
      });
  }

  protected seleccionar(c: CotizacionResponse): void {
    this.api.seleccionarCotizacion(c.id).subscribe(() => {
      this.notificaciones.success(`Se seleccionó la cotización de ${c.proveedorNombre}.`);
      this.cargarCotizaciones();
    });
  }

  protected generarOrden(c: CotizacionResponse): void {
    this.ocupado.set(true);
    this.api.ordenDesdeCotizacion(c.id).subscribe({
      next: (orden) => {
        this.ocupado.set(false);
        this.notificaciones.success('Se generó la orden de compra en borrador.');
        void this.router.navigate(['/compras/ordenes', orden.resumen.id]);
      },
      error: () => this.ocupado.set(false),
    });
  }
}
