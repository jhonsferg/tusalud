import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { CajaStore } from '@tusalud/app/core/state/caja.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-historial-caja',
  imports: [CurrencyPipe, DatePipe, TableModule, Tag, EmptyState, PageHeader],
  templateUrl: './historial-caja.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HistorialCaja implements OnInit {
  protected readonly store = inject(CajaStore);

  ngOnInit(): void {
    this.store.cargarHistorial();
  }
}
