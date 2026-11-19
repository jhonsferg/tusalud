import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DispositivosService {
  readonly camaraId = signal<string | null>(null);
  readonly microfonoId = signal<string | null>(null);

  restricciones(): MediaStreamConstraints {
    const camara = this.camaraId();
    const microfono = this.microfonoId();
    return {
      video: camara
        ? { deviceId: { exact: camara }, width: { ideal: 1280 }, height: { ideal: 720 } }
        : { width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: microfono
        ? { deviceId: { exact: microfono }, echoCancellation: true, noiseSuppression: true }
        : { echoCancellation: true, noiseSuppression: true },
    };
  }
}

export function mensajeDeMedios(error: unknown): string {
  const nombre = (error as { name?: string } | null)?.name ?? '';
  switch (nombre) {
    case 'NotAllowedError':
    case 'SecurityError':
      return 'El navegador no tiene permiso para usar la cámara o el micrófono. Permítelo desde el candado de la barra de direcciones y vuelve a intentarlo.';
    case 'NotFoundError':
    case 'OverconstrainedError':
      return 'No encontramos una cámara o un micrófono disponibles. Conecta uno o elige otro dispositivo.';
    case 'NotReadableError':
    case 'AbortError':
      return 'Otra aplicación está usando la cámara o el micrófono. Ciérrala y vuelve a intentarlo.';
    default:
      return 'No pudimos acceder a la cámara y al micrófono.';
  }
}
