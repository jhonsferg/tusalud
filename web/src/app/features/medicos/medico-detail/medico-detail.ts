import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';
import { Tag } from 'primeng/tag';

import { NOMBRE_DIA } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { MedicosStore } from '@tusalud/app/core/state/medicos.store';
import { optionalNumberAttribute } from '@tusalud/app/core/utils/route-input';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-medico-detail',
  imports: [CurrencyPipe, RouterLink, Button, Skeleton, Tag, EmptyState, PageHeader],
  templateUrl: './medico-detail.html',
  styleUrl: './medico-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoDetail {
  readonly id = input(undefined, { transform: optionalNumberAttribute });

  protected readonly store = inject(MedicosStore);
  protected readonly auth = inject(AuthService);
  protected readonly nombreDia = NOMBRE_DIA;

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        this.store.selected.reset();
        this.store.loadOne(id);
        this.store.loadHorarios(id);
      }
    });
  }
}
