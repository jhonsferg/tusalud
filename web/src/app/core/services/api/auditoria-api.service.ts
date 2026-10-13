import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { CitaResponse, CitasPorEstadoResponse, RangoFechas } from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class AuditoriaApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/admin/auditoria`;

  citasPorEstado(): Observable<CitasPorEstadoResponse[]> {
    return this.http.get<CitasPorEstadoResponse[]>(`${this.baseUrl}/citas-por-estado`);
  }

  citas(rango: RangoFechas = {}): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.baseUrl}/citas`, {
      params: toHttpParams({ inicio: rango.inicio, fin: rango.fin }),
    });
  }
}
