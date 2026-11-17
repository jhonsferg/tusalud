import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { AlmacenStore, TipoMovimiento, toIsoDate } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-movimientos',
  imports: [DatePipe, FormsModule, DatePicker, Select, TableModule, Tag, EmptyState, PageHeader],
  templateUrl: './movimientos.html',
  styleUrl: './movimientos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Movimientos implements OnInit {
  protected readonly store = inject(AlmacenStore);

  protected readonly productoId = signal<string | null>(null);
  protected readonly tipo = signal<TipoMovimiento | null>(null);
  protected readonly rango = signal<Date[] | null>(null);

  protected readonly tipos = [
    { label: 'Entradas', value: 'ENTRADA' },
    { label: 'Salidas', value: 'SALIDA' },
    { label: 'Ajustes', value: 'AJUSTE' },
  ];
  protected readonly opcionesProducto = computed(() =>
    (this.store.productos.value() ?? []).map((producto) => ({
      label: `${producto.codigo} · ${producto.nombre}`,
      value: producto.id,
    })),
  );

  ngOnInit(): void {
    this.store.cargarProductos();
    this.cargar();
  }

  protected cambiarRango(valor: Date[] | null): void {
    this.rango.set(valor);
    const [inicio] = valor ?? [];
    if (!valor || inicio) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarMovimientos({
      productoId: this.productoId(),
      tipo: this.tipo(),
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : inicio ? toIsoDate(inicio) : null,
    });
  }
}
