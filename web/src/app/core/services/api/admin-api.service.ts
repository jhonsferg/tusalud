import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  CuponRequest,
  CuponResponse,
  EstadisticasResponse,
  PagoResponse,
  PersonalRequest,
  RangoFechas,
  RolResumenResponse,
  UsuarioAdminResponse,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/admin`;

  estadisticas(): Observable<EstadisticasResponse> {
    return this.http.get<EstadisticasResponse>(`${this.baseUrl}/estadisticas`);
  }

  listarCupones(): Observable<CuponResponse[]> {
    return this.http.get<CuponResponse[]>(`${this.baseUrl}/cupones`);
  }

  crearCupon(request: CuponRequest): Observable<CuponResponse> {
    return this.http.post<CuponResponse>(`${this.baseUrl}/cupones`, request);
  }

  actualizarCupon(id: number, request: CuponRequest): Observable<CuponResponse> {
    return this.http.put<CuponResponse>(`${this.baseUrl}/cupones/${id}`, request);
  }

  eliminarCupon(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/cupones/${id}`);
  }

  listarPagos(rango: RangoFechas = {}): Observable<PagoResponse[]> {
    return this.http.get<PagoResponse[]>(`${this.baseUrl}/pagos`, {
      params: toHttpParams({ inicio: rango.inicio, fin: rango.fin }),
    });
  }

  listarUsuarios(rol?: string | null, q?: string | null): Observable<UsuarioAdminResponse[]> {
    return this.http.get<UsuarioAdminResponse[]>(`${this.baseUrl}/usuarios`, {
      params: toHttpParams({ rol, q }),
    });
  }

  cambiarEstadoUsuario(
    id: number,
    estado: 'ACTIVO' | 'INACTIVO',
  ): Observable<UsuarioAdminResponse> {
    return this.http.put<UsuarioAdminResponse>(`${this.baseUrl}/usuarios/${id}/estado`, { estado });
  }

  crearPersonal(request: PersonalRequest): Observable<UsuarioAdminResponse> {
    return this.http.post<UsuarioAdminResponse>(`${this.baseUrl}/personal`, request);
  }

  listarRoles(): Observable<RolResumenResponse[]> {
    return this.http.get<RolResumenResponse[]>(`${this.baseUrl}/roles`);
  }

  actualizarRoles(id: number, roles: string[]): Observable<UsuarioAdminResponse> {
    return this.http.put<UsuarioAdminResponse>(`${this.baseUrl}/usuarios/${id}/roles`, { roles });
  }
}
