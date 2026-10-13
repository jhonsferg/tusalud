import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { createSession } from '../../../testing/auth-fixtures';
import { SKIP_AUTH, SKIP_ERROR_NOTIFICATION, SKIP_LOADING } from '../config/http-context';
import { ApiError } from '../models';
import { AuthService, SESSION_STORAGE_KEY } from '../services/auth.service';
import { LoadingService } from '../services/loading.service';
import { NotificationService } from '../services/notification.service';
import { authInterceptor } from './auth.interceptor';
import { errorInterceptor } from './error.interceptor';
import { loadingInterceptor } from './loading.interceptor';

describe('interceptores', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  function setup(authenticated: boolean): void {
    localStorage.clear();
    if (authenticated) {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_PACIENTE'])));
    }
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(
          withInterceptors([loadingInterceptor, authInterceptor, errorInterceptor]),
        ),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  }

  afterEach(() => controller.verify());

  describe('authInterceptor', () => {
    it('agrega el token solo a peticiones de la API', () => {
      setup(true);
      const token = TestBed.inject(AuthService).token();
      http.get('/api/v1/especialidades').subscribe();
      http.get('/assets/config.json').subscribe();

      expect(
        controller.expectOne('/api/v1/especialidades').request.headers.get('Authorization'),
      ).toBe(`Bearer ${token}`);
      expect(controller.expectOne('/assets/config.json').request.headers.has('Authorization')).toBe(
        false,
      );
    });

    it('respeta SKIP_AUTH y la ausencia de sesión', () => {
      setup(true);
      http
        .post('/api/v1/auth/login', {}, { context: new HttpContext().set(SKIP_AUTH, true) })
        .subscribe();
      expect(controller.expectOne('/api/v1/auth/login').request.headers.has('Authorization')).toBe(
        false,
      );

      vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      TestBed.inject(AuthService).logout();
      http.get('/api/v1/medicos').subscribe();
      expect(controller.expectOne('/api/v1/medicos').request.headers.has('Authorization')).toBe(
        false,
      );
    });
  });

  describe('loadingInterceptor', () => {
    it('marca la carga mientras hay peticiones pendientes', () => {
      setup(false);
      const loading = TestBed.inject(LoadingService);
      http.get('/api/v1/especialidades').subscribe({ error: () => undefined });
      http
        .get('/api/v1/silenciosa', { context: new HttpContext().set(SKIP_LOADING, true) })
        .subscribe();

      expect(loading.pendingRequests()).toBe(1);
      controller.expectOne('/api/v1/especialidades').flush([]);
      controller.expectOne('/api/v1/silenciosa').flush({});
      expect(loading.isLoading()).toBe(false);
    });
  });

  describe('errorInterceptor', () => {
    it('convierte el error en ApiError y lo notifica', () => {
      setup(true);
      let error: ApiError | undefined;
      http.post('/api/v1/citas', {}).subscribe({ error: (e: ApiError) => (error = e) });
      controller
        .expectOne('/api/v1/citas')
        .flush(
          { status: 409, message: 'El médico ya tiene una cita registrada en ese horario' },
          { status: 409, statusText: 'Conflict' },
        );

      expect(error?.status).toBe(409);
      expect(error?.message).toContain('ya tiene una cita');
      expect(TestBed.inject(NotificationService).notifications()[0]?.type).toBe('error');
    });

    it('no notifica cuando la petición lo indica', () => {
      setup(false);
      http
        .post(
          '/api/v1/auth/login',
          {},
          { context: new HttpContext().set(SKIP_ERROR_NOTIFICATION, true) },
        )
        .subscribe({ error: () => undefined });
      controller
        .expectOne('/api/v1/auth/login')
        .flush(
          { status: 401, message: 'Credenciales inválidas' },
          { status: 401, statusText: 'Unauthorized' },
        );
      expect(TestBed.inject(NotificationService).hasNotifications()).toBe(false);
    });

    it('cierra la sesión cuando el backend rechaza el token', () => {
      setup(true);
      const auth = TestBed.inject(AuthService);
      vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
      http.get('/api/v1/citas/mis-citas').subscribe({ error: () => undefined });
      controller
        .expectOne('/api/v1/citas/mis-citas')
        .flush(
          { status: 401, message: 'El token ha expirado' },
          { status: 401, statusText: 'Unauthorized' },
        );

      expect(auth.isAuthenticated()).toBe(false);
      const [notificacion] = TestBed.inject(NotificationService).notifications();
      expect(notificacion?.type).toBe('warning');
    });

    it('informa errores de red', () => {
      setup(false);
      let error: ApiError | undefined;
      http.get('/api/v1/especialidades').subscribe({ error: (e: ApiError) => (error = e) });
      controller
        .expectOne('/api/v1/especialidades')
        .error(new ProgressEvent('error'), { status: 0 });
      expect(error?.status).toBe(0);
      expect(error?.message).toContain('No se pudo conectar');
    });
  });
});
