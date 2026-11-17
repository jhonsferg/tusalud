import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { AdminStore } from '@tusalud/app/core/state/admin.store';
import { toIsoDate } from '@tusalud/app/core/utils/date';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-pagos-admin',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    Button,
    DatePicker,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './pagos-admin.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PagosAdmin implements OnInit {
  protected readonly store = inject(AdminStore);
  protected readonly rango = signal<Date[] | null>(null);

  ngOnInit(): void {
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
    this.store.cargarPagos({
      inicio: inicio ? toIsoDate(inicio) : null,
      fin: fin ? toIsoDate(fin) : null,
    });
  }
}
