import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_ERROR_NOTIFICATION } from '@tusalud/app/core/config/http-context';
import {
  PasswordChangeRequest,
  PerfilResponse,
  PerfilUpdateRequest,
  PreferenciasResponse,
  SesionResponse,
} from '@tusalud/app/core/models';

@Injectable({ providedIn: 'root' })
export class PerfilApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/perfil`;

  obtener(): Observable<PerfilResponse> {
    return this.http.get<PerfilResponse>(this.baseUrl);
  }

  actualizar(request: PerfilUpdateRequest): Observable<PerfilResponse> {
    return this.http.put<PerfilResponse>(this.baseUrl, request);
  }

  cambiarPassword(request: PasswordChangeRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/password`, request, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }

  actualizarAvatar(imagen: string): Observable<PerfilResponse> {
    return this.http.put<PerfilResponse>(`${this.baseUrl}/avatar`, { imagen });
  }

  eliminarAvatar(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/avatar`);
  }

  obtenerPreferencias(): Observable<PreferenciasResponse> {
    return this.http.get<PreferenciasResponse>(`${this.baseUrl}/preferencias`);
  }

  actualizarPreferencias(request: PreferenciasResponse): Observable<PreferenciasResponse> {
    return this.http.put<PreferenciasResponse>(`${this.baseUrl}/preferencias`, request);
  }

  listarSesiones(): Observable<SesionResponse[]> {
    return this.http.get<SesionResponse[]>(`${this.baseUrl}/sesiones`);
  }

  cerrarSesion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sesiones/${id}`);
  }

  cerrarOtrasSesiones(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/sesiones`);
  }
}
