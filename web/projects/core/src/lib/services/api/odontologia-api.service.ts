import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CambioOdontograma,
  CambioPiezaRequest,
  ItemPlanRequest,
  Odontograma,
  PagoPlanRequest,
  PlanOdontologico,
  PlanOdontologicoResumen,
  ProcedimientoOdontologico,
  ProcedimientoOdontologicoRequest,
  SesionOdontologicaRequest,
} from '../../models';

@Injectable({ providedIn: 'root' })
export class OdontologiaApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/odontologia`;

  procedimientos(soloActivos = true): Observable<ProcedimientoOdontologico[]> {
    const params = new HttpParams().set('soloActivos', soloActivos);
    return this.http.get<ProcedimientoOdontologico[]>(`${this.base}/procedimientos`, { params });
  }

  crearProcedimiento(
    request: ProcedimientoOdontologicoRequest,
  ): Observable<ProcedimientoOdontologico> {
    return this.http.post<ProcedimientoOdontologico>(`${this.base}/procedimientos`, request);
  }

  actualizarProcedimiento(
    id: string,
    request: ProcedimientoOdontologicoRequest,
  ): Observable<ProcedimientoOdontologico> {
    return this.http.put<ProcedimientoOdontologico>(`${this.base}/procedimientos/${id}`, request);
  }

  odontograma(pacienteId: string): Observable<Odontograma> {
    return this.http.get<Odontograma>(`${this.base}/pacientes/${pacienteId}/odontograma`);
  }

  odontogramaMio(): Observable<Odontograma> {
    return this.http.get<Odontograma>(`${this.base}/mio/odontograma`);
  }

  historial(pacienteId: string, pieza?: number): Observable<CambioOdontograma[]> {
    const params = pieza === undefined ? new HttpParams() : new HttpParams().set('pieza', pieza);
    return this.http.get<CambioOdontograma[]>(
      `${this.base}/pacientes/${pacienteId}/odontograma/historial`,
      { params },
    );
  }

  registrarCambio(pacienteId: string, request: CambioPiezaRequest): Observable<CambioOdontograma> {
    return this.http.post<CambioOdontograma>(
      `${this.base}/pacientes/${pacienteId}/odontograma/cambios`,
      request,
    );
  }

  planesDelPaciente(pacienteId: string): Observable<PlanOdontologicoResumen[]> {
    return this.http.get<PlanOdontologicoResumen[]>(`${this.base}/pacientes/${pacienteId}/planes`);
  }

  planesMios(): Observable<PlanOdontologicoResumen[]> {
    return this.http.get<PlanOdontologicoResumen[]>(`${this.base}/mios/planes`);
  }

  porCobrar(): Observable<PlanOdontologicoResumen[]> {
    return this.http.get<PlanOdontologicoResumen[]>(`${this.base}/planes/por-cobrar`);
  }

  plan(id: string): Observable<PlanOdontologico> {
    return this.http.get<PlanOdontologico>(`${this.base}/planes/${id}`);
  }

  crearPlan(pacienteId: string, notas: string | null): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes`, { pacienteId, notas });
  }

  agregarItem(planId: string, request: ItemPlanRequest): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/items`, request);
  }

  quitarItem(planId: string, itemId: string): Observable<PlanOdontologico> {
    return this.http.delete<PlanOdontologico>(`${this.base}/planes/${planId}/items/${itemId}`);
  }

  vincularConsentimiento(
    planId: string,
    itemId: string,
    consentimientoId: string,
  ): Observable<PlanOdontologico> {
    return this.http.put<PlanOdontologico>(
      `${this.base}/planes/${planId}/items/${itemId}/consentimiento`,
      { consentimientoId },
    );
  }

  presentar(planId: string): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/presentar`, {});
  }

  aceptar(planId: string): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/aceptar`, {});
  }

  rechazar(planId: string, motivo: string): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/rechazar`, { motivo });
  }

  cancelar(planId: string, motivo: string): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/cancelar`, { motivo });
  }

  registrarSesion(
    planId: string,
    request: SesionOdontologicaRequest,
  ): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/sesiones`, request);
  }

  registrarPago(planId: string, request: PagoPlanRequest): Observable<PlanOdontologico> {
    return this.http.post<PlanOdontologico>(`${this.base}/planes/${planId}/pagos`, request);
  }
}
