import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { ToggleSwitch } from 'primeng/toggleswitch';

import {
  AlmacenApiService,
  ComprasApiService,
  NotificationService,
  OrdenCompraDetalle,
  ProductoResponse,
  ProveedorResponse,
  parseIsoDate,
  toIsoDate,
} from '@tusalud/core';
import { PageHeader } from '@tusalud/ui/page-header';
import { ESTILOS_COMPRAS } from '../compras.estilos';
import { importe } from '../compras.utils';

const TASA_IGV = 0.18;

@Component({
  selector: 'app-orden-form',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    Button,
    DatePicker,
    InputNumber,
    InputText,
    Select,
    ToggleSwitch,
    PageHeader,
  ],
  template: `
    <app-page-header
      [title]="id() ? 'Editar orden de compra' : 'Nueva orden de compra'"
      subtitle="La orden se numera cuando se aprueba y desde entonces no se modifica"
      icon="pi-shopping-cart"
      backLink="/compras/ordenes"
      backLabel="Órdenes de compra"
    />

    <form class="panel formulario" [formGroup]="form" (submit)="$event.preventDefault()" novalidate>
      <label for="oc-proveedor"
        >Proveedor
        <p-select
          inputId="oc-proveedor"
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
      <div class="dos">
        <label for="oc-pago"
          >Condición de pago en días
          <p-inputnumber
            inputId="oc-pago"
            formControlName="condicionPagoDias"
            [min]="0"
            [max]="365"
            [useGrouping]="false"
          />
        </label>
        <label for="oc-entrega"
          >Entrega estimada
          <p-datepicker
            inputId="oc-entrega"
            formControlName="fechaEntregaEstimada"
            dateFormat="dd/mm/yy"
            appendTo="body"
          />
        </label>
      </div>
      <label for="oc-obs"
        >Observaciones
        <input
          pInputText
          id="oc-obs"
          formControlName="observaciones"
          maxlength="300"
          class="w-full"
        />
      </label>
      <label for="oc-igv" class="filtros">
        <p-toggleswitch inputId="oc-igv" formControlName="afectaIgv" />
        Los precios están afectos al IGV del 18 %
      </label>

      <h3 class="subseccion">Productos</h3>
      <div formArrayName="items">
        @for (fila of items.controls; track fila; let i = $index) {
          <div class="linea" [formGroupName]="i">
            <label
              >Producto
              <p-select
                formControlName="productoId"
                [options]="productos()"
                optionLabel="nombre"
                optionValue="id"
                [filter]="true"
                filterBy="nombre,codigo"
                placeholder="Elige un producto"
                appendTo="body"
                [ariaLabel]="'Producto ' + (i + 1)"
              />
            </label>
            <label
              >Cantidad
              <p-inputnumber
                formControlName="cantidad"
                [min]="1"
                [max]="100000"
                [useGrouping]="false"
                [inputId]="'oc-cantidad-' + i"
                [ariaLabel]="'Cantidad del producto ' + (i + 1)"
              />
            </label>
            <label
              >Precio unitario
              <p-inputnumber
                formControlName="precioUnitario"
                mode="currency"
                currency="PEN"
                locale="es-PE"
                [min]="0"
                [inputId]="'oc-precio-' + i"
                [ariaLabel]="'Precio unitario del producto ' + (i + 1)"
              />
            </label>
            <p-button
              icon="pi pi-trash"
              severity="danger"
              [text]="true"
              [rounded]="true"
              [ariaLabel]="'Quitar el producto ' + (i + 1)"
              (onClick)="quitar(i)"
            />
          </div>
        }
      </div>
      <div>
        <p-button
          label="Agregar producto"
          icon="pi pi-plus"
          severity="secondary"
          [outlined]="true"
          size="small"
          (onClick)="agregar()"
        />
      </div>

      <div class="totales" aria-live="polite">
        <div>
          <span>Subtotal</span><span>{{ subtotal() | currency: 'PEN' : 'symbol-narrow' }}</span>
        </div>
        <div>
          <span>IGV 18 %</span><span>{{ igv() | currency: 'PEN' : 'symbol-narrow' }}</span>
        </div>
        <div class="total">
          <span>Total</span><span>{{ total() | currency: 'PEN' : 'symbol-narrow' }}</span>
        </div>
        @if (limite() !== null) {
          <small class="text-muted">
            {{
              total() > (limite() ?? 0)
                ? 'Supera el límite de finanzas y la aprobará la gerencia.'
                : 'La aprobará finanzas.'
            }}
          </small>
        }
      </div>

      @if (error(); as mensaje) {
        <p class="text-red-600" role="alert">{{ mensaje }}</p>
      }
      <div class="acciones">
        <p-button
          label="Guardar borrador"
          icon="pi pi-save"
          [loading]="guardando()"
          (onClick)="guardar()"
        />
      </div>
    </form>
  `,
  styles: [ESTILOS_COMPRAS],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdenForm implements OnInit {
  readonly id = input<string>();
  readonly requisicionId = input<string>();

  private readonly api = inject(ComprasApiService);
  private readonly almacen = inject(AlmacenApiService);
  private readonly notificaciones = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly proveedores = signal<ProveedorResponse[]>([]);
  protected readonly productos = signal<ProductoResponse[]>([]);
  protected readonly limite = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    proveedorId: ['', [Validators.required]],
    afectaIgv: [true],
    condicionPagoDias: [0],
    fechaEntregaEstimada: [null as Date | null],
    observaciones: [''],
    items: this.fb.array([this.nuevaLinea()]),
  });

  private readonly valor = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  protected readonly subtotal = computed(() => {
    const v = this.valor() as {
      items?: { cantidad: number | null; precioUnitario: number | null }[];
    };
    return importe(
      (v.items ?? []).reduce((suma, i) => suma + (i.cantidad ?? 0) * (i.precioUnitario ?? 0), 0),
    );
  });
  protected readonly igv = computed(() =>
    (this.valor() as { afectaIgv?: boolean }).afectaIgv ? importe(this.subtotal() * TASA_IGV) : 0,
  );
  protected readonly total = computed(() => importe(this.subtotal() + this.igv()));

  protected get items() {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    this.almacen.listarProveedores({ activo: true }).subscribe((l) => this.proveedores.set(l));
    this.almacen.listarProductos({ activo: true }).subscribe((l) => this.productos.set(l));
    this.api.configuracion().subscribe((c) => this.limite.set(c.limiteFinanzas));
    const id = this.id();
    if (id) {
      this.api.orden(id).subscribe((o) => this.cargar(o));
      return;
    }
    const requisicion = this.requisicionId();
    if (requisicion) {
      this.api.requisicion(requisicion).subscribe((r) => {
        this.items.clear();
        for (const item of r.items) {
          this.items.push(this.nuevaLinea(item.productoId, item.cantidad, null));
        }
      });
    }
  }

  protected alElegirProveedor(proveedorId: string): void {
    const proveedor = this.proveedores().find((p) => p.id === proveedorId);
    if (!proveedor) {
      return;
    }
    this.form.controls.condicionPagoDias.setValue(proveedor.condicionPagoDias ?? 0);
    if (proveedor.plazoEntregaDias != null && !this.form.controls.fechaEntregaEstimada.value) {
      const fecha = new Date();
      fecha.setDate(fecha.getDate() + proveedor.plazoEntregaDias);
      this.form.controls.fechaEntregaEstimada.setValue(fecha);
    }
  }

  protected agregar(): void {
    this.items.push(this.nuevaLinea());
  }

  protected quitar(indice: number): void {
    this.items.removeAt(indice);
    if (this.items.length === 0) {
      this.agregar();
    }
  }

  protected guardar(): void {
    this.error.set(null);
    const v = this.form.getRawValue();
    const items = v.items
      .filter((i) => i.productoId)
      .map((i) => ({
        productoId: i.productoId as string,
        cantidad: i.cantidad ?? 1,
        precioUnitario: i.precioUnitario ?? 0,
      }));
    if (!v.proveedorId) {
      this.error.set('Elige el proveedor.');
      return;
    }
    if (items.length === 0 || items.some((i) => i.precioUnitario <= 0)) {
      this.error.set('Cada producto necesita una cantidad y un precio mayor a cero.');
      return;
    }
    const request = {
      proveedorId: v.proveedorId,
      requisicionId: this.requisicionId() ?? null,
      afectaIgv: v.afectaIgv,
      condicionPagoDias: v.condicionPagoDias,
      fechaEntregaEstimada: v.fechaEntregaEstimada ? toIsoDate(v.fechaEntregaEstimada) : null,
      observaciones: v.observaciones.trim() || null,
      items,
    };
    this.guardando.set(true);
    const id = this.id();
    (id ? this.api.actualizarOrden(id, request) : this.api.crearOrden(request)).subscribe({
      next: (orden) => {
        this.guardando.set(false);
        this.notificaciones.success('La orden quedó en borrador.');
        void this.router.navigate(['/compras/ordenes', orden.resumen.id]);
      },
      error: () => this.guardando.set(false),
    });
  }

  private cargar(orden: OrdenCompraDetalle): void {
    this.form.patchValue({
      proveedorId: orden.proveedorId,
      afectaIgv: orden.afectaIgv,
      condicionPagoDias: orden.condicionPagoDias,
      fechaEntregaEstimada: orden.fechaEntregaEstimada
        ? parseIsoDate(orden.fechaEntregaEstimada)
        : null,
      observaciones: orden.observaciones ?? '',
    });
    this.items.clear();
    for (const item of orden.items) {
      this.items.push(this.nuevaLinea(item.productoId, item.cantidad, item.precioUnitario));
    }
  }

  private nuevaLinea(productoId: string | null = null, cantidad = 1, precio: number | null = null) {
    return this.fb.group({
      productoId: [productoId as string | null, [Validators.required]],
      cantidad: [cantidad, [Validators.required, Validators.min(1)]],
      precioUnitario: [precio as number | null, [Validators.required, Validators.min(0.01)]],
    });
  }
}
