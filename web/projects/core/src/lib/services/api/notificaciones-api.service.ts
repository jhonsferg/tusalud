import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { SKIP_ERROR_NOTIFICATION, SKIP_LOADING } from '../../config/http-context';
import { NotificacionResponse, NotificacionResumen } from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class NotificacionesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/notificaciones`;

  listar(soloNoLeidas = false, limit = 50): Observable<NotificacionResponse[]> {
    return this.http.get<NotificacionResponse[]>(this.baseUrl, {
      params: toHttpParams({ soloNoLeidas, limit }),
    });
  }

  resumen(): Observable<NotificacionResumen> {
    return this.http.get<NotificacionResumen>(`${this.baseUrl}/resumen`, {
      context: new HttpContext().set(SKIP_LOADING, true).set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  marcarLeida(id: string): Observable<NotificacionResponse> {
    return this.http.put<NotificacionResponse>(`${this.baseUrl}/${id}/leida`, null);
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/leidas`, null);
  }

  eliminar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
