import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  AjusteRequest,
  AlertasAlmacenResponse,
  FiltroMovimientos,
  IngresoRequest,
  IngresoResponse,
  LoteResponse,
  MovimientoResponse,
  ProductoRequest,
  ProductoResponse,
  ProveedorRequest,
  ProveedorResponse,
  ResumenAlmacenResponse,
  SalidaRequest,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class AlmacenApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/almacen`;

  resumen(): Observable<ResumenAlmacenResponse> {
    return this.http.get<ResumenAlmacenResponse>(`${this.baseUrl}/resumen`);
  }

  alertas(): Observable<AlertasAlmacenResponse> {
    return this.http.get<AlertasAlmacenResponse>(`${this.baseUrl}/alertas`);
  }

  listarProductos(
    filtro: {
      q?: string | null;
      tipo?: string | null;
      bajoStock?: boolean | null;
      activo?: boolean | null;
    } = {},
  ): Observable<ProductoResponse[]> {
    return this.http.get<ProductoResponse[]>(`${this.baseUrl}/productos`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  crearProducto(request: ProductoRequest): Observable<ProductoResponse> {
    return this.http.post<ProductoResponse>(`${this.baseUrl}/productos`, request);
  }

  actualizarProducto(id: number, request: ProductoRequest): Observable<ProductoResponse> {
    return this.http.put<ProductoResponse>(`${this.baseUrl}/productos/${id}`, request);
  }

  listarLotes(productoId: number): Observable<LoteResponse[]> {
    return this.http.get<LoteResponse[]>(`${this.baseUrl}/productos/${productoId}/lotes`);
  }

  listarProveedores(
    filtro: { q?: string | null; activo?: boolean | null } = {},
  ): Observable<ProveedorResponse[]> {
    return this.http.get<ProveedorResponse[]>(`${this.baseUrl}/proveedores`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  crearProveedor(request: ProveedorRequest): Observable<ProveedorResponse> {
    return this.http.post<ProveedorResponse>(`${this.baseUrl}/proveedores`, request);
  }

  actualizarProveedor(id: number, request: ProveedorRequest): Observable<ProveedorResponse> {
    return this.http.put<ProveedorResponse>(`${this.baseUrl}/proveedores/${id}`, request);
  }

  listarIngresos(): Observable<IngresoResponse[]> {
    return this.http.get<IngresoResponse[]>(`${this.baseUrl}/ingresos`);
  }

  registrarIngreso(request: IngresoRequest): Observable<IngresoResponse> {
    return this.http.post<IngresoResponse>(`${this.baseUrl}/ingresos`, request);
  }

  registrarSalida(request: SalidaRequest): Observable<MovimientoResponse[]> {
    return this.http.post<MovimientoResponse[]>(`${this.baseUrl}/salidas`, request);
  }

  registrarAjuste(request: AjusteRequest): Observable<MovimientoResponse> {
    return this.http.post<MovimientoResponse>(`${this.baseUrl}/ajustes`, request);
  }

  listarMovimientos(filtro: FiltroMovimientos = {}): Observable<MovimientoResponse[]> {
    return this.http.get<MovimientoResponse[]>(`${this.baseUrl}/movimientos`, {
      params: toHttpParams({ ...filtro }),
    });
  }
}
