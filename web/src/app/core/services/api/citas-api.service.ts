import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { CitaCreateRequest, CitaResponse, RangoFechas } from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class CitasApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/citas`;

  crear(request: CitaCreateRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(this.baseUrl, request);
  }

  misCitas(rango: RangoFechas = {}): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.baseUrl}/mis-citas`, {
      params: toHttpParams({ inicio: rango.inicio, fin: rango.fin }),
    });
  }

  obtener(id: number): Observable<CitaResponse> {
    return this.http.get<CitaResponse>(`${this.baseUrl}/${id}`);
  }

  cancelar(id: number): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/cancelar`, null);
  }

  confirmar(id: number): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/confirmar`, null);
  }

  atender(id: number): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/atender`, null);
  }
}
