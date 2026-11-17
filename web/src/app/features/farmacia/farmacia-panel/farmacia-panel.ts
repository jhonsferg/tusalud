import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { FarmaciaStore } from '@tusalud/app/core/state/farmacia.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { Kpi } from '@tusalud/app/shared/components/kpi/kpi';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-farmacia-panel',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    Button,
    TableModule,
    Tag,
    EmptyState,
    Kpi,
    PageHeader,
  ],
  templateUrl: './farmacia-panel.html',
  styleUrl: './farmacia-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FarmaciaPanel implements OnInit {
  protected readonly store = inject(FarmaciaStore);

  ngOnInit(): void {
    this.store.cargarResumen();
    this.store.cargarRecetas();
  }
}
