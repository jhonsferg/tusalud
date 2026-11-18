import { HttpClient, HttpEventType, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, filter, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { ArchivoResponse, ProgresoSubida, PropositoArchivo } from '../../models';

@Injectable({ providedIn: 'root' })
export class ArchivosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/archivos`;

  subir(
    archivo: File,
    proposito: PropositoArchivo,
    referenciaId?: string | null,
    referenciaTipo?: string | null,
  ): Observable<ProgresoSubida> {
    const formulario = new FormData();
    formulario.append('archivo', archivo, archivo.name);
    formulario.append('proposito', proposito);
    if (referenciaId) {
      formulario.append('referenciaId', referenciaId);
    }
    if (referenciaTipo) {
      formulario.append('referenciaTipo', referenciaTipo);
    }
    return this.http
      .post<ArchivoResponse>(this.baseUrl, formulario, { observe: 'events', reportProgress: true })
      .pipe(
        map((evento) => {
          if (evento.type === HttpEventType.UploadProgress) {
            const total = evento.total ?? archivo.size;
            return {
              progreso: total > 0 ? Math.round((evento.loaded / total) * 100) : 0,
              archivo: null,
            };
          }
          if (evento.type === HttpEventType.Response) {
            return { progreso: 100, archivo: evento.body };
          }
          return null;
        }),
        filter((estado): estado is ProgresoSubida => estado !== null),
      );
  }

  descargar(id: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}`, { responseType: 'blob' });
  }

  metadatos(id: string): Observable<ArchivoResponse> {
    return this.http.get<ArchivoResponse>(`${this.baseUrl}/${id}/metadatos`);
  }

  listar(referenciaTipo: string, referenciaId: string): Observable<ArchivoResponse[]> {
    const params = new HttpParams()
      .set('referenciaTipo', referenciaTipo)
      .set('referenciaId', referenciaId);
    return this.http.get<ArchivoResponse[]>(this.baseUrl, { params });
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
