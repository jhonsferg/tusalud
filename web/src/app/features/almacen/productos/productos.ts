import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { ToggleSwitch } from 'primeng/toggleswitch';
import { Subject, debounceTime } from 'rxjs';

import {
  AlmacenStore,
  AuthService,
  ETIQUETA_MOTIVO_SALIDA,
  ETIQUETA_TIPO_PRODUCTO,
  MotivoSalida,
  NotificationService,
  ProductoResponse,
  TipoProducto,
  TIPOS_PRODUCTO,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-productos',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    IconField,
    InputIcon,
    InputNumber,
    InputText,
    Message,
    Select,
    TableModule,
    Tag,
    Textarea,
    ToggleSwitch,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './productos.html',
  styleUrl: './productos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Productos implements OnInit {
  protected readonly store = inject(AlmacenStore);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly gestiona = computed(() => this.auth.isAlmacen() || this.auth.isAdmin());
  protected readonly busqueda = signal('');
  protected readonly tipo = signal<TipoProducto | null>(null);
  protected readonly soloBajoStock = signal(false);
  private readonly cambio = new Subject<void>();

  protected readonly tipos = TIPOS_PRODUCTO.map((valor) => ({
    label: ETIQUETA_TIPO_PRODUCTO[valor],
    value: valor,
  }));
  protected readonly etiquetaTipo: Readonly<Record<string, string>> = ETIQUETA_TIPO_PRODUCTO;
  protected readonly motivos = (Object.keys(ETIQUETA_MOTIVO_SALIDA) as MotivoSalida[]).map(
    (valor) => ({ label: ETIQUETA_MOTIVO_SALIDA[valor], value: valor }),
  );

  protected readonly formVisible = signal(false);
  protected readonly editando = signal<ProductoResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly lotesVisible = signal(false);
  protected readonly productoLotes = signal<ProductoResponse | null>(null);

  protected readonly salidaVisible = signal(false);
  protected readonly productoSalida = signal<ProductoResponse | null>(null);
  protected readonly errorSalida = signal<string | null>(null);

  protected readonly form = this.fb.group({
    codigo: ['', [Validators.required, Validators.maxLength(30)]],
    nombre: ['', [Validators.required, Validators.maxLength(150)]],
    tipo: ['MEDICAMENTO' as TipoProducto, [Validators.required]],
    unidad: ['unidad', [Validators.required, Validators.maxLength(30)]],
    principioActivo: ['', [Validators.maxLength(150)]],
    presentacion: ['', [Validators.maxLength(150)]],
    precioVenta: [0, [Validators.required, Validators.min(0)]],
    stockMinimo: [0, [Validators.required, Validators.min(0)]],
    requiereReceta: [false],
    activo: [true],
    descripcion: ['', [Validators.maxLength(500)]],
  });

  protected readonly salida = this.fb.group({
    cantidad: [1, [Validators.required, Validators.min(1)]],
    motivo: ['CONSUMO_INTERNO' as MotivoSalida, [Validators.required]],
    observaciones: ['', [Validators.maxLength(300)]],
  });

  constructor() {
    this.cambio.pipe(debounceTime(300), takeUntilDestroyed()).subscribe(() => this.cargar());
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.store.cargarProductos({
      q: this.busqueda().trim() || null,
      tipo: this.tipo(),
      bajoStock: this.soloBajoStock() || null,
    });
  }

  protected buscar(valor: string): void {
    this.busqueda.set(valor);
    this.cambio.next();
  }

  protected abrirNuevo(): void {
    this.editando.set(null);
    this.error.set(null);
    this.form.reset({
      codigo: '',
      nombre: '',
      tipo: 'MEDICAMENTO',
      unidad: 'unidad',
      principioActivo: '',
      presentacion: '',
      precioVenta: 0,
      stockMinimo: 0,
      requiereReceta: false,
      activo: true,
      descripcion: '',
    });
    this.formVisible.set(true);
  }

  protected abrirEdicion(producto: ProductoResponse): void {
    this.editando.set(producto);
    this.error.set(null);
    this.form.reset({
      codigo: producto.codigo,
      nombre: producto.nombre,
      tipo: producto.tipo,
      unidad: producto.unidad,
      principioActivo: producto.principioActivo ?? '',
      presentacion: producto.presentacion ?? '',
      precioVenta: producto.precioVenta,
      stockMinimo: producto.stockMinimo,
      requiereReceta: producto.requiereReceta,
      activo: producto.activo,
      descripcion: producto.descripcion ?? '',
    });
    this.formVisible.set(true);
  }

  protected guardar(): void {
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const request = {
      ...valor,
      codigo: valor.codigo.trim().toUpperCase(),
      nombre: valor.nombre.trim(),
      principioActivo: valor.principioActivo.trim() || null,
      presentacion: valor.presentacion.trim() || null,
      descripcion: valor.descripcion.trim() || null,
    };
    const actual = this.editando();
    this.guardando.set(true);
    const operacion = actual
      ? this.store.actualizarProducto(actual.id, request)
      : this.store.crearProducto(request);
    operacion.subscribe({
      next: (producto) => {
        this.guardando.set(false);
        this.formVisible.set(false);
        this.notifications.success(
          actual ? `${producto.nombre} fue actualizado.` : `${producto.nombre} fue registrado.`,
        );
      },
      error: (error: { message: string }) => {
        this.guardando.set(false);
        this.error.set(error.message);
      },
    });
  }

  protected verLotes(producto: ProductoResponse): void {
    this.productoLotes.set(producto);
    this.store.cargarLotes(producto.id);
    this.lotesVisible.set(true);
  }

  protected abrirSalida(producto: ProductoResponse): void {
    this.productoSalida.set(producto);
    this.errorSalida.set(null);
    this.salida.reset({ cantidad: 1, motivo: 'CONSUMO_INTERNO', observaciones: '' });
    this.salidaVisible.set(true);
  }

  protected registrarSalida(): void {
    const producto = this.productoSalida();
    this.errorSalida.set(null);
    if (!producto || this.salida.invalid) {
      this.salida.markAllAsTouched();
      return;
    }
    const valor = this.salida.getRawValue();
    this.guardando.set(true);
    this.store
      .registrarSalida({
        productoId: producto.id,
        cantidad: valor.cantidad,
        motivo: valor.motivo,
        observaciones: valor.observaciones.trim() || null,
      })
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.salidaVisible.set(false);
          this.notifications.success('La salida fue registrada.');
          this.cargar();
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.errorSalida.set(error.message);
        },
      });
  }
}
