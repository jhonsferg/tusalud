import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AlertaEsterilizacion,
  ChecklistRequest,
  CicloEsterilizacion,
  CicloRequest,
  CirugiaDetalle,
  CirugiaResumen,
  EstadoCirugia,
  Esterilizador,
  EsterilizadorRequest,
  EventoCirugia,
  IndicadoresRequest,
  InsumoDisponible,
  LoteEsteril,
  MiCirugia,
  MiembroEquipo,
  NotaOperatoriaRequest,
  ProgramacionRequest,
  Quirofano,
  QuirofanoRequest,
  RegistroAnestesicoRequest,
  RolQuirurgico,
  SetInstrumental,
  SetInstrumentalRequest,
  SolicitudCirugiaRequest,
  TrazabilidadLote,
} from '../../models';

@Injectable({ providedIn: 'root' })
export class QuirofanoApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/quirofano`;

  quirofanos(soloActivos = true): Observable<Quirofano[]> {
    const params = new HttpParams().set('soloActivos', soloActivos);
    return this.http.get<Quirofano[]>(`${this.base}/quirofanos`, { params });
  }

  crearQuirofano(request: QuirofanoRequest): Observable<Quirofano> {
    return this.http.post<Quirofano>(`${this.base}/quirofanos`, request);
  }

  actualizarQuirofano(id: string, request: QuirofanoRequest): Observable<Quirofano> {
    return this.http.put<Quirofano>(`${this.base}/quirofanos/${id}`, request);
  }

  personal(rol: RolQuirurgico): Observable<MiembroEquipo[]> {
    return this.http.get<MiembroEquipo[]>(`${this.base}/personal`, { params: { rol } });
  }

  insumos(q?: string): Observable<InsumoDisponible[]> {
    const params = q ? new HttpParams().set('q', q) : new HttpParams();
    return this.http.get<InsumoDisponible[]>(`${this.base}/insumos`, { params });
  }

  agenda(
    desde: string,
    hasta: string,
    quirofanoId?: string,
    estado?: EstadoCirugia,
  ): Observable<CirugiaResumen[]> {
    let params = new HttpParams().set('desde', desde).set('hasta', hasta);
    if (quirofanoId) {
      params = params.set('quirofanoId', quirofanoId);
    }
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<CirugiaResumen[]>(`${this.base}/cirugias/agenda`, { params });
  }

  mias(): Observable<MiCirugia[]> {
    return this.http.get<MiCirugia[]>(`${this.base}/cirugias/mias`);
  }

  detalle(id: string): Observable<CirugiaDetalle> {
    return this.http.get<CirugiaDetalle>(`${this.base}/cirugias/${id}`);
  }

  historial(id: string): Observable<EventoCirugia[]> {
    return this.http.get<EventoCirugia[]>(`${this.base}/cirugias/${id}/historial`);
  }

  solicitar(request: SolicitudCirugiaRequest): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias`, request);
  }

  programar(id: string, request: ProgramacionRequest): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/programar`, request);
  }

  solicitarConsentimientos(id: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/consentimientos`, {});
  }

  preparar(id: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/preparar`, {});
  }

  checklist(id: string, request: ChecklistRequest): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/checklist`, request);
  }

  iniciar(id: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/iniciar`, {});
  }

  finalizar(id: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/finalizar`, {});
  }

  suspender(id: string, motivo: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/suspender`, { motivo });
  }

  cancelar(id: string, motivo: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/cancelar`, { motivo });
  }

  notaOperatoria(id: string, request: NotaOperatoriaRequest): Observable<CirugiaDetalle> {
    return this.http.put<CirugiaDetalle>(`${this.base}/cirugias/${id}/nota-operatoria`, request);
  }

  registroAnestesico(id: string, request: RegistroAnestesicoRequest): Observable<CirugiaDetalle> {
    return this.http.put<CirugiaDetalle>(
      `${this.base}/cirugias/${id}/registro-anestesico`,
      request,
    );
  }

  registrarInsumo(id: string, productoId: string, cantidad: number): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/insumos`, {
      productoId,
      cantidad,
    });
  }

  usarSet(id: string, loteId: string): Observable<CirugiaDetalle> {
    return this.http.post<CirugiaDetalle>(`${this.base}/cirugias/${id}/sets`, { loteId });
  }
}

@Injectable({ providedIn: 'root' })
export class EsterilizacionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/esterilizacion`;

  esterilizadores(): Observable<Esterilizador[]> {
    return this.http.get<Esterilizador[]>(`${this.base}/esterilizadores`);
  }

  crearEsterilizador(request: EsterilizadorRequest): Observable<Esterilizador> {
    return this.http.post<Esterilizador>(`${this.base}/esterilizadores`, request);
  }

  actualizarEsterilizador(id: string, request: EsterilizadorRequest): Observable<Esterilizador> {
    return this.http.put<Esterilizador>(`${this.base}/esterilizadores/${id}`, request);
  }

  sets(): Observable<SetInstrumental[]> {
    return this.http.get<SetInstrumental[]>(`${this.base}/sets`);
  }

  crearSet(request: SetInstrumentalRequest): Observable<SetInstrumental> {
    return this.http.post<SetInstrumental>(`${this.base}/sets`, request);
  }

  actualizarSet(id: string, request: SetInstrumentalRequest): Observable<SetInstrumental> {
    return this.http.put<SetInstrumental>(`${this.base}/sets/${id}`, request);
  }

  ciclos(limite = 50): Observable<CicloEsterilizacion[]> {
    return this.http.get<CicloEsterilizacion[]>(`${this.base}/ciclos`, {
      params: new HttpParams().set('limite', limite),
    });
  }

  crearCiclo(request: CicloRequest): Observable<CicloEsterilizacion> {
    return this.http.post<CicloEsterilizacion>(`${this.base}/ciclos`, request);
  }

  indicadores(id: string, request: IndicadoresRequest): Observable<CicloEsterilizacion> {
    return this.http.put<CicloEsterilizacion>(`${this.base}/ciclos/${id}/indicadores`, request);
  }

  trazabilidadCiclo(id: string): Observable<TrazabilidadLote[]> {
    return this.http.get<TrazabilidadLote[]>(`${this.base}/ciclos/${id}/trazabilidad`);
  }

  lotes(soloUtilizables = false): Observable<LoteEsteril[]> {
    return this.http.get<LoteEsteril[]>(`${this.base}/lotes`, {
      params: new HttpParams().set('soloUtilizables', soloUtilizables),
    });
  }

  trazabilidadLote(id: string): Observable<TrazabilidadLote> {
    return this.http.get<TrazabilidadLote>(`${this.base}/lotes/${id}/trazabilidad`);
  }

  alertas(): Observable<AlertaEsterilizacion[]> {
    return this.http.get<AlertaEsterilizacion[]>(`${this.base}/alertas`);
  }
}
