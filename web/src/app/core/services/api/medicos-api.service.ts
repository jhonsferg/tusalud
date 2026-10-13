import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  CitaResponse,
  DisponibilidadResponse,
  HorarioRequest,
  HorarioResponse,
  MedicoCreateRequest,
  MedicoResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class MedicosApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/medicos`;

  listar(especialidadId?: number | null): Observable<MedicoResponse[]> {
    return this.http.get<MedicoResponse[]>(this.baseUrl, {
      params: toHttpParams({ especialidadId }),
    });
  }

  obtener(id: number): Observable<MedicoResponse> {
    return this.http.get<MedicoResponse>(`${this.baseUrl}/${id}`);
  }

  crear(request: MedicoCreateRequest): Observable<MedicoResponse> {
    return this.http.post<MedicoResponse>(this.baseUrl, request);
  }

  listarHorarios(medicoId: number): Observable<HorarioResponse[]> {
    return this.http.get<HorarioResponse[]>(`${this.baseUrl}/${medicoId}/horarios`);
  }

  agregarHorario(medicoId: number, request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.post<HorarioResponse>(`${this.baseUrl}/${medicoId}/horarios`, request);
  }

  eliminarHorario(medicoId: number, horarioId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${medicoId}/horarios/${horarioId}`);
  }

  disponibilidad(medicoId: number, fecha: string): Observable<DisponibilidadResponse> {
    return this.http.get<DisponibilidadResponse>(`${this.baseUrl}/${medicoId}/disponibilidad`, {
      params: toHttpParams({ fecha }),
    });
  }

  miAgenda(fecha?: string | null): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(`${this.baseUrl}/me/agenda`, {
      params: toHttpParams({ fecha }),
    });
  }
}
