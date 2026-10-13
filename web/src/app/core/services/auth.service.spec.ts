import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { createAuthResponse, createSession } from '@tusalud/testing/auth-fixtures';
import { SKIP_AUTH, SKIP_ERROR_NOTIFICATION } from '@tusalud/app/core/config/http-context';
import {
  AuthService,
  SESSION_STORAGE_KEY,
  toSession,
} from '@tusalud/app/core/services/auth.service';
import { NotificationService } from '@tusalud/app/core/services/notification.service';

describe('AuthService', () => {
  let http: HttpTestingController;

  function setup(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => {
    http?.verify();
    vi.useRealTimers();
  });

  it('inicia sin sesión', () => {
    const auth = setup();
    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.roles()).toEqual([]);
    expect(auth.homeUrl()).toBe('/especialidades');
  });

  it('inicia sesión, expone signals derivados y persiste la sesión', () => {
    const auth = setup();
    let usuario: unknown;
    auth
      .login({ email: '  Juan.Perez@Correo.com ', password: 'Paciente123!' })
      .subscribe((u) => (usuario = u));

    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({
      email: 'juan.perez@correo.com',
      password: 'Paciente123!',
    });
    expect(request.request.context.get(SKIP_AUTH)).toBe(true);
    expect(request.request.context.get(SKIP_ERROR_NOTIFICATION)).toBe(true);
    request.flush(createAuthResponse(['ROLE_PACIENTE']));
    TestBed.tick();

    expect(usuario).toEqual(expect.objectContaining({ email: 'juan.perez@correo.com', id: 4 }));
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isPaciente()).toBe(true);
    expect(auth.isAdmin()).toBe(false);
    expect(auth.homeUrl()).toBe('/citas');
    expect(auth.token()).toContain('.');
    expect(JSON.parse(localStorage.getItem(SESSION_STORAGE_KEY) ?? 'null')?.user.email).toBe(
      'juan.perez@correo.com',
    );
  });

  it('registra pacientes y deja la sesión iniciada', () => {
    const auth = setup();
    auth
      .register({
        nombre: 'Rosa',
        apellido: 'Quispe',
        email: 'ROSA@correo.com',
        password: 'Paciente123!',
        dni: '12345678',
        fechaNacimiento: '1995-03-10',
      })
      .subscribe();
    const request = http.expectOne('/api/v1/auth/register');
    expect(request.request.body.email).toBe('rosa@correo.com');
    request.flush(createAuthResponse(['ROLE_PACIENTE']));
    expect(auth.isAuthenticated()).toBe(true);
  });

  it('restaura una sesión vigente y descarta una expirada', () => {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_MEDICO'])));
    expect(setup().homeUrl()).toBe('/agenda');

    TestBed.resetTestingModule();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ADMIN'], -1000)));
    const auth = setup();
    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('descarta sesiones almacenadas con forma inválida', () => {
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify({ token: 'x', user: { roles: ['ROOT'] } }),
    );
    expect(setup().isAuthenticated()).toBe(false);
  });

  it('cierra la sesión manualmente y navega al login', () => {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ADMIN'])));
    const auth = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    auth.logout();
    TestBed.tick();

    expect(auth.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith('/auth/login');
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
  });

  it('cierra la sesión cuando el token expira y avisa al usuario', () => {
    vi.useFakeTimers();
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify(createSession(['ROLE_PACIENTE'], 5000)),
    );
    const auth = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const notifications = TestBed.inject(NotificationService);
    TestBed.tick();

    vi.advanceTimersByTime(5001);

    expect(auth.isAuthenticated()).toBe(false);
    expect(notifications.notifications()[0]?.message).toContain('expiró');
    expect(navigate).toHaveBeenCalledWith(['/auth/login'], expect.anything());
  });

  it('verifica roles', () => {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_MEDICO'])));
    const auth = setup();
    expect(auth.hasAnyRole(['ROLE_ADMIN', 'ROLE_MEDICO'])).toBe(true);
    expect(auth.hasAnyRole(['ROLE_ADMIN'])).toBe(false);
  });
});

describe('toSession', () => {
  it('toma la expiración del JWT y filtra roles desconocidos', () => {
    const now = Date.now();
    const response = {
      ...createAuthResponse(['ROLE_ADMIN'], 60_000, now),
      roles: ['ROLE_ADMIN', 'ROLE_X'],
    };
    const session = toSession(response, now);
    expect(Math.abs(session.expiresAt - (now + 60_000))).toBeLessThan(1000);
    expect(session.user.roles).toEqual(['ROLE_ADMIN']);
  });

  it('usa expiresIn cuando el token no trae expiración', () => {
    const session = toSession({ ...createAuthResponse(), token: 'sin.exp' }, 1000);
    expect(session.expiresAt).toBe(1000 + 60 * 60 * 1000);
  });
});
