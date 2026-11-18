import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';

import {
  ETIQUETA_ESTADO_EXPORTACION,
  ETIQUETA_TIPO_EXPORTACION,
  EstadoExportacion,
  ExportacionResponse,
  ExportacionesStore,
  NotificationService,
  esTerminal,
} from '@tusalud/core';
import { EmptyState } from '@tusalud/ui/empty-state';
import { PageHeader } from '@tusalud/ui/page-header';

type Severidad = 'success' | 'info' | 'warn' | 'danger' | 'secondary';

const SEVERIDAD: Readonly<Record<EstadoExportacion, Severidad>> = {
  SOLICITADA: 'info',
  PROCESANDO: 'info',
  LISTA: 'success',
  ERROR: 'danger',
  EXPIRADA: 'secondary',
};

const REFRESCO_MS = 3000;

@Component({
  selector: 'app-mis-exportaciones',
  imports: [DatePipe, Button, TableModule, Tag, EmptyState, PageHeader],
  templateUrl: './mis-exportaciones.html',
  styleUrl: './mis-exportaciones.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MisExportaciones implements OnInit {
  protected readonly store = inject(ExportacionesStore);
  private readonly notificaciones = inject(NotificationService);
  private readonly confirmacion = inject(ConfirmationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly etiquetaTipo = ETIQUETA_TIPO_EXPORTACION;
  protected readonly etiquetaEstado = ETIQUETA_ESTADO_EXPORTACION;

  ngOnInit(): void {
    this.store.cargar();
    const temporizador = setInterval(() => {
      const items = this.store.lista.value() ?? [];
      if (items.some((item) => !esTerminal(item.estado))) {
        this.store.refrescar();
      }
    }, REFRESCO_MS);
    this.destroyRef.onDestroy(() => clearInterval(temporizador));
  }

  protected nombreTipo(tipo: string): string {
    return this.etiquetaTipo[tipo] || tipo;
  }

  protected etiquetaDeEstado(estado: EstadoExportacion): string {
    return this.etiquetaEstado[estado];
  }

  protected severidad(estado: EstadoExportacion): Severidad {
    return SEVERIDAD[estado];
  }

  protected tamano(bytes: number | null): string {
    if (bytes === null) {
      return '';
    }
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected descargar(exportacion: ExportacionResponse): void {
    this.store.descargar(exportacion).subscribe({ error: () => undefined });
  }

  protected eliminar(exportacion: ExportacionResponse): void {
    this.confirmacion.confirm({
      header: 'Eliminar exportación',
      message: 'Se eliminará el archivo generado. Esta acción no se puede deshacer.',
      icon: 'pi pi-trash',
      acceptLabel: 'Eliminar',
      rejectLabel: 'Cancelar',
      acceptButtonProps: { severity: 'danger' },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        this.store.eliminar(exportacion.id).subscribe({
          next: () => this.notificaciones.success('La exportación fue eliminada.'),
          error: () => undefined,
        }),
    });
  }
}
