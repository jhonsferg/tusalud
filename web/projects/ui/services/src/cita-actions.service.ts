import { Injectable, inject } from '@angular/core';
import { MotivoService } from '@tusalud/ui/dialogo-motivo';
import { Observable } from 'rxjs';
import { ConfirmationService } from 'primeng/api';

import { CitaResponse, CitasStore, NotificationService } from '@tusalud/core';

interface Accion {
  readonly header: string;
  readonly message: (cita: CitaResponse) => string;
  readonly acceptLabel: string;
  readonly severity: 'danger' | 'success' | 'primary';
  readonly icon: string;
  readonly success: string;
  readonly run: (store: CitasStore, id: string) => Observable<CitaResponse>;
}

const ACCIONES = {
  confirmar: {
    header: 'Confirmar cita',
    message: (cita) => `¿Confirmas la cita de ${cita.pacienteNombre}?`,
    acceptLabel: 'Confirmar cita',
    severity: 'primary',
    icon: 'pi pi-check-circle',
    success: 'La cita fue confirmada.',
    run: (store, id) => store.confirmar(id),
  },
  atender: {
    header: 'Marcar como atendida',
    message: (cita) => `¿Registras la cita de ${cita.pacienteNombre} como atendida?`,
    acceptLabel: 'Marcar atendida',
    severity: 'success',
    icon: 'pi pi-verified',
    success: 'La cita fue marcada como atendida.',
    run: (store, id) => store.atender(id),
  },
} satisfies Record<string, Accion>;

export type AccionCita = keyof typeof ACCIONES;

@Injectable({ providedIn: 'root' })
export class CitaActions {
  private readonly confirmation = inject(ConfirmationService);
  private readonly notifications = inject(NotificationService);
  private readonly store = inject(CitasStore);
  private readonly motivos = inject(MotivoService);

  cancelar(cita: CitaResponse, onDone?: (actualizada: CitaResponse) => void): void {
    this.motivos
      .solicitar({
        titulo: 'Cancelar cita',
        descripcion: `Se cancelará la cita de ${cita.pacienteNombre} con ${cita.medicoNombre}. El horario quedará libre para otros pacientes.`,
        etiqueta: 'Motivo de la cancelación',
        confirmar: 'Cancelar cita',
        severidad: 'danger',
        icono: 'pi pi-times-circle',
      })
      .subscribe((motivo) =>
        this.store.cancelar(cita.id, motivo).subscribe({
          next: (actualizada) => {
            this.notifications.success('La cita fue cancelada.');
            onDone?.(actualizada);
          },
          error: () => undefined,
        }),
      );
  }

  ejecutar(
    accion: AccionCita,
    cita: CitaResponse,
    onDone?: (actualizada: CitaResponse) => void,
  ): void {
    const definicion: Accion = ACCIONES[accion];
    this.confirmation.confirm({
      header: definicion.header,
      message: definicion.message(cita),
      icon: definicion.icon,
      acceptLabel: definicion.acceptLabel,
      rejectLabel: 'Volver',
      acceptButtonProps: {
        severity: definicion.severity === 'primary' ? undefined : definicion.severity,
      },
      rejectButtonProps: { severity: 'secondary', outlined: true },
      accept: () =>
        definicion.run(this.store, cita.id).subscribe({
          next: (actualizada) => {
            this.notifications.success(definicion.success);
            onDone?.(actualizada);
          },
          error: () => undefined,
        }),
    });
  }
}
