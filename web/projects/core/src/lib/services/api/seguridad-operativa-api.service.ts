import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  EstadoSistemaResponse,
  FiltroEventosSeguridad,
  PaginaEventosSeguridadResponse,
  ResumenSeguridadResponse,
  UsoAlmacenamientoResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class SeguridadOperativaApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/admin`;

  eventos(filtro: FiltroEventosSeguridad = {}): Observable<PaginaEventosSeguridadResponse> {
    return this.http.get<PaginaEventosSeguridadResponse>(`${this.baseUrl}/seguridad/eventos`, {
      params: toHttpParams({
        tipo: filtro.tipo,
        severidad: filtro.severidad,
        desde: filtro.desde,
        hasta: filtro.hasta,
        usuarioId: filtro.usuarioId,
        pagina: filtro.pagina ?? 0,
        tamano: filtro.tamano ?? 20,
      }),
    });
  }

  resumen(): Observable<ResumenSeguridadResponse> {
    return this.http.get<ResumenSeguridadResponse>(`${this.baseUrl}/seguridad/resumen`);
  }

  estadoSistema(): Observable<EstadoSistemaResponse> {
    return this.http.get<EstadoSistemaResponse>(`${this.baseUrl}/sistema/estado`);
  }

  usoAlmacenamiento(): Observable<UsoAlmacenamientoResponse> {
    return this.http.get<UsoAlmacenamientoResponse>(`${this.baseUrl}/almacenamiento/uso`);
  }
}
