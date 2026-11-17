import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { startWith } from 'rxjs';

import { AlmacenStore, IngresoResponse, NotificationService, toIsoDate } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-ingresos',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    Button,
    DatePicker,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './ingresos.html',
  styleUrl: './ingresos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ingresos implements OnInit {
  protected readonly store = inject(AlmacenStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly visible = signal(false);
  protected readonly detalle = signal<IngresoResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly manana = new Date(Date.now() + 86_400_000);

  protected readonly form = this.fb.group({
    proveedorId: [null as string | null],
    documento: ['', [Validators.maxLength(40)]],
    observaciones: ['', [Validators.maxLength(300)]],
    items: this.fb.array([this.nuevoItem()]),
  });

  protected get items(): FormArray {
    return this.form.controls.items;
  }

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.value)), {
    initialValue: this.form.value,
  });

  protected readonly total = computed(() =>
    (this.valores().items ?? []).reduce(
      (suma, item) =>
        suma + (Number(item?.['cantidad']) || 0) * (Number(item?.['costoUnitario']) || 0),
      0,
    ),
  );

  protected readonly opcionesProducto = computed(() =>
    this.store.productosActivos().map((producto) => ({
      label: `${producto.codigo} · ${producto.nombre}`,
      value: producto.id,
    })),
  );
  protected readonly opcionesProveedor = computed(() =>
    this.store.proveedoresActivos().map((proveedor) => ({
      label: proveedor.razonSocial,
      value: proveedor.id,
    })),
  );

  ngOnInit(): void {
    this.store.cargarIngresos();
    this.store.cargarProductos({ activo: true });
    this.store.cargarProveedores({ activo: true });
  }

  protected nuevoItem() {
    return this.fb.group({
      productoId: [null as string | null, [Validators.required]],
      numeroLote: ['', [Validators.required, Validators.maxLength(40)]],
      fechaVencimiento: [null as Date | null, [Validators.required]],
      cantidad: [1, [Validators.required, Validators.min(1)]],
      costoUnitario: [0, [Validators.required, Validators.min(0)]],
      ubicacion: ['', [Validators.maxLength(60)]],
    });
  }

  protected abrir(): void {
    this.error.set(null);
    this.form.reset({ proveedorId: null, documento: '', observaciones: '' });
    this.items.clear();
    this.items.push(this.nuevoItem());
    this.visible.set(true);
  }

  protected agregarItem(): void {
    this.items.push(this.nuevoItem());
  }

  protected quitarItem(indice: number): void {
    if (this.items.length > 1) {
      this.items.removeAt(indice);
    }
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Completa el producto, el lote, el vencimiento y la cantidad de cada ítem.');
      return;
    }
    const valor = this.form.getRawValue();
    this.guardando.set(true);
    this.store
      .registrarIngreso({
        proveedorId: valor.proveedorId,
        documento: valor.documento?.trim() || null,
        observaciones: valor.observaciones?.trim() || null,
        items: (valor.items as Record<string, unknown>[]).map((item) => ({
          productoId: item['productoId'] as string,
          numeroLote: String(item['numeroLote']).trim(),
          fechaVencimiento: toIsoDate(item['fechaVencimiento'] as Date),
          cantidad: Number(item['cantidad']),
          costoUnitario: Number(item['costoUnitario']),
          ubicacion: String(item['ubicacion'] ?? '').trim() || null,
        })),
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.visible.set(false);
          this.notifications.success('El ingreso fue registrado y el stock actualizado.');
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }
}
