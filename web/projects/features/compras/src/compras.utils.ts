import { signal } from '@angular/core';
import { Observable } from 'rxjs';

import { NotificationService } from '@tusalud/core';

export interface Operacion<T> {
  readonly ocupado: ReturnType<typeof signal<boolean>>;
  ejecutar(fuente: Observable<T>, mensaje: string, alExito?: (resultado: T) => void): void;
}

export function crearOperacion<T>(notificaciones: NotificationService): Operacion<T> {
  const ocupado = signal(false);
  return {
    ocupado,
    ejecutar(fuente, mensaje, alExito) {
      if (ocupado()) {
        return;
      }
      ocupado.set(true);
      fuente.subscribe({
        next: (resultado) => {
          ocupado.set(false);
          notificaciones.success(mensaje);
          alExito?.(resultado);
        },
        error: () => ocupado.set(false),
      });
    },
  };
}

export function importe(valor: number | null | undefined): number {
  return Math.round((valor ?? 0) * 100) / 100;
}
