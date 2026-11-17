import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { Tag } from 'primeng/tag';

import { ETIQUETA_METODO_FARMACIA, FarmaciaStore } from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

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
