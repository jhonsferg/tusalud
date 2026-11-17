import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  EventoCalendarioRequest,
  EventoCalendarioResponse,
  MoverEventoRequest,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

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

  actualizar(id: number, request: EventoCalendarioRequest): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}`, request);
  }

  mover(id: number, request: MoverEventoRequest): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}/mover`, request);
  }

  completar(id: number, completado: boolean): Observable<EventoCalendarioResponse> {
    return this.http.put<EventoCalendarioResponse>(`${this.baseUrl}/${id}/completado`, {
      completado,
    });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
