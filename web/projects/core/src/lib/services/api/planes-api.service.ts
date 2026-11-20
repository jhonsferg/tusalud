import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import {
  EstadoPlan,
  MedicionPlanRequest,
  PlanDetalle,
  PlanRequest,
  PlanResumen,
} from '../../models';

@Injectable({ providedIn: 'root' })
export class PlanesApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${inject(API_URL)}/planes-tratamiento`;

  crear(request: PlanRequest): Observable<PlanDetalle> {
    return this.http.post<PlanDetalle>(this.base, request);
  }

  mios(): Observable<PlanResumen[]> {
    return this.http.get<PlanResumen[]>(`${this.base}/mios`);
  }

  tablero(): Observable<PlanResumen[]> {
    return this.http.get<PlanResumen[]>(`${this.base}/tablero`);
  }

  obtener(id: string): Observable<PlanDetalle> {
    return this.http.get<PlanDetalle>(`${this.base}/${id}`);
  }

  medir(id: string, request: MedicionPlanRequest): Observable<PlanDetalle> {
    return this.http.post<PlanDetalle>(`${this.base}/${id}/mediciones`, request);
  }

  cumplirControl(id: string, controlId: string): Observable<PlanDetalle> {
    return this.http.post<PlanDetalle>(`${this.base}/${id}/controles/${controlId}/cumplir`, {});
  }

  cambiarEstado(id: string, estado: EstadoPlan): Observable<PlanDetalle> {
    return this.http.put<PlanDetalle>(`${this.base}/${id}/estado`, { estado });
  }
}
