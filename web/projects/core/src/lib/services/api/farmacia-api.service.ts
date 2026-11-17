import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CatalogoProductoResponse,
  DispensacionRequest,
  DispensacionResponse,
  RecetaFarmaciaResponse,
  ResumenFarmaciaResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class FarmaciaApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/farmacia`;

  resumen(): Observable<ResumenFarmaciaResponse> {
    return this.http.get<ResumenFarmaciaResponse>(`${this.baseUrl}/resumen`);
  }

  catalogo(q?: string | null): Observable<CatalogoProductoResponse[]> {
    return this.http.get<CatalogoProductoResponse[]>(`${this.baseUrl}/catalogo`, {
      params: toHttpParams({ q }),
    });
  }

  recetas(estado?: string | null, q?: string | null): Observable<RecetaFarmaciaResponse[]> {
    return this.http.get<RecetaFarmaciaResponse[]>(`${this.baseUrl}/recetas`, {
      params: toHttpParams({ estado, q }),
    });
  }

  dispensar(request: DispensacionRequest): Observable<DispensacionResponse> {
    return this.http.post<DispensacionResponse>(`${this.baseUrl}/dispensaciones`, request);
  }

  listarDispensaciones(
    filtro: {
      desde?: string | null;
      hasta?: string | null;
      q?: string | null;
    } = {},
  ): Observable<DispensacionResponse[]> {
    return this.http.get<DispensacionResponse[]>(`${this.baseUrl}/dispensaciones`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  obtenerDispensacion(id: string): Observable<DispensacionResponse> {
    return this.http.get<DispensacionResponse>(`${this.baseUrl}/dispensaciones/${id}`);
  }

  anular(id: string): Observable<DispensacionResponse> {
    return this.http.put<DispensacionResponse>(`${this.baseUrl}/dispensaciones/${id}/anular`, null);
  }

  misDispensaciones(): Observable<DispensacionResponse[]> {
    return this.http.get<DispensacionResponse[]>(`${this.baseUrl}/mis-dispensaciones`);
  }
}
