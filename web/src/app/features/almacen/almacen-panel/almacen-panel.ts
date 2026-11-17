import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { AlmacenStore } from '@tusalud/app/core/state/almacen.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-almacen-panel',
  imports: [CurrencyPipe, DatePipe, RouterLink, Button, Tag, EmptyState, Kpi, PageHeader],
  templateUrl: './almacen-panel.html',
  styleUrl: './almacen-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AlmacenPanel implements OnInit {
  protected readonly store = inject(AlmacenStore);

  ngOnInit(): void {
    this.store.cargarResumen();
    this.store.cargarAlertas();
  }
}
