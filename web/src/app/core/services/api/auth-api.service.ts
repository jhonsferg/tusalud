import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '@tusalud/app/core/config/api.config';
import { SKIP_ERROR_NOTIFICATION, publicRequest } from '@tusalud/app/core/config/http-context';
import {
  AuthResponse,
  LoginRequest,
  LoginResponse,
  MfaActivacionResponse,
  MfaConfiguracionResponse,
  MfaDesactivarRequest,
  MfaEstadoResponse,
  MfaVerifyRequest,
  RefreshRequest,
  RegisterRequest,
} from '@tusalud/app/core/models';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${inject(API_URL)}/auth`;

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request, {
      context: publicRequest(),
    });
  }

  refresh(request: RefreshRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, request, {
      context: publicRequest(),
    });
  }

  verificarMfa(request: MfaVerifyRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/mfa/verificar`, request, {
      context: publicRequest(),
    });
  }

  estadoMfa(): Observable<MfaEstadoResponse> {
    return this.http.get<MfaEstadoResponse>(`${this.baseUrl}/mfa/estado`);
  }

  configurarMfa(): Observable<MfaConfiguracionResponse> {
    return this.http.post<MfaConfiguracionResponse>(`${this.baseUrl}/mfa/configurar`, null);
  }

  activarMfa(codigo: string): Observable<MfaActivacionResponse> {
    return this.http.post<MfaActivacionResponse>(`${this.baseUrl}/mfa/activar`, { codigo });
  }

  desactivarMfa(request: MfaDesactivarRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/mfa/desactivar`, request);
  }

  regenerarCodigosMfa(codigo: string): Observable<MfaActivacionResponse> {
    return this.http.post<MfaActivacionResponse>(
      `${this.baseUrl}/mfa/codigos-recuperacion/regenerar`,
      { codigo },
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, request, {
      context: publicRequest(),
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, null, {
      context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true),
    });
  }
}
