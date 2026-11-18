import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { ArchivoDescargado, ExportacionRequest, ExportacionResponse } from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class ExportacionesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/exportaciones`;

  solicitar(request: ExportacionRequest): Observable<ExportacionResponse> {
    return this.http.post<ExportacionResponse>(this.baseUrl, request);
  }

  mias(): Observable<ExportacionResponse[]> {
    return this.http.get<ExportacionResponse[]>(this.baseUrl);
  }

  obtener(id: string): Observable<ExportacionResponse> {
    return this.http.get<ExportacionResponse>(`${this.baseUrl}/${id}`);
  }

  descargar(id: string): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.baseUrl}/${id}/descargar`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'exportacion')));
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
