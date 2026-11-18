import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

import { ArchivoDescargado } from '../models';

const LIBERAR_TRAS_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class DescargaService {
  private readonly documento = inject(DOCUMENT);

  guardar(archivo: ArchivoDescargado): void {
    const url = URL.createObjectURL(archivo.blob);
    const enlace = this.documento.createElement('a');
    enlace.href = url;
    enlace.download = archivo.nombre;
    enlace.rel = 'noopener';
    enlace.style.display = 'none';
    this.documento.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), LIBERAR_TRAS_MS);
  }

  abrir(archivo: ArchivoDescargado): void {
    const url = URL.createObjectURL(archivo.blob);
    const ventana = this.documento.defaultView?.open(url, '_blank', 'noopener');
    if (!ventana) {
      URL.revokeObjectURL(url);
      this.guardar(archivo);
      return;
    }
    setTimeout(() => URL.revokeObjectURL(url), LIBERAR_TRAS_MS);
  }
}
