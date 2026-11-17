import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import {
  AltaRequest,
  CamaRequest,
  CamaResponse,
  EmergenciaRequest,
  EvolucionRequest,
  EvolucionResponse,
  IngresoHospRequest,
  IngresoHospResponse,
  MapaCamasResponse,
  ResumenHospResponse,
} from '@tusalud/app/core/models';
import { toHttpParams } from '@tusalud/app/core/utils/http-params';

@Injectable({ providedIn: 'root' })
export class HospitalizacionApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/hospitalizacion`;

  resumen(): Observable<ResumenHospResponse> {
    return this.http.get<ResumenHospResponse>(`${this.baseUrl}/resumen`);
  }

  mapa(): Observable<MapaCamasResponse[]> {
    return this.http
      .get<{ areas: MapaCamasResponse[] }>(`${this.baseUrl}/mapa`)
      .pipe(map((respuesta) => respuesta.areas));
  }

  camas(area?: string | null, estado?: string | null): Observable<CamaResponse[]> {
    return this.http.get<CamaResponse[]>(`${this.baseUrl}/camas`, {
      params: toHttpParams({ area, estado }),
    });
  }

  crearCama(request: CamaRequest): Observable<CamaResponse> {
    return this.http.post<CamaResponse>(`${this.baseUrl}/camas`, request);
  }

  actualizarCama(id: number, request: CamaRequest): Observable<CamaResponse> {
    return this.http.put<CamaResponse>(`${this.baseUrl}/camas/${id}`, request);
  }

  estadoCama(id: number, estado: string): Observable<CamaResponse> {
    return this.http.put<CamaResponse>(`${this.baseUrl}/camas/${id}/estado`, { estado });
  }

  ingresos(estado?: string | null, q?: string | null): Observable<IngresoHospResponse[]> {
    return this.http.get<IngresoHospResponse[]>(`${this.baseUrl}/ingresos`, {
      params: toHttpParams({ estado, q }),
    });
  }

  ingreso(id: number): Observable<IngresoHospResponse> {
    return this.http.get<IngresoHospResponse>(`${this.baseUrl}/ingresos/${id}`);
  }

  crearIngreso(request: IngresoHospRequest): Observable<IngresoHospResponse> {
    return this.http.post<IngresoHospResponse>(`${this.baseUrl}/ingresos`, request);
  }

  trasladar(id: number, camaId: number): Observable<IngresoHospResponse> {
    return this.http.put<IngresoHospResponse>(`${this.baseUrl}/ingresos/${id}/trasladar`, {
      camaId,
    });
  }

  asignarCama(id: number, camaId: number): Observable<IngresoHospResponse> {
    return this.http.put<IngresoHospResponse>(`${this.baseUrl}/ingresos/${id}/asignar-cama`, {
      camaId,
    });
  }

  alta(id: number, request: AltaRequest): Observable<IngresoHospResponse> {
    return this.http.put<IngresoHospResponse>(`${this.baseUrl}/ingresos/${id}/alta`, request);
  }

  evoluciones(id: number): Observable<EvolucionResponse[]> {
    return this.http.get<EvolucionResponse[]>(`${this.baseUrl}/ingresos/${id}/evoluciones`);
  }

  registrarEvolucion(id: number, request: EvolucionRequest): Observable<EvolucionResponse> {
    return this.http.post<EvolucionResponse>(`${this.baseUrl}/ingresos/${id}/evoluciones`, request);
  }

  emergencias(): Observable<IngresoHospResponse[]> {
    return this.http.get<IngresoHospResponse[]>(`${this.baseUrl}/emergencias`);
  }

  registrarEmergencia(request: EmergenciaRequest): Observable<IngresoHospResponse> {
    return this.http.post<IngresoHospResponse>(`${this.baseUrl}/emergencias`, request);
  }

  misIngresos(): Observable<IngresoHospResponse[]> {
    return this.http.get<IngresoHospResponse[]>(`${this.baseUrl}/mis-ingresos`);
  }
}
