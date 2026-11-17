import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  EventoCalendarioRequest,
  EventoCalendarioResponse,
  MoverEventoRequest,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class CalendarioApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/calendario/eventos`;

  listar(inicio: string, fin: string): Observable<EventoCalendarioResponse[]> {
    return this.http.get<EventoCalendarioResponse[]>(this.baseUrl, {
      params: toHttpParams({ inicio, fin }),
    });
  }

  crear(request: EventoCalendarioRequest): Observable<EventoCalendarioResponse> {
    return this.http.post<EventoCalendarioResponse>(this.baseUrl, request);
  }

  actualizar(id: string, request: EventoCalendarioRequest): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}`, request);
  }

  mover(id: string, request: MoverEventoRequest): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}/mover`, request);
  }

  completar(id: string, completado: boolean): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}/completado`, {
      completado,
    });
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
