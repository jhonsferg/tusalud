import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  AccionEventoRequest,
  AnalisisEventoRequest,
  ArchivoDescargado,
  CierreEventoRequest,
  EstadoEvento,
  EventoDetalle,
  EventoRequest,
  EventoResumen,
  GuiaDetalle,
  GuiaRequest,
  GuiaResumen,
  TableroSeguridad,
} from '../../models';
import { archivoDeRespuesta } from './documentos-api.service';

@Injectable({ providedIn: 'root' })
export class SeguridadPacienteApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/seguridad-paciente`;

  notificar(request: EventoRequest): Observable<EventoResumen> {
    return this.http.post<EventoResumen>(`${this.base}/eventos`, request);
  }

  mios(): Observable<EventoResumen[]> {
    return this.http.get<EventoResumen[]>(`${this.base}/eventos/mios`);
  }

  listar(estado?: EstadoEvento | ''): Observable<EventoResumen[]> {
    const params = estado ? new HttpParams().set('estado', estado) : new HttpParams();
    return this.http.get<EventoResumen[]>(`${this.base}/eventos`, { params });
  }

  detalle(id: string): Observable<EventoDetalle> {
    return this.http.get<EventoDetalle>(`${this.base}/eventos/${id}`);
  }

  analizar(id: string, request: AnalisisEventoRequest): Observable<EventoDetalle> {
    return this.http.post<EventoDetalle>(`${this.base}/eventos/${id}/analisis`, request);
  }

  agregarAccion(id: string, request: AccionEventoRequest): Observable<EventoDetalle> {
    return this.http.post<EventoDetalle>(`${this.base}/eventos/${id}/acciones`, request);
  }

  completarAccion(accionId: string): Observable<EventoDetalle> {
    return this.http.post<EventoDetalle>(`${this.base}/acciones/${accionId}/completar`, {});
  }

  cerrar(id: string, request: CierreEventoRequest): Observable<EventoDetalle> {
    return this.http.post<EventoDetalle>(`${this.base}/eventos/${id}/cierre`, request);
  }

  tablero(): Observable<TableroSeguridad> {
    return this.http.get<TableroSeguridad>(`${this.base}/tablero`);
  }

  exportar(): Observable<ArchivoDescargado> {
    return this.http
      .get(`${this.base}/eventos/exportacion`, { responseType: 'blob', observe: 'response' })
      .pipe(map((respuesta) => archivoDeRespuesta(respuesta, 'eventos-adversos.xlsx')));
  }

  guias(q = '', soloVigentes = true): Observable<GuiaResumen[]> {
    let params = new HttpParams().set('soloVigentes', soloVigentes);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<GuiaResumen[]>(`${this.base}/guias`, { params });
  }

  guia(id: string): Observable<GuiaDetalle> {
    return this.http.get<GuiaDetalle>(`${this.base}/guias/${id}`);
  }

  guiasSugeridas(codigos: string[]): Observable<GuiaResumen[]> {
    const params = new HttpParams().set('codigos', codigos.join(','));
    return this.http.get<GuiaResumen[]>(`${this.base}/guias/sugeridas`, { params });
  }

  crearGuia(request: GuiaRequest): Observable<GuiaDetalle> {
    return this.http.post<GuiaDetalle>(`${this.base}/guias`, request);
  }

  nuevaVersionGuia(id: string, request: GuiaRequest): Observable<GuiaDetalle> {
    return this.http.post<GuiaDetalle>(`${this.base}/guias/${id}/versiones`, request);
  }
}
