import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { CitaCreateRequest, CitaResponse, RangoFechas } from '../../models';
import { toHttpParams } from '../../utils/http-params';

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

  obtener(id: string): Observable<CitaResponse> {
    return this.http.get<CitaResponse>(`${this.baseUrl}/${id}`);
  }

  cancelar(id: string, motivo: string): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/cancelar`, { motivo });
  }

  confirmar(id: string): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/confirmar`, null);
  }

  reprogramar(id: string, fechaHora: string): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/reprogramar`, { fechaHora });
  }

  atender(id: string): Observable<CitaResponse> {
    return this.http.put<CitaResponse>(`${this.baseUrl}/${id}/atender`, null);
  }
}
