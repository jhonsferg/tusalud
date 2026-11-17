import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { PagoResponse, PagosStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

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
