import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { SelectButton } from 'primeng/selectbutton';
import { Skeleton } from 'primeng/skeleton';

import { NotificacionResponse } from '@tusalud/app/core/models';
import { NotificacionesStore } from '@tusalud/app/core/state/notificaciones.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';
import { COLOR_NOTIFICACION, ICONO_NOTIFICACION } from '@tusalud/app/shared/utils/notificacion';

type Filtro = 'todas' | 'noLeidas';

@Component({
  selector: 'app-notificaciones',
  imports: [DatePipe, FormsModule, Button, SelectButton, Skeleton, EmptyState, PageHeader],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Notificaciones implements OnInit {
  protected readonly store = inject(NotificacionesStore);
  private readonly router = inject(Router);

  protected readonly filtro = signal<Filtro>('todas');
  protected readonly opciones = [
    { label: 'Todas', value: 'todas' },
    { label: 'Sin leer', value: 'noLeidas' },
  ];
  protected readonly icono = ICONO_NOTIFICACION;
  protected readonly color = COLOR_NOTIFICACION;

  protected readonly visibles = computed(() =>
    this.filtro() === 'noLeidas'
      ? this.store.items().filter((item) => !item.leida)
      : this.store.items(),
  );

  ngOnInit(): void {
    this.store.cargar();
    this.store.refrescarContador();
  }

  protected abrir(item: NotificacionResponse): void {
    if (!item.leida) {
      this.store.marcarLeida(item.id).subscribe({ error: () => undefined });
    }
    if (item.enlace) {
      void this.router.navigateByUrl(item.enlace);
    }
  }

  protected marcarLeida(item: NotificacionResponse): void {
    this.store.marcarLeida(item.id).subscribe({ error: () => undefined });
  }

  protected eliminar(item: NotificacionResponse): void {
    this.store.eliminar(item.id).subscribe({ error: () => undefined });
  }

  protected marcarTodas(): void {
    this.store.marcarTodasLeidas().subscribe({ error: () => undefined });
  }
}
