import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '../../config/http-context';
import {
  AusenciaRequest,
  AusenciaResponse,
  EmpleadoRequest,
  EmpleadoResponse,
  FiltroAusencias,
  FiltroEmpleados,
  MisAusenciasResponse,
  PlantillaTurnoResponse,
  ResumenRrhhResponse,
  SaldoVacacionesResponse,
  SemanaTurnosResponse,
  TurnoPersonalRequest,
  TurnoPersonalResponse,
  UsuarioDisponibleResponse,
} from '../../models';
import { toHttpParams } from '../../utils/http-params';

@Injectable({ providedIn: 'root' })
export class RrhhApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/rrhh`;

  resumen(): Observable<ResumenRrhhResponse> {
    return this.http.get<ResumenRrhhResponse>(`${this.baseUrl}/resumen`);
  }

  empleados(filtro: FiltroEmpleados = {}): Observable<EmpleadoResponse[]> {
    return this.http.get<EmpleadoResponse[]>(`${this.baseUrl}/empleados`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  empleado(id: string): Observable<EmpleadoResponse> {
    return this.http.get<EmpleadoResponse>(`${this.baseUrl}/empleados/${id}`);
  }

  crearEmpleado(request: EmpleadoRequest): Observable<EmpleadoResponse> {
    return this.http.post<EmpleadoResponse>(`${this.baseUrl}/empleados`, request);
  }

  actualizarEmpleado(id: string, request: EmpleadoRequest): Observable<EmpleadoResponse> {
    return this.http.put<EmpleadoResponse>(`${this.baseUrl}/empleados/${id}`, request);
  }

  usuariosDisponibles(): Observable<UsuarioDisponibleResponse[]> {
    return this.http.get<UsuarioDisponibleResponse[]>(`${this.baseUrl}/usuarios-disponibles`);
  }

  saldoVacaciones(empleadoId: string): Observable<SaldoVacacionesResponse> {
    return this.http.get<SaldoVacacionesResponse>(
      `${this.baseUrl}/empleados/${empleadoId}/saldo-vacaciones`,
    );
  }

  plantillasTurno(): Observable<PlantillaTurnoResponse[]> {
    return this.http.get<PlantillaTurnoResponse[]>(`${this.baseUrl}/plantillas-turno`);
  }

  semana(fecha: string, area?: string | null): Observable<SemanaTurnosResponse> {
    return this.http.get<SemanaTurnosResponse>(`${this.baseUrl}/turnos/semana`, {
      params: toHttpParams({ fecha, area }),
    });
  }

  asignarTurno(request: TurnoPersonalRequest): Observable<TurnoPersonalResponse> {
    return this.http.post<TurnoPersonalResponse>(`${this.baseUrl}/turnos`, request);
  }

  eliminarTurno(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/turnos/${id}`);
  }

  ausencias(filtro: FiltroAusencias = {}): Observable<AusenciaResponse[]> {
    return this.http.get<AusenciaResponse[]>(`${this.baseUrl}/ausencias`, {
      params: toHttpParams({ ...filtro }),
    });
  }

  resolverAusencia(
    id: string,
    accion: 'aprobar' | 'rechazar',
    motivo: string | null,
  ): Observable<AusenciaResponse> {
    return this.http.put<AusenciaResponse>(
      `${this.baseUrl}/ausencias/${id}/${accion}`,
      motivo ? { motivo } : {},
    );
  }

  miFicha(): Observable<EmpleadoResponse> {
    return this.http.get<EmpleadoResponse>(`${this.baseUrl}/mi-ficha`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  misTurnos(desde: string, hasta: string): Observable<TurnoPersonalResponse[]> {
    return this.http.get<TurnoPersonalResponse[]>(`${this.baseUrl}/mis-turnos`, {
      params: toHttpParams({ desde, hasta }),
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  misAusencias(): Observable<MisAusenciasResponse> {
    return this.http.get<MisAusenciasResponse>(`${this.baseUrl}/mis-ausencias`, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  solicitarAusencia(request: AusenciaRequest): Observable<AusenciaResponse> {
    return this.http.post<AusenciaResponse>(`${this.baseUrl}/mis-ausencias`, request);
  }

  cancelarMiAusencia(id: string): Observable<AusenciaResponse> {
    return this.http.put<AusenciaResponse>(`${this.baseUrl}/mis-ausencias/${id}/cancelar`, {});
  }

  cancelarAusencia(id: string): Observable<AusenciaResponse> {
    return this.http.put<AusenciaResponse>(`${this.baseUrl}/ausencias/${id}/cancelar`, {});
  }
}
