import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AjusteAsistenciaRequest,
  ArchivoPlanillaResponse,
  AsistenciaDiaResponse,
  BoletaPagoResponse,
  ConceptoPlanillaRequest,
  ConceptoPlanillaResponse,
  ConsolidacionResponse,
  ContratoLaboralRequest,
  ContratoLaboralResponse,
  EmpleadoConceptoRequest,
  EmpleadoConceptoResponse,
  EstadoContrato,
  MarcacionManualRequest,
  MarcacionResponse,
  MiBoletaResumenResponse,
  ParametroPlanillaRequest,
  ParametroPlanillaResponse,
  PlanillaAuditoriaResponse,
  PlanillaDetalleResponse,
  PlanillaResponse,
  ResumenAsistenciaResponse,
  ResumenPlanillaResponse,
  TerminalAsistenciaResponse,
  TerminalCreadaResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class PlanillaApiService {
  private readonly http = inject(HttpClient);
  private readonly planilla = `${inject(API_URL)}/planilla`;
  private readonly asistencia = `${inject(API_URL)}/asistencia`;

  resumen(): Observable<ResumenPlanillaResponse> {
    return this.http.get<ResumenPlanillaResponse>(`${this.planilla}/resumen`);
  }

  planillas(): Observable<PlanillaResponse[]> {
    return this.http.get<PlanillaResponse[]>(`${this.planilla}/planillas`);
  }

  planillaDetalle(id: string): Observable<PlanillaDetalleResponse> {
    return this.http.get<PlanillaDetalleResponse>(`${this.planilla}/planillas/${id}`);
  }

  calcular(anio: number, mes: number): Observable<PlanillaResponse> {
    return this.http.post<PlanillaResponse>(`${this.planilla}/planillas/calcular`, { anio, mes });
  }

  devolver(id: string, motivo: string): Observable<PlanillaResponse> {
    return this.http.post<PlanillaResponse>(`${this.planilla}/planillas/${id}/devolver`, {
      motivo,
    });
  }

  aprobar(id: string): Observable<PlanillaResponse> {
    return this.http.post<PlanillaResponse>(`${this.planilla}/planillas/${id}/aprobar`, {});
  }

  pagar(id: string, fechaPago: string, referencia: string): Observable<PlanillaResponse> {
    return this.http.post<PlanillaResponse>(`${this.planilla}/planillas/${id}/pagar`, {
      fechaPago,
      referencia,
    });
  }

  cerrar(id: string): Observable<PlanillaResponse> {
    return this.http.post<PlanillaResponse>(`${this.planilla}/planillas/${id}/cerrar`, {});
  }

  archivoBancario(id: string): Observable<ArchivoPlanillaResponse> {
    return this.http.get<ArchivoPlanillaResponse>(
      `${this.planilla}/planillas/${id}/archivo-bancario`,
    );
  }

  plame(id: string): Observable<ArchivoPlanillaResponse> {
    return this.http.get<ArchivoPlanillaResponse>(`${this.planilla}/planillas/${id}/plame`);
  }

  boleta(id: string): Observable<BoletaPagoResponse> {
    return this.http.get<BoletaPagoResponse>(`${this.planilla}/boletas/${id}`);
  }

  misBoletas(): Observable<MiBoletaResumenResponse[]> {
    return this.http.get<MiBoletaResumenResponse[]>(`${this.planilla}/mis-boletas`);
  }

  auditoria(entidad?: string): Observable<PlanillaAuditoriaResponse[]> {
    return this.http.get<PlanillaAuditoriaResponse[]>(`${this.planilla}/auditoria`, {
      params: toHttpParams({ entidad }),
    });
  }

  parametros(vigentes: boolean, codigo?: string): Observable<ParametroPlanillaResponse[]> {
    return this.http.get<ParametroPlanillaResponse[]>(`${this.planilla}/parametros`, {
      params: toHttpParams({ vigentes, codigo }),
    });
  }

  nuevoParametro(request: ParametroPlanillaRequest): Observable<ParametroPlanillaResponse> {
    return this.http.post<ParametroPlanillaResponse>(`${this.planilla}/parametros`, request);
  }

  validarParametro(id: string): Observable<ParametroPlanillaResponse> {
    return this.http.post<ParametroPlanillaResponse>(
      `${this.planilla}/parametros/${id}/validar`,
      {},
    );
  }

  conceptos(): Observable<ConceptoPlanillaResponse[]> {
    return this.http.get<ConceptoPlanillaResponse[]>(`${this.planilla}/conceptos`);
  }

  crearConcepto(request: ConceptoPlanillaRequest): Observable<ConceptoPlanillaResponse> {
    return this.http.post<ConceptoPlanillaResponse>(`${this.planilla}/conceptos`, request);
  }

  actualizarConcepto(
    id: string,
    request: ConceptoPlanillaRequest,
  ): Observable<ConceptoPlanillaResponse> {
    return this.http.put<ConceptoPlanillaResponse>(`${this.planilla}/conceptos/${id}`, request);
  }

  contratos(
    empleadoId?: string,
    estado?: EstadoContrato | null,
  ): Observable<ContratoLaboralResponse[]> {
    return this.http.get<ContratoLaboralResponse[]>(`${this.planilla}/contratos`, {
      params: toHttpParams({ empleadoId, estado }),
    });
  }

  crearContrato(request: ContratoLaboralRequest): Observable<ContratoLaboralResponse> {
    return this.http.post<ContratoLaboralResponse>(`${this.planilla}/contratos`, request);
  }

  actualizarContrato(
    id: string,
    request: ContratoLaboralRequest,
  ): Observable<ContratoLaboralResponse> {
    return this.http.put<ContratoLaboralResponse>(`${this.planilla}/contratos/${id}`, request);
  }

  finalizarContrato(id: string, fechaFin: string): Observable<ContratoLaboralResponse> {
    return this.http.post<ContratoLaboralResponse>(`${this.planilla}/contratos/${id}/finalizar`, {
      fechaFin,
    });
  }

  conceptosDeEmpleado(empleadoId: string): Observable<EmpleadoConceptoResponse[]> {
    return this.http.get<EmpleadoConceptoResponse[]>(
      `${this.planilla}/empleados/${empleadoId}/conceptos`,
    );
  }

  asignarConcepto(
    empleadoId: string,
    request: EmpleadoConceptoRequest,
  ): Observable<EmpleadoConceptoResponse> {
    return this.http.post<EmpleadoConceptoResponse>(
      `${this.planilla}/empleados/${empleadoId}/conceptos`,
      request,
    );
  }

  quitarConcepto(empleadoId: string, asignacionId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.planilla}/empleados/${empleadoId}/conceptos/${asignacionId}`,
    );
  }

  marcaciones(desde: string, hasta: string, empleadoId?: string): Observable<MarcacionResponse[]> {
    return this.http.get<MarcacionResponse[]>(`${this.asistencia}/marcaciones`, {
      params: toHttpParams({ desde, hasta, empleadoId }),
    });
  }

  registrarMarcacion(request: MarcacionManualRequest): Observable<MarcacionResponse> {
    return this.http.post<MarcacionResponse>(`${this.asistencia}/marcaciones`, request);
  }

  diasAsistencia(
    desde: string,
    hasta: string,
    empleadoId?: string,
  ): Observable<AsistenciaDiaResponse[]> {
    return this.http.get<AsistenciaDiaResponse[]>(`${this.asistencia}/dias`, {
      params: toHttpParams({ desde, hasta, empleadoId }),
    });
  }

  consolidar(
    desde: string,
    hasta: string,
    empleadoId?: string | null,
  ): Observable<ConsolidacionResponse> {
    return this.http.post<ConsolidacionResponse>(`${this.asistencia}/consolidar`, {
      desde,
      hasta,
      empleadoId: empleadoId ?? null,
    });
  }

  ajustarDia(id: string, request: AjusteAsistenciaRequest): Observable<AsistenciaDiaResponse> {
    return this.http.put<AsistenciaDiaResponse>(`${this.asistencia}/dias/${id}/ajuste`, request);
  }

  terminales(): Observable<TerminalAsistenciaResponse[]> {
    return this.http.get<TerminalAsistenciaResponse[]>(`${this.asistencia}/terminales`);
  }

  crearTerminal(nombre: string): Observable<TerminalCreadaResponse> {
    return this.http.post<TerminalCreadaResponse>(`${this.asistencia}/terminales`, { nombre });
  }

  estadoTerminal(id: string, activo: boolean): Observable<TerminalAsistenciaResponse> {
    return this.http.patch<TerminalAsistenciaResponse>(
      `${this.asistencia}/terminales/${id}/estado`,
      {},
      { params: toHttpParams({ activo }) },
    );
  }

  miAsistencia(desde: string, hasta: string): Observable<ResumenAsistenciaResponse> {
    return this.http.get<ResumenAsistenciaResponse>(`${this.asistencia}/mi-asistencia`, {
      params: toHttpParams({ desde, hasta }),
    });
  }
}
