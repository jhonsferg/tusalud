import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  ConsentimientoTelemedicinaResponse,
  IceServersResponse,
  ResumenPacienteTeleconsultaResponse,
  TeleconsultaResponse,
  TicketTeleconsultaResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class TeleconsultaApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/teleconsultas`;

  mias(): Observable<TeleconsultaResponse[]> {
    return this.http.get<TeleconsultaResponse[]>(`${this.baseUrl}/mias`);
  }

  delMedico(desde?: string | null): Observable<TeleconsultaResponse[]> {
    return this.http.get<TeleconsultaResponse[]>(`${this.baseUrl}/medico`, {
      params: toHttpParams({ desde }),
    });
  }

  obtener(id: string): Observable<TeleconsultaResponse> {
    return this.http.get<TeleconsultaResponse>(`${this.baseUrl}/${id}`);
  }

  consentimiento(): Observable<ConsentimientoTelemedicinaResponse> {
    return this.http.get<ConsentimientoTelemedicinaResponse>(`${this.baseUrl}/consentimiento`);
  }

  aceptarConsentimiento(version: string): Observable<ConsentimientoTelemedicinaResponse> {
    return this.http.post<ConsentimientoTelemedicinaResponse>(`${this.baseUrl}/consentimiento`, {
      version,
    });
  }

  abrir(id: string): Observable<TeleconsultaResponse> {
    return this.http.post<TeleconsultaResponse>(`${this.baseUrl}/${id}/abrir`, {});
  }

  ticket(id: string): Observable<TicketTeleconsultaResponse> {
    return this.http.post<TicketTeleconsultaResponse>(`${this.baseUrl}/${id}/ticket`, {});
  }

  ice(id: string): Observable<IceServersResponse> {
    return this.http.get<IceServersResponse>(`${this.baseUrl}/${id}/ice`);
  }

  finalizar(id: string): Observable<TeleconsultaResponse> {
    return this.http.post<TeleconsultaResponse>(`${this.baseUrl}/${id}/finalizar`, {});
  }

  resumenPaciente(id: string): Observable<ResumenPacienteTeleconsultaResponse> {
    return this.http.get<ResumenPacienteTeleconsultaResponse>(
      `${this.baseUrl}/${id}/resumen-paciente`,
    );
  }
}
