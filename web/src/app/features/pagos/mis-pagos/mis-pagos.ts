import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { PagoResponse } from '@tusalud/app/core/models';
import { PagosStore } from '@tusalud/app/core/state/pagos.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-mis-pagos',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    Button,
    Dialog,
    TableModule,
    Tag,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './mis-pagos.html',
  styleUrl: './mis-pagos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisPagos implements OnInit {
  protected readonly store = inject(PagosStore);
  protected readonly seleccionado = signal<PagoResponse | null>(null);
  protected readonly visible = signal(false);

  ngOnInit(): void {
    this.store.cargarPagos();
  }

  protected ver(pago: PagoResponse): void {
    this.seleccionado.set(pago);
    this.visible.set(true);
  }

  protected imprimir(): void {
    window.print();
  }
}
