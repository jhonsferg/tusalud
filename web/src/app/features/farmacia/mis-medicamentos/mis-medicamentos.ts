import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Tag } from 'primeng/tag';

import { ETIQUETA_METODO_FARMACIA } from '@tusalud/app/core/models';
import { FarmaciaStore } from '@tusalud/app/core/state/farmacia.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-mis-medicamentos',
  imports: [CurrencyPipe, DatePipe, Tag, EmptyState, PageHeader],
  templateUrl: './mis-medicamentos.html',
  styleUrl: './mis-medicamentos.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisMedicamentos implements OnInit {
  protected readonly store = inject(FarmaciaStore);
  protected readonly etiquetaMetodo: Readonly<Record<string, string>> = ETIQUETA_METODO_FARMACIA;

  ngOnInit(): void {
    this.store.cargarMisDispensaciones();
  }
}
