import { CurrencyPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { EspecialidadResponse } from '@tusalud/app/core/models';
import { AuthService } from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { EspecialidadesStore } from '@tusalud/app/core/state/especialidades.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

@Component({
  selector: 'app-especialidad-list',
  imports: [
    CurrencyPipe,
    FormsModule,
    RouterLink,
    Button,
    IconField,
    InputIcon,
    InputText,
    TableModule,
    EmptyState,
    PageHeader,
  ],
  templateUrl: './especialidad-list.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EspecialidadList implements OnInit {
  protected readonly store = inject(EspecialidadesStore);
  protected readonly auth = inject(AuthService);
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);

  protected readonly busqueda = signal('');
  protected readonly filtradas = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    return this.store
      .especialidades()
      .filter(
        (item) =>
          !termino ||
          item.nombre.toLowerCase().includes(termino) ||
          (item.descripcion ?? '').toLowerCase().includes(termino),
      );
  });

  ngOnInit(): void {
    this.store.load();
  }

  protected recargar(): void {
    this.store.load();
  }

  protected eliminar(item: EspecialidadResponse): void {
    this.confirmation.confirm({
      header: 'Eliminar especialidad',
      message: `¿Seguro que deseas eliminar la especialidad ${item.nombre}? Esta acción no se puede deshacer.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminar(item.id).subscribe({
          next: () => this.notifications.success(`La especialidad ${item.nombre} fue eliminada.`),
          error: () => undefined,
        }),
    });
  }
}
