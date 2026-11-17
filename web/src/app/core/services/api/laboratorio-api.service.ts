import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  ExamenRequest,
  ExamenResponse,
  FiltroOrdenes,
  OrdenRequest,
  OrdenResponse,
  ResultadosRequest,
  ResumenLaboratorioResponse,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class LaboratorioApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/laboratorio`;

  resumen(): Observable<ResumenLaboratorioResponse> {
    return this.http.get<ResumenLaboratorioResponse>(`${this.baseUrl}/resumen`);
  }

  listarExamenes(
    filtro: {
      q?: string | null;
      categoria?: string | null;
      activo?: boolean | null;
    } = {},
  ): Observable<ExamenResponse[]> {
    return this.http.get<ExamenResponse[]>(`${this.baseUrl}/examenes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  crearExamen(request: ExamenRequest): Observable<ExamenResponse> {
    return this.http.post<ExamenResponse>(`${this.baseUrl}/examenes`, request);
  }

  actualizarExamen(id: string, request: ExamenRequest): Observable<ExamenResponse> {
    return this.http.put<ExamenResponse>(`${this.baseUrl}/examenes/${id}`, request);
  }

  listarOrdenes(filtro: FiltroOrdenes = {}): Observable<OrdenResponse[]> {
    return this.http.get<OrdenResponse[]>(`${this.baseUrl}/ordenes`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  obtenerOrden(id: string): Observable<OrdenResponse> {
    return this.http.get<OrdenResponse>(`${this.baseUrl}/ordenes/${id}`);
  }

  crearOrden(request: OrdenRequest): Observable<OrdenResponse> {
    return this.http.post<OrdenResponse>(`${this.baseUrl}/ordenes`, request);
  }

  tomarMuestra(id: string): Observable<OrdenResponse> {
    return this.http.put<OrdenResponse>(`${this.baseUrl}/ordenes/${id}/muestra`, null);
  }

  registrarResultados(
    id: string,
    itemId: string,
    request: ResultadosRequest,
  ): Observable<OrdenResponse> {
    return this.http.put<OrdenResponse>(
      `${this.baseUrl}/ordenes/${id}/items/${itemId}/resultados`,
      request,
    );
  }

  validar(id: string): Observable<OrdenResponse> {
    return this.http.put<OrdenResponse>(`${this.baseUrl}/ordenes/${id}/validar`, null);
  }

  cancelar(id: string, motivo?: string | null): Observable<OrdenResponse> {
    return this.http.put<OrdenResponse>(`${this.baseUrl}/ordenes/${id}/cancelar`, { motivo });
  }

  misOrdenes(): Observable<OrdenResponse[]> {
    return this.http.get<OrdenResponse[]>(`${this.baseUrl}/mis-ordenes`);
  }
}
