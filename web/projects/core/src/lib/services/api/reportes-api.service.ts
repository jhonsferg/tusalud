import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  FiltroReporte,
  ReporteCalidadResponse,
  ReporteEjecutivoResponse,
  ReporteFinancieroResponse,
  ReporteOperativoResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class ReportesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/reportes`;

  ejecutivo(filtro: FiltroReporte): Observable<ReporteEjecutivoResponse> {
    return this.http.get<ReporteEjecutivoResponse>(`${this.baseUrl}/ejecutivo`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  financiero(filtro: FiltroReporte): Observable<ReporteFinancieroResponse> {
    return this.http.get<ReporteFinancieroResponse>(`${this.baseUrl}/financiero`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  operativo(filtro: FiltroReporte): Observable<ReporteOperativoResponse> {
    return this.http.get<ReporteOperativoResponse>(`${this.baseUrl}/operativo`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  calidad(filtro: FiltroReporte): Observable<ReporteCalidadResponse> {
    return this.http.get<ReporteCalidadResponse>(`${this.baseUrl}/calidad`, {
      params: toHttpParams({ ...filtro }),
    });
  }
}
