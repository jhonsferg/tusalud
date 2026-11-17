import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import { FarmaciaStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { Kpi } from '@tusalud/ui/kpi';
import { PageHeader } from '@tusalud/ui/page-header';

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
