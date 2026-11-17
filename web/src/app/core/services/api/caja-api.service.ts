import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '@tusalud/app/core/config/http-context';
import {
  CierreCajaRequest,
  CobroRequest,
  ComprobanteResponse,
  PendienteCobroResponse,
  ReporteIngresosResponse,
  ResumenCajaResponse,
  SesionCajaResponse,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class CajaApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/caja`;

  actual(): Observable<SesionCajaResponse> {
    return this.http.get<SesionCajaResponse>(`${this.baseUrl}/actual`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  abrir(montoApertura: number): Observable<SesionCajaResponse> {
    return this.http.post<SesionCajaResponse>(`${this.baseUrl}/aperturas`, { montoApertura });
  }

  cerrar(request: CierreCajaRequest): Observable<SesionCajaResponse> {
    return this.http.post<SesionCajaResponse>(`${this.baseUrl}/cierre`, request);
  }

  resumen(): Observable<ResumenCajaResponse> {
    return this.http.get<ResumenCajaResponse>(`${this.baseUrl}/resumen`);
  }

  historial(): Observable<SesionCajaResponse[]> {
    return this.http.get<SesionCajaResponse[]>(`${this.baseUrl}/historial`);
  }

  pendientes(q?: string | null): Observable<PendienteCobroResponse[]> {
    return this.http.get<PendienteCobroResponse[]>(`${this.baseUrl}/pendientes`, {
      params: toHttpParams({ q }),
    });
  }

  cobrar(request: CobroRequest): Observable<ComprobanteResponse> {
    return this.http.post<ComprobanteResponse>(`${this.baseUrl}/cobros`, request);
  }

  comprobantes(
    filtro: {
      desde?: string | null;
      hasta?: string | null;
      q?: string | null;
      estado?: string | null;
    } = {},
  ): Observable<ComprobanteResponse[]> {
    return this.http.get<ComprobanteResponse[]>(`${this.baseUrl}/comprobantes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  anular(id: string, motivo: string): Observable<ComprobanteResponse> {
    return this.http.put<ComprobanteResponse>(`${this.baseUrl}/comprobantes/${id}/anular`, {
      motivo,
    });
  }

  reporteIngresos(desde: string, hasta: string): Observable<ReporteIngresosResponse> {
    return this.http.get<ReporteIngresosResponse>(`${this.baseUrl}/reportes/ingresos`, {
      params: toHttpParams({ desde, hasta }),
    });
  }
}
