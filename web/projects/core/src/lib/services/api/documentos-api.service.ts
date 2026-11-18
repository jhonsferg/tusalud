import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { ArchivoDescargado, TipoDocumentoPdf } from '../../models';

export function nombreDeDisposicion(disposicion: string | null, porDefecto: string): string {
  if (!disposicion) {
    return porDefecto;
  }
  const codificado = /filename\*=UTF-8''([^;]+)/i.exec(disposicion);
  if (codificado) {
    try {
      return decodeURIComponent(codificado[1]);
    } catch {
      return porDefecto;
    }
  }
  const simple = /filename="?([^";]+)"?/i.exec(disposicion);
  return simple ? simple[1] : porDefecto;
}

export function archivoDeRespuesta(
  respuesta: HttpResponse<Blob>,
  nombrePorDefecto: string,
): ArchivoDescargado {
  return {
    blob: respuesta.body ?? new Blob(),
    nombre: nombreDeDisposicion(respuesta.headers.get('Content-Disposition'), nombrePorDefecto),
  };
}

@Injectable({ providedIn: 'root' })
export class DocumentosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/documentos`;

  generar(tipo: TipoDocumentoPdf, id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/${tipo}/${id}`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, `${tipo}.pdf`)));
  }
}
