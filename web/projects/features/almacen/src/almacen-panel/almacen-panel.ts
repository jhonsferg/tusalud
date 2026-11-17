import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { AlmacenStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';

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
