import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { createAuthResponse, createSession } from '../../testing/auth-fixtures';
import { authInterceptor } from './auth.interceptor';
import { errorInterceptor } from './error.interceptor';
import { AuthService, SESSION_STORAGE_KEY } from '../services/auth.service';

const NO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

describe('renovación del token', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  function setup(refresh: { token: string; vence: number } | null = null): AuthService {
    localStorage.clear();
    const sesion = {
      ...createSession(['ROLE_PACIENTE']),
      ...(refresh ? { refreshToken: refresh.token, refreshExpiresAt: refresh.vence } : {}),
    };
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sesion));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor, authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    return TestBed.inject(AuthService);
  }

  function respuestaRenovada(refreshToken: string) {
    return {
      ...createAuthResponse(['ROLE_PACIENTE'], 15 * 60 * 1000),
      refreshToken,
      refreshExpiresIn: 7 * 86_400_000,
    };
  }

  afterEach(() => {
    controller.verify();
    vi.useRealTimers();
  });

  it('renueva el token ante un 401 y repite la petición con el token nuevo', () => {
    const auth = setup({ token: 'refresh-1', vence: Date.now() + 86_400_000 });
    let resultado: unknown;
    http.get('/api/v1/citas/mis-citas').subscribe((valor) => (resultado = valor));

    controller.expectOne('/api/v1/citas/mis-citas').flush({}, NO_AUTORIZADO);
    const renovacion = controller.expectOne('/api/v1/auth/refresh');
    expect(renovacion.request.body).toEqual({ refreshToken: 'refresh-1' });
    expect(renovacion.request.headers.has('Authorization')).toBe(false);
    renovacion.flush(respuestaRenovada('refresh-2'));

    const reintento = controller.expectOne('/api/v1/citas/mis-citas');
    expect(reintento.request.headers.get('Authorization')).toBe(`Bearer ${auth.token()}`);
    reintento.flush([{ id: 1 }]);

    expect(resultado).toEqual([{ id: 1 }]);
    expect(auth.isAuthenticated()).toBe(true);
    TestBed.tick();
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? '{}').refreshToken).toBe(
      'refresh-2',
    );
  });

  it('comparte una sola renovación entre peticiones simultáneas', () => {
    setup({ token: 'refresh-1', vence: Date.now() + 86_400_000 });
    http.get('/api/v1/medicos').subscribe();
    http.get('/api/v1/especialidades').subscribe();

    controller.expectOne('/api/v1/medicos').flush({}, NO_AUTORIZADO);
    controller.expectOne('/api/v1/especialidades').flush({}, NO_AUTORIZADO);
    controller.expectOne('/api/v1/auth/refresh').flush(respuestaRenovada('refresh-2'));
    controller.expectOne('/api/v1/medicos').flush([]);
    controller.expectOne('/api/v1/especialidades').flush([]);
  });

  it('cierra la sesión si el refresh token es rechazado', () => {
    const auth = setup({ token: 'refresh-1', vence: Date.now() + 86_400_000 });
    let fallo = false;
    http.get('/api/v1/medicos').subscribe({ error: () => (fallo = true) });

    controller.expectOne('/api/v1/medicos').flush({}, NO_AUTORIZADO);
    controller
      .expectOne('/api/v1/auth/refresh')
      .flush({ status: 401, message: 'Refresh token inválido' }, NO_AUTORIZADO);

    expect(fallo).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('no intenta renovar cuando la sesión no tiene refresh token', () => {
    const auth = setup();
    http.get('/api/v1/medicos').subscribe({ error: () => undefined });
    controller.expectOne('/api/v1/medicos').flush({}, NO_AUTORIZADO);
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('no renueva cuando el refresh token ya venció', () => {
    const auth = setup({ token: 'refresh-1', vence: Date.now() - 1000 });
    expect(auth.puedeRenovar()).toBe(false);
  });

  it('renueva por adelantado antes de que venza el token de acceso', () => {
    vi.useFakeTimers();
    setup({ token: 'refresh-1', vence: Date.now() + 86_400_000 });
    TestBed.tick();
    vi.advanceTimersByTime(60 * 60 * 1000 - 61_000);
    controller.expectNone('/api/v1/auth/refresh');
    vi.advanceTimersByTime(2000);
    controller.expectOne('/api/v1/auth/refresh').flush(respuestaRenovada('refresh-2'));
  });
});
