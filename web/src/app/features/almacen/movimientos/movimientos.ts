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

import { TipoMovimiento } from '@tusalud/app/core/models';
import { AlmacenStore } from '@tusalud/app/core/state/almacen.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-movimientos',
  imports: [DatePipe, FormsModule, DatePicker, Select, TableModule, Tag, EmptyState, PageHeader],
  templateUrl: './movimientos.html',
  styleUrl: './movimientos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Movimientos implements OnInit {
  protected readonly store = inject(AlmacenStore);

  protected readonly productoId = signal<number | null>(null);
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
    const [inicio, fin] = valor ?? [];
    if (!valor || (inicio && fin)) {
      this.cargar();
    }
  }

  protected cargar(): void {
    const [inicio, fin] = this.rango() ?? [];
    this.store.cargarMovimientos({
      productoId: this.productoId(),
      tipo: this.tipo(),
      desde: inicio ? toIsoDate(inicio) : null,
      hasta: fin ? toIsoDate(fin) : null,
    });
  }
}
