import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AprobarPreautorizacionRequest,
  AseguradoraRequest,
  AseguradoraResponse,
  CarteraResponse,
  CoberturaPacienteResponse,
  ConvenioRequest,
  ConvenioResponse,
  CuentaAseguradoraResponse,
  EstadoPreautorizacion,
  LiquidacionCrearRequest,
  LiquidacionPagoRequest,
  LiquidacionResponse,
  MiPolizaResponse,
  PolizaEstadoRequest,
  PolizaRequest,
  PolizaResponse,
  PreautorizacionRequest,
  PreautorizacionResponse,
  SegurosResumenResponse,
  VerificacionCoberturaResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class SegurosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/seguros`;

  aseguradoras(): Observable<AseguradoraResponse[]> {
    return this.http.get<AseguradoraResponse[]>(`${this.baseUrl}/aseguradoras`);
  }

  crearAseguradora(request: AseguradoraRequest): Observable<AseguradoraResponse> {
    return this.http.post<AseguradoraResponse>(`${this.baseUrl}/aseguradoras`, request);
  }

  actualizarAseguradora(id: string, request: AseguradoraRequest): Observable<AseguradoraResponse> {
    return this.http.put<AseguradoraResponse>(`${this.baseUrl}/aseguradoras/${id}`, request);
  }

  convenios(): Observable<ConvenioResponse[]> {
    return this.http.get<ConvenioResponse[]>(`${this.baseUrl}/convenios`);
  }

  crearConvenio(request: ConvenioRequest): Observable<ConvenioResponse> {
    return this.http.post<ConvenioResponse>(`${this.baseUrl}/convenios`, request);
  }

  actualizarConvenio(id: string, request: ConvenioRequest): Observable<ConvenioResponse> {
    return this.http.put<ConvenioResponse>(`${this.baseUrl}/convenios/${id}`, request);
  }

  buscarPolizas(q: string): Observable<PolizaResponse[]> {
    return this.http.get<PolizaResponse[]>(`${this.baseUrl}/polizas`, {
      params: toHttpParams({ q }),
    });
  }

  polizasDelPaciente(pacienteId: string): Observable<PolizaResponse[]> {
    return this.http.get<PolizaResponse[]>(`${this.baseUrl}/pacientes/${pacienteId}/polizas`);
  }

  registrarPoliza(request: PolizaRequest): Observable<PolizaResponse> {
    return this.http.post<PolizaResponse>(`${this.baseUrl}/polizas`, request);
  }

  cambiarEstadoPoliza(id: string, request: PolizaEstadoRequest): Observable<PolizaResponse> {
    return this.http.put<PolizaResponse>(`${this.baseUrl}/polizas/${id}/estado`, request);
  }

  verificarPoliza(id: string): Observable<VerificacionCoberturaResponse> {
    return this.http.post<VerificacionCoberturaResponse>(
      `${this.baseUrl}/polizas/${id}/verificaciones`,
      {},
    );
  }

  misPolizas(): Observable<MiPolizaResponse[]> {
    return this.http.get<MiPolizaResponse[]>(`${this.baseUrl}/mis-polizas`);
  }

  coberturaDelPaciente(pacienteId: string): Observable<CoberturaPacienteResponse> {
    return this.http.get<CoberturaPacienteResponse>(
      `${this.baseUrl}/cobertura/pacientes/${pacienteId}`,
    );
  }

  solicitarPreautorizacion(request: PreautorizacionRequest): Observable<PreautorizacionResponse> {
    return this.http.post<PreautorizacionResponse>(`${this.baseUrl}/preautorizaciones`, request);
  }

  preautorizaciones(estado?: EstadoPreautorizacion | null): Observable<PreautorizacionResponse[]> {
    return this.http.get<PreautorizacionResponse[]>(`${this.baseUrl}/preautorizaciones`, {
      params: toHttpParams({ estado }),
    });
  }

  misPreautorizaciones(): Observable<PreautorizacionResponse[]> {
    return this.http.get<PreautorizacionResponse[]>(`${this.baseUrl}/preautorizaciones/mias`);
  }

  aprobarPreautorizacion(
    id: string,
    request: AprobarPreautorizacionRequest,
  ): Observable<PreautorizacionResponse> {
    return this.http.put<PreautorizacionResponse>(
      `${this.baseUrl}/preautorizaciones/${id}/aprobar`,
      request,
    );
  }

  rechazarPreautorizacion(id: string, motivo: string): Observable<PreautorizacionResponse> {
    return this.http.put<PreautorizacionResponse>(
      `${this.baseUrl}/preautorizaciones/${id}/rechazar`,
      { motivo },
    );
  }

  cartera(): Observable<CarteraResponse[]> {
    return this.http.get<CarteraResponse[]>(`${this.baseUrl}/cartera`);
  }

  cuentasDeAseguradora(aseguradoraId: string): Observable<CuentaAseguradoraResponse[]> {
    return this.http.get<CuentaAseguradoraResponse[]>(
      `${this.baseUrl}/aseguradoras/${aseguradoraId}/cuentas`,
    );
  }

  liquidaciones(): Observable<LiquidacionResponse[]> {
    return this.http.get<LiquidacionResponse[]>(`${this.baseUrl}/liquidaciones`);
  }

  cuentasDeLiquidacion(id: string): Observable<CuentaAseguradoraResponse[]> {
    return this.http.get<CuentaAseguradoraResponse[]>(
      `${this.baseUrl}/liquidaciones/${id}/cuentas`,
    );
  }

  crearLiquidacion(request: LiquidacionCrearRequest): Observable<LiquidacionResponse> {
    return this.http.post<LiquidacionResponse>(`${this.baseUrl}/liquidaciones`, request);
  }

  enviarLiquidacion(id: string): Observable<LiquidacionResponse> {
    return this.http.put<LiquidacionResponse>(`${this.baseUrl}/liquidaciones/${id}/enviar`, {});
  }

  registrarPagoLiquidacion(
    id: string,
    request: LiquidacionPagoRequest,
  ): Observable<LiquidacionResponse> {
    return this.http.put<LiquidacionResponse>(`${this.baseUrl}/liquidaciones/${id}/pago`, request);
  }

  resumen(): Observable<SegurosResumenResponse> {
    return this.http.get<SegurosResumenResponse>(`${this.baseUrl}/resumen`);
  }
}
