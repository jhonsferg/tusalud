import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ActualizarCuentaContableRequest,
  AsientoPendienteResponse,
  AsientoRequest,
  AsientoResponse,
  AsientoResumen,
  BalanceComprobacionResponse,
  ContabilidadAuditoria,
  CuentaContableRequest,
  CuentaContableResponse,
  CuentaTesoreriaRequest,
  CuentaTesoreriaResponse,
  EstadoLineaExtracto,
  EstadoResultadosResponse,
  EstadoSituacionResponse,
  ExtractoLineaResponse,
  ExtractoResponse,
  FiltroAsientos,
  FiltroMovimientosTesoreria,
  FlujoCajaResponse,
  LibroElectronico,
  LibroElectronicoTipo,
  LibroMayorResponse,
  MovimientoTesoreriaRequest,
  MovimientoTesoreriaResponse,
  PeriodoResponse,
  PlantillaAsientoRequest,
  PlantillaAsientoResponse,
  ResumenConciliacion,
  ResumenContabilidad,
  SugerenciaConciliacion,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class ContabilidadApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/contabilidad`;

  resumen(): Observable<ResumenContabilidad> {
    return this.http.get<ResumenContabilidad>(`${this.baseUrl}/resumen`);
  }

  cuentas(soloActivas = false): Observable<CuentaContableResponse[]> {
    return this.http.get<CuentaContableResponse[]>(`${this.baseUrl}/cuentas`, {
      params: toHttpParams({ soloActivas }),
    });
  }

  crearCuenta(request: CuentaContableRequest): Observable<CuentaContableResponse> {
    return this.http.post<CuentaContableResponse>(`${this.baseUrl}/cuentas`, request);
  }

  actualizarCuenta(
    id: string,
    request: ActualizarCuentaContableRequest,
  ): Observable<CuentaContableResponse> {
    return this.http.put<CuentaContableResponse>(`${this.baseUrl}/cuentas/${id}`, request);
  }

  periodos(): Observable<PeriodoResponse[]> {
    return this.http.get<PeriodoResponse[]>(`${this.baseUrl}/periodos`);
  }

  abrirPeriodo(anio: number, mes: number): Observable<PeriodoResponse> {
    return this.http.post<PeriodoResponse>(`${this.baseUrl}/periodos`, { anio, mes });
  }

  cerrarPeriodo(id: string): Observable<PeriodoResponse> {
    return this.http.post<PeriodoResponse>(`${this.baseUrl}/periodos/${id}/cerrar`, {});
  }

  reabrirPeriodo(id: string, motivo: string): Observable<PeriodoResponse> {
    return this.http.post<PeriodoResponse>(`${this.baseUrl}/periodos/${id}/reabrir`, { motivo });
  }

  asientos(filtro: FiltroAsientos = {}): Observable<AsientoResumen[]> {
    return this.http.get<AsientoResumen[]>(`${this.baseUrl}/asientos`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  asiento(id: string): Observable<AsientoResponse> {
    return this.http.get<AsientoResponse>(`${this.baseUrl}/asientos/${id}`);
  }

  crearAsiento(request: AsientoRequest): Observable<AsientoResponse> {
    return this.http.post<AsientoResponse>(`${this.baseUrl}/asientos`, request);
  }

  actualizarAsiento(id: string, request: AsientoRequest): Observable<AsientoResponse> {
    return this.http.put<AsientoResponse>(`${this.baseUrl}/asientos/${id}`, request);
  }

  eliminarAsiento(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/asientos/${id}`);
  }

  contabilizar(id: string): Observable<AsientoResponse> {
    return this.http.post<AsientoResponse>(`${this.baseUrl}/asientos/${id}/contabilizar`, {});
  }

  reversar(id: string, motivo: string): Observable<AsientoResponse> {
    return this.http.post<AsientoResponse>(`${this.baseUrl}/asientos/${id}/reversar`, { motivo });
  }

  plantillas(): Observable<PlantillaAsientoResponse[]> {
    return this.http.get<PlantillaAsientoResponse[]>(`${this.baseUrl}/plantillas`);
  }

  actualizarPlantilla(
    id: string,
    request: PlantillaAsientoRequest,
  ): Observable<PlantillaAsientoResponse> {
    return this.http.put<PlantillaAsientoResponse>(`${this.baseUrl}/plantillas/${id}`, request);
  }

  pendientes(soloAbiertos = true): Observable<AsientoPendienteResponse[]> {
    return this.http.get<AsientoPendienteResponse[]>(`${this.baseUrl}/pendientes`, {
      params: toHttpParams({ soloAbiertos }),
    });
  }

  reprocesar(id: string): Observable<AsientoPendienteResponse> {
    return this.http.post<AsientoPendienteResponse>(
      `${this.baseUrl}/pendientes/${id}/reprocesar`,
      {},
    );
  }

  auditoria(entidad?: string, entidadId?: string): Observable<ContabilidadAuditoria[]> {
    return this.http.get<ContabilidadAuditoria[]>(`${this.baseUrl}/auditoria`, {
      params: toHttpParams({ entidad, entidadId }),
    });
  }

  mayor(cuentaId: string, desde: string, hasta: string): Observable<LibroMayorResponse> {
    return this.http.get<LibroMayorResponse>(`${this.baseUrl}/libros/mayor/${cuentaId}`, {
      params: toHttpParams({ desde, hasta }),
    });
  }

  balanceComprobacion(desde: string, hasta: string): Observable<BalanceComprobacionResponse> {
    return this.http.get<BalanceComprobacionResponse>(
      `${this.baseUrl}/libros/balance-comprobacion`,
      { params: toHttpParams({ desde, hasta }) },
    );
  }

  estadoSituacion(al: string): Observable<EstadoSituacionResponse> {
    return this.http.get<EstadoSituacionResponse>(`${this.baseUrl}/libros/estado-situacion`, {
      params: toHttpParams({ al }),
    });
  }

  estadoResultados(desde: string, hasta: string): Observable<EstadoResultadosResponse> {
    return this.http.get<EstadoResultadosResponse>(`${this.baseUrl}/libros/estado-resultados`, {
      params: toHttpParams({ desde, hasta }),
    });
  }

  libroElectronico(
    libro: LibroElectronicoTipo,
    anio: number,
    mes: number,
  ): Observable<LibroElectronico> {
    return this.http.get<LibroElectronico>(`${this.baseUrl}/libros/electronicos/${libro}`, {
      params: toHttpParams({ anio, mes }),
    });
  }

  cuentasTesoreria(soloActivas = false): Observable<CuentaTesoreriaResponse[]> {
    return this.http.get<CuentaTesoreriaResponse[]>(`${this.baseUrl}/tesoreria/cuentas`, {
      params: toHttpParams({ soloActivas }),
    });
  }

  crearCuentaTesoreria(request: CuentaTesoreriaRequest): Observable<CuentaTesoreriaResponse> {
    return this.http.post<CuentaTesoreriaResponse>(`${this.baseUrl}/tesoreria/cuentas`, request);
  }

  actualizarCuentaTesoreria(
    id: string,
    request: CuentaTesoreriaRequest,
  ): Observable<CuentaTesoreriaResponse> {
    return this.http.put<CuentaTesoreriaResponse>(
      `${this.baseUrl}/tesoreria/cuentas/${id}`,
      request,
    );
  }

  movimientos(filtro: FiltroMovimientosTesoreria = {}): Observable<MovimientoTesoreriaResponse[]> {
    return this.http.get<MovimientoTesoreriaResponse[]>(`${this.baseUrl}/tesoreria/movimientos`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  registrarMovimiento(
    request: MovimientoTesoreriaRequest,
  ): Observable<MovimientoTesoreriaResponse> {
    return this.http.post<MovimientoTesoreriaResponse>(
      `${this.baseUrl}/tesoreria/movimientos`,
      request,
    );
  }

  flujoCaja(semanas = 8): Observable<FlujoCajaResponse> {
    return this.http.get<FlujoCajaResponse>(`${this.baseUrl}/tesoreria/flujo-caja`, {
      params: toHttpParams({ semanas }),
    });
  }

  extractos(cuentaId?: string): Observable<ExtractoResponse[]> {
    return this.http.get<ExtractoResponse[]>(`${this.baseUrl}/tesoreria/conciliacion/extractos`, {
      params: toHttpParams({ cuentaId }),
    });
  }

  importarExtracto(
    cuentaId: string,
    archivo: string,
    contenido: string,
  ): Observable<ExtractoResponse> {
    return this.http.post<ExtractoResponse>(`${this.baseUrl}/tesoreria/conciliacion/extractos`, {
      cuentaId,
      archivo,
      contenido,
    });
  }

  lineasExtracto(
    cuentaId: string,
    estado?: EstadoLineaExtracto,
  ): Observable<ExtractoLineaResponse[]> {
    return this.http.get<ExtractoLineaResponse[]>(
      `${this.baseUrl}/tesoreria/conciliacion/${cuentaId}/lineas`,
      { params: toHttpParams({ estado }) },
    );
  }

  sugerencias(cuentaId: string): Observable<SugerenciaConciliacion[]> {
    return this.http.get<SugerenciaConciliacion[]>(
      `${this.baseUrl}/tesoreria/conciliacion/${cuentaId}/sugerencias`,
    );
  }

  movimientosSinConciliar(cuentaId: string): Observable<MovimientoTesoreriaResponse[]> {
    return this.http.get<MovimientoTesoreriaResponse[]>(
      `${this.baseUrl}/tesoreria/conciliacion/${cuentaId}/sin-conciliar`,
    );
  }

  resumenConciliacion(cuentaId: string): Observable<ResumenConciliacion> {
    return this.http.get<ResumenConciliacion>(
      `${this.baseUrl}/tesoreria/conciliacion/${cuentaId}/resumen`,
    );
  }

  conciliar(lineaId: string, movimientoId: string): Observable<ExtractoLineaResponse> {
    return this.http.post<ExtractoLineaResponse>(
      `${this.baseUrl}/tesoreria/conciliacion/conciliar`,
      { lineaId, movimientoId },
    );
  }

  deshacerConciliacion(lineaId: string): Observable<ExtractoLineaResponse> {
    return this.http.post<ExtractoLineaResponse>(
      `${this.baseUrl}/tesoreria/conciliacion/lineas/${lineaId}/deshacer`,
      {},
    );
  }

  ignorarLinea(lineaId: string): Observable<ExtractoLineaResponse> {
    return this.http.post<ExtractoLineaResponse>(
      `${this.baseUrl}/tesoreria/conciliacion/lineas/${lineaId}/ignorar`,
      {},
    );
  }
}
