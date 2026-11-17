import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  CitaResponse,
  DisponibilidadDiaResponse,
  DisponibilidadResponse,
  HorarioRequest,
  HorarioResponse,
  MedicoCreateRequest,
  MedicoResponse,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class MedicosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/medicos`;

  listar(especialidadId?: string | null): Observable<MedicoResponse[]> {
    return this.http.get<MedicoResponse[]>(this.baseUrl, {
      params: toHttpParams({ especialidadId }),
    });
  }

  obtener(id: string): Observable<MedicoResponse> {
    return this.http.get<MedicoResponse>(`${this.baseUrl}/${id}`);
  }

  crear(request: MedicoCreateRequest): Observable<MedicoResponse> {
    return this.http.post<MedicoResponse>(this.baseUrl, request);
  }

  actualizarEspecialidades(id: string, especialidadIds: string[]): Observable<MedicoResponse> {
    return this.http.put<MedicoResponse>(`${this.baseUrl}/${id}/especialidades`, {
      especialidadIds,
    });
  }

  listarHorarios(medicoId: string): Observable<HorarioResponse[]> {
    return this.http.get<HorarioResponse[]>(`${this.baseUrl}/${medicoId}/horarios`);
  }

  agregarHorario(medicoId: string, request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.post<HorarioResponse>(`${this.baseUrl}/${medicoId}/horarios`, request);
  }

  eliminarHorario(medicoId: string, horarioId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${medicoId}/horarios/${horarioId}`);
  }

  disponibilidad(medicoId: string, fecha: string): Observable<DisponibilidadResponse> {
    return this.http.get<DisponibilidadResponse>(`${this.baseUrl}/${medicoId}/disponibilidad`, {
      params: toHttpParams({ fecha }),
    });
  }

  disponibilidadRango(
    medicoId: string,
    inicio: string,
    fin: string,
  ): Observable<DisponibilidadDiaResponse[]> {
    return this.http.get<DisponibilidadDiaResponse[]>(
      `${this.baseUrl}/${medicoId}/disponibilidad/rango`,
      { params: toHttpParams({ inicio, fin }) },
    );
  }

  miAgendaRango(inicio: string, fin: string): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.baseUrl}/me/agenda`, {
      params: toHttpParams({ inicio, fin }),
    });
  }

  miAgenda(fecha?: string | null): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.baseUrl}/me/agenda`, {
      params: toHttpParams({ fecha }),
    });
  }
}
