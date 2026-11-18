import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ArchivosApiService } from './api/archivos-api.service';

export interface ArchivoBlob {
  readonly url: string;
  readonly tipo: string;
  liberar(): void;
}

@Injectable({ providedIn: 'root' })
export class ArchivoBlobService {
  private readonly api = inject(ArchivosApiService);

  cargar(id: string): Observable<ArchivoBlob> {
    return this.api.descargar(id).pipe(map((blob) => this.crear(blob)));
  }

  crear(blob: Blob): ArchivoBlob {
    const url = URL.createObjectURL(blob);
    let liberada = false;
    return {
      url,
      tipo: blob.type,
      liberar: () => {
        if (!liberada) {
          liberada = true;
          URL.revokeObjectURL(url);
        }
      },
    };
  }
}
