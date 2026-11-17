import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';

import { ETIQUETA_ESTADO_TURNO, ETIQUETA_PRIORIDAD, TurnoResponse } from '@tusalud/app/core/models';
import { NotificationService } from '@tusalud/app/core/services/notification.service';
import { RecepcionStore } from '@tusalud/app/core/state/recepcion.store';
import { EmptyState } from '@tusalud/app/shared/components/empty-state/empty-state';
import { PageHeader } from '@tusalud/app/shared/components/page-header/page-header';

const REFRESCO_MS = 10_000;

@Component({
  selector: 'app-cola-medico',
  imports: [RouterLink, Button, Tag, EmptyState, PageHeader],
  templateUrl: './cola-medico.html',
  styleUrl: './cola-medico.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ColaMedico implements OnInit {
  protected readonly store = inject(RecepcionStore);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly etiquetaEstado = ETIQUETA_ESTADO_TURNO;
  protected readonly etiquetaPrioridad = ETIQUETA_PRIORIDAD;

  ngOnInit(): void {
    this.store.cargarCola();
    const temporizador = setInterval(() => this.store.cargarCola(), REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }

  protected activos(): TurnoResponse[] {
    return (this.store.cola.value() ?? []).filter(
      (turno) =>
        turno.estado === 'EN_ESPERA' ||
        turno.estado === 'LLAMADO' ||
        turno.estado === 'EN_ATENCION',
    );
  }

  protected cambiar(
    turno: TurnoResponse,
    accion: 'llamar' | 'iniciar' | 'finalizar' | 'ausente',
    mensaje: string,
  ): void {
    this.store.cambiar(turno.id, accion).subscribe({
      next: () => this.notifications.success(mensaje),
      error: () => undefined,
    });
  }
}
