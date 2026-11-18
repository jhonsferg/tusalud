import { Injectable, signal } from '@angular/core';
import { Observable, Subscriber } from 'rxjs';

export interface SolicitudMotivo {
  readonly titulo: string;
  readonly descripcion?: string;
  readonly etiqueta?: string;
  readonly confirmar?: string;
  readonly severidad?: 'danger' | 'primary';
  readonly icono?: string;
  readonly minimo?: number;
  readonly maximo?: number;
}

interface Pendiente {
  readonly solicitud: SolicitudMotivo;
  readonly suscriptor: Subscriber<string>;
}

export function limpiarMotivo(texto: string): string {
  return texto
    .replace(/[\u0000-\u001f\u007f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

@Injectable({ providedIn: 'root' })
export class MotivoService {
  private readonly pendienteState = signal<Pendiente | null>(null);

  readonly solicitud = () => this.pendienteState()?.solicitud ?? null;

  solicitar(solicitud: SolicitudMotivo): Observable<string> {
    return new Observable<string>((suscriptor) => {
      this.pendienteState()?.suscriptor.complete();
      const pendiente: Pendiente = { solicitud, suscriptor };
      this.pendienteState.set(pendiente);
      return () => {
        if (this.pendienteState() === pendiente) {
          this.pendienteState.set(null);
        }
      };
    });
  }

  resolver(motivo: string | null): void {
    const pendiente = this.pendienteState();
    if (!pendiente) {
      return;
    }
    this.pendienteState.set(null);
    if (motivo !== null) {
      pendiente.suscriptor.next(motivo);
    }
    pendiente.suscriptor.complete();
  }
}
