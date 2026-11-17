import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '@tusalud/app/core/config/http-context';
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
} from '@tusalud/app/core/models';

@Injectable({ providedIn: 'root' })
export class ClinicaApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  obtenerAtencion(citaId: number): Observable<AtencionResponse> {
    return this.http.get<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  registrarAtencion(citaId: number, request: AtencionRequest): Observable<AtencionResponse> {
    return this.http.post<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, request);
  }

  actualizarAtencion(citaId: number, request: AtencionRequest): Observable<AtencionResponse> {
    return this.http.put<AtencionResponse>(`${this.apiUrl}/citas/${citaId}/atencion`, request);
  }

  miHistoria(): Observable<HistoriaClinicaResponse> {
    return this.http.get<HistoriaClinicaResponse>(`${this.apiUrl}/historia-clinica`);
  }

  actualizarDatosMedicos(request: DatosMedicosRequest): Observable<PacienteClinicoResponse> {
    return this.http.put<PacienteClinicoResponse>(
      `${this.apiUrl}/historia-clinica/datos-medicos`,
      request,
    );
  }

  historiaDePaciente(pacienteId: number): Observable<HistoriaClinicaResponse> {
    return this.http.get<HistoriaClinicaResponse>(
      `${this.apiUrl}/pacientes/${pacienteId}/historia-clinica`,
    );
  }

  listarCompartidos(pacienteId: number): Observable<CompartidoResponse[]> {
    return this.http.get<CompartidoResponse[]>(
      `${this.apiUrl}/pacientes/${pacienteId}/compartidos`,
    );
  }

  compartirPaciente(
    pacienteId: number,
    request: CompartirPacienteRequest,
  ): Observable<CompartidoResponse> {
    return this.http.post<CompartidoResponse>(
      `${this.apiUrl}/pacientes/${pacienteId}/compartir`,
      request,
    );
  }

  dejarDeCompartir(pacienteId: number, medicoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/pacientes/${pacienteId}/compartir/${medicoId}`);
  }

  misPacientes(): Observable<PacienteResumenResponse[]> {
    return this.http.get<PacienteResumenResponse[]>(`${this.apiUrl}/medicos/me/pacientes`);
  }

  obtenerValoracion(citaId: number): Observable<ValoracionResponse> {
    return this.http.get<ValoracionResponse>(`${this.apiUrl}/citas/${citaId}/valoracion`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  valorar(citaId: number, request: ValoracionRequest): Observable<ValoracionResponse> {
    return this.http.post<ValoracionResponse>(`${this.apiUrl}/citas/${citaId}/valoracion`, request);
  }

  valoracionesDeMedico(medicoId: number): Observable<ValoracionesMedicoResponse> {
    return this.http.get<ValoracionesMedicoResponse>(
      `${this.apiUrl}/medicos/${medicoId}/valoraciones`,
    );
  }
}
