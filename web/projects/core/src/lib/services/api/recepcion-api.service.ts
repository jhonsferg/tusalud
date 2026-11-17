import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CitaHoyResponse,
  CitaMostradorRequest,
  CitaResponse,
  PacienteMostradorRequest,
  PacienteMostradorResponse,
  PacienteRegistradoResponse,
  PantallaTurno,
  ResumenRecepcionResponse,
  TurnoRequest,
  TurnoResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class RecepcionApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/recepcion`;

  resumen(): Observable<ResumenRecepcionResponse> {
    return this.http.get<ResumenRecepcionResponse>(`${this.baseUrl}/resumen`);
  }

  citasHoy(q?: string | null): Observable<CitaHoyResponse[]> {
    return this.http.get<CitaHoyResponse[]>(`${this.baseUrl}/citas-hoy`, {
      params: toHttpParams({ q }),
    });
  }

  registrarLlegada(citaId: string): Observable<TurnoResponse> {
    return this.http.post<TurnoResponse>(`${this.baseUrl}/citas/${citaId}/llegada`, null);
  }

  crearTurno(request: TurnoRequest): Observable<TurnoResponse> {
    return this.http.post<TurnoResponse>(`${this.baseUrl}/turnos`, request);
  }

  cola(estado?: string | null, medicoId?: string | null): Observable<TurnoResponse[]> {
    return this.http.get<TurnoResponse[]>(`${this.baseUrl}/cola`, {
      params: toHttpParams({ estado, medicoId }),
    });
  }

  cambiarTurno(
    id: string,
    accion: 'llamar' | 'iniciar' | 'finalizar' | 'ausente' | 'cancelar',
  ): Observable<TurnoResponse> {
    return this.http.put<TurnoResponse>(`${this.baseUrl}/turnos/${id}/${accion}`, null);
  }

  pantalla(): Observable<PantallaTurno[]> {
    return this.http.get<PantallaTurno[]>(`${this.baseUrl}/pantalla`);
  }

  buscarPacientes(q: string): Observable<PacienteMostradorResponse[]> {
    return this.http.get<PacienteMostradorResponse[]>(`${this.baseUrl}/pacientes`, {
      params: toHttpParams({ q }),
    });
  }

  registrarPaciente(request: PacienteMostradorRequest): Observable<PacienteRegistradoResponse> {
    return this.http.post<PacienteRegistradoResponse>(`${this.baseUrl}/pacientes`, request);
  }

  reservarCita(request: CitaMostradorRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(`${this.baseUrl}/citas`, request);
  }
}
