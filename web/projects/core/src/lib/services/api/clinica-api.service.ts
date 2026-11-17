import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '../../config/http-context';
import {
  AtencionRequest,
  AtencionResponse,
  CompartidoResponse,
  CompartirPacienteRequest,
  DatosMedicosRequest,
  HistoriaClinicaResponse,
  PacienteClinicoResponse,
  PacienteResumenResponse,
  ValoracionRequest,
  ValoracionResponse,
  ValoracionesMedicoResponse,
} from '../../models';

@Injectable({ providedIn: 'root' })
export class ClinicaApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  obtenerAtencion(citaId: string): Observable<AtencionResponse> {
    return this.http.get<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  registrarAtencion(citaId: string, request: AtencionRequest): Observable<AtencionResponse> {
    return this.http.post<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, request);
  }

  actualizarAtencion(citaId: string, request: AtencionRequest): Observable<AtencionResponse> {
    return this.http.put<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, request);
  }

  miHistoria(): Observable<HistoriaClinicaResponse> {
    return this.http.get<HistoriaClinicaResponse>(`${this.apiUrl}/historia-clinica`);
  }

  actualizarDatosMedicos(
    pacienteId: string,
    request: DatosMedicosRequest,
  ): Observable<PacienteClinicoResponse> {
    return this.http.put<PacienteClinicoResponse>(
      `${this.apiUrl}/pacientes/${pacienteId}/datos-medicos`,
      request,
    );
  }

  historiaDePaciente(pacienteId: string): Observable<HistoriaClinicaResponse> {
    return this.http.get<HistoriaClinicaResponse>(
      `${this.apiUrl}/pacientes/${pacienteId}/historia-clinica`,
    );
  }

  listarCompartidos(pacienteId: string): Observable<CompartidoResponse[]> {
    return this.http.get<CompartidoResponse[]>(
      `${this.apiUrl}/pacientes/${pacienteId}/compartidos`,
    );
  }

  compartirPaciente(
    pacienteId: string,
    request: CompartirPacienteRequest,
  ): Observable<CompartidoResponse> {
    return this.http.post<CompartidoResponse>(
      `${this.apiUrl}/pacientes/${pacienteId}/compartir`,
      request,
    );
  }

  dejarDeCompartir(pacienteId: string, medicoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/pacientes/${pacienteId}/compartir/${medicoId}`);
  }

  misPacientes(): Observable<PacienteResumenResponse[]> {
    return this.http.get<PacienteResumenResponse[]>(`${this.apiUrl}/medicos/me/pacientes`);
  }

  obtenerValoracion(citaId: string): Observable<ValoracionResponse> {
    return this.http.get<ValoracionResponse>(`${this.apiUrl}/citas/${citaId}/valoracion`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  valorar(citaId: string, request: ValoracionRequest): Observable<ValoracionResponse> {
    return this.http.post<ValoracionResponse>(`${this.apiUrl}/citas/${citaId}/valoracion`, request);
  }

  valoracionesDeMedico(medicoId: string): Observable<ValoracionesMedicoResponse> {
    return this.http.get<ValoracionesMedicoResponse>(
      `${this.apiUrl}/medicos/${medicoId}/valoraciones`,
    );
  }
}
