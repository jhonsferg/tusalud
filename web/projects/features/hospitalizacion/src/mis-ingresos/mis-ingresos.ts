import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Tag } from 'primeng/tag';

import { HospitalizacionStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

@Component({
  selector: 'app-mis-ingresos',
  imports: [CurrencyPipe, DatePipe, Tag, EmptyState, PageHeader],
  templateUrl: './mis-ingresos.html',
  styleUrl: './mis-ingresos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisIngresos implements OnInit {
  protected readonly store = inject(HospitalizacionStore);

  ngOnInit(): void {
    this.store.cargarMisIngresos();
  }
}
