import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Message } from 'primeng/message';
import { Select } from 'primeng/select';
import { Textarea } from 'primeng/textarea';
import { startWith } from 'rxjs';

import {
  DispensacionResponse,
  ETIQUETA_METODO_FARMACIA,
  METODOS_PAGO_FARMACIA,
  MetodoPagoFarmacia,
  RecetaFarmaciaResponse,
} from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { FarmaciaStore } from '@tusalud/app/core/state/farmacia.store';
import { optionalIdAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

@Component({
  selector: 'app-dispensar',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ReactiveFormsModule,
    Button,
    Dialog,
    InputNumber,
    InputText,
    Message,
    Select,
    Textarea,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './dispensar.html',
  styleUrl: './dispensar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Dispensar implements OnInit {
  readonly atencionId = input(undefined, { transform: optionalIdAttribute });

  protected readonly store = inject(FarmaciaStore);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly busqueda = signal('');
  protected readonly receta = signal<RecetaFarmaciaResponse | null>(null);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly comprobante = signal<DispensacionResponse | null>(null);

  protected readonly metodos = METODOS_PAGO_FARMACIA.map((valor) => ({
    label: ETIQUETA_METODO_FARMACIA[valor],
    value: valor,
  }));

  protected readonly form = this.fb.group({
    metodoPago: ['EFECTIVO' as MetodoPagoFarmacia, [Validators.required]],
    observaciones: [''],
    items: this.fb.array([] as ReturnType<Dispensar['nuevoItem']>[]),
  });

  protected get items(): FormArray {
    return this.form.controls.items;
  }

  private readonly valores = toSignal(this.form.valueChanges.pipe(startWith(this.form.value)), {
    initialValue: this.form.value,
  });

  protected readonly opciones = computed(() =>
    (this.store.catalogo.value() ?? []).map((producto) => ({
      label: `${producto.nombre}${producto.presentacion ? ' · ' + producto.presentacion : ''} — stock ${producto.stockActual}`,
      value: producto.id,
      producto,
    })),
  );

  protected readonly total = computed(() => {
    const catalogo = new Map((this.store.catalogo.value() ?? []).map((p) => [p.id, p]));
    return (this.valores().items ?? []).reduce((suma, item) => {
      const producto = catalogo.get(String(item?.['productoId']));
      return suma + (producto ? producto.precioVenta * (Number(item?.['cantidad']) || 0) : 0);
    }, 0);
  });

  protected readonly recetasFiltradas = computed(() => {
    const termino = normalizar(this.busqueda().trim());
    return (this.store.recetas.value() ?? []).filter(
      (receta) =>
        !termino ||
        normalizar(receta.pacienteNombre).includes(termino) ||
        receta.dni.includes(termino),
    );
  });

  constructor() {
    effect(() => {
      const id = this.atencionId();
      const recetas = this.store.recetas.value();
      if (id !== undefined && recetas && !untracked(() => this.receta())) {
        const encontrada = recetas.find((item) => item.atencionId === id);
        if (encontrada) {
          untracked(() => this.elegirReceta(encontrada));
        }
      }
    });
  }

  ngOnInit(): void {
    this.store.cargarCatalogo();
    this.store.cargarRecetas();
  }

  protected nuevoItem(recetaItemId: string | null = null, productoId: string | null = null) {
    return this.fb.group({
      recetaItemId: [recetaItemId],
      etiqueta: [''],
      productoId: [productoId as string | null, [Validators.required]],
      cantidad: [1, [Validators.required, Validators.min(1)]],
    });
  }

  protected elegirReceta(receta: RecetaFarmaciaResponse | null): void {
    this.receta.set(receta);
    this.items.clear();
    if (!receta) {
      return;
    }
    const catalogo = this.store.catalogo.value() ?? [];
    for (const item of receta.items.filter((i) => !i.dispensado)) {
      const nombre = normalizar(item.medicamento);
      const sugerido = catalogo.find(
        (producto) =>
          normalizar(producto.nombre).includes(nombre) ||
          nombre.includes(normalizar(producto.principioActivo ?? '\u0000')),
      );
      const fila = this.nuevoItem(item.id, sugerido?.id ?? null);
      fila.controls.etiqueta.setValue(`${item.medicamento}${item.dosis ? ' · ' + item.dosis : ''}`);
      this.items.push(fila);
    }
  }

  protected agregarLibre(): void {
    this.items.push(this.nuevoItem());
  }

  protected quitar(indice: number): void {
    this.items.removeAt(indice);
  }

  protected confirmar(): void {
    this.error.set(null);
    if (this.items.length === 0) {
      this.error.set('Agrega al menos un producto.');
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const receta = this.receta();
    this.guardando.set(true);
    this.store
      .dispensar({
        atencionId: receta?.atencionId ?? null,
        pacienteId: receta?.pacienteId ?? null,
        metodoPago: valor.metodoPago ?? 'EFECTIVO',
        observaciones: valor.observaciones?.trim() || null,
        items: (valor.items as Record<string, unknown>[]).map((item) => ({
          recetaItemId: (item['recetaItemId'] as string | null) ?? null,
          productoId: item['productoId'] as string,
          cantidad: Number(item['cantidad']),
        })),
      })
      .subscribe({
        next: (dispensacion) => {
          this.guardando.set(false);
          this.comprobante.set(dispensacion);
          this.notifications.success(
            `Dispensación registrada. Comprobante ${dispensacion.comprobante}.`,
          );
          this.elegirReceta(null);
          this.store.cargarCatalogo();
          this.store.cargarRecetas();
        },
        error: (error: { message: string }) => {
          this.guardando.set(false);
          this.error.set(error.message);
        },
      });
  }

  protected imprimir(): void {
    window.print();
  }
}
