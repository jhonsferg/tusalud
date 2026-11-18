import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { InactividadService, SESSION_STORAGE_KEY, provideInactividad } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { PantallaBloqueo } from './pantalla-bloqueo';

function respuestaLogin() {
  return {
    token: 'a.b.c',
    tokenType: 'Bearer',
    expiresIn: 900_000,
    usuarioId: '11111111-1111-4111-8111-111111111111',
    email: 'dr.garcia@tusalud.com',
    nombreCompleto: 'Luis García',
    roles: ['ROLE_MEDICO'],
  };
}

describe('PantallaBloqueo', () => {
  let fixture: ComponentFixture<PantallaBloqueo>;
  let inactividad: InactividadService;
  let http: HttpTestingController;

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const campo = (id: string) =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(id)!;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    localStorage.clear();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_MEDICO'])));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
        provideInactividad({ bloqueoMinutos: 10, avisoSegundos: 60, sesionMaximaHoras: 12 }),
      ],
    });
    inactividad = TestBed.inject(InactividadService);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PantallaBloqueo);
    fixture.detectChanges();
  });

  afterEach(() => inactividad.detener());

  it('no muestra nada mientras hay actividad', () => {
    expect(texto().trim()).toBe('');
  });

  it('muestra el aviso con la cuenta regresiva y permite seguir trabajando', () => {
    inactividad.iniciar();
    const t0 = Date.now();
    inactividad.registrarActividad(t0);
    inactividad.verificar(t0 + 10 * 60_000 - 20_000);
    fixture.detectChanges();
    expect(texto()).toContain('Tu sesión se bloqueará por inactividad');
    expect(texto()).toContain('20 segundos');

    (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('button.primario')!
      .click();
    fixture.detectChanges();
    expect(inactividad.estado()).toBe('activo');
  });

  it('bloquea con un formulario de contraseña que no revela el contenido', () => {
    inactividad.bloquearAhora();
    fixture.detectChanges();
    expect(texto()).toContain('Sesión bloqueada');
    expect(campo('#bloqueo-password').type).toBe('password');
    expect(campo('#bloqueo-password').getAttribute('autocomplete')).toBe('current-password');
  });

  it('mantiene el bloqueo y avisa cuando la contraseña es incorrecta', async () => {
    inactividad.bloquearAhora();
    fixture.detectChanges();
    campo('#bloqueo-password').value = 'mala';
    campo('#bloqueo-password').dispatchEvent(new Event('input'));
    (fixture.nativeElement as HTMLElement)
      .querySelector('form')!
      .dispatchEvent(new Event('submit'));
    http
      .expectOne('/api/v1/auth/login')
      .flush({ message: 'No' }, { status: 401, statusText: 'No' });
    fixture.detectChanges();
    expect(inactividad.estado()).toBe('bloqueado');
    expect(texto()).toContain('La contraseña no es correcta.');
  });

  it('desbloquea cuando la contraseña es correcta', async () => {
    inactividad.bloquearAhora();
    fixture.detectChanges();
    campo('#bloqueo-password').value = 'Medico123!';
    campo('#bloqueo-password').dispatchEvent(new Event('input'));
    (fixture.nativeElement as HTMLElement)
      .querySelector('form')!
      .dispatchEvent(new Event('submit'));
    const peticion = http.expectOne('/api/v1/auth/login');
    expect(peticion.request.body.password).toBe('Medico123!');
    peticion.flush(respuestaLogin());
    fixture.detectChanges();
    expect(inactividad.estado()).toBe('activo');
    expect(texto().trim()).toBe('');
  });

  it('pide el código cuando la cuenta usa verificación en dos pasos', () => {
    inactividad.bloquearAhora();
    fixture.detectChanges();
    campo('#bloqueo-password').value = 'Medico123!';
    campo('#bloqueo-password').dispatchEvent(new Event('input'));
    (fixture.nativeElement as HTMLElement)
      .querySelector('form')!
      .dispatchEvent(new Event('submit'));
    http
      .expectOne('/api/v1/auth/login')
      .flush({ ...respuestaLogin(), token: null, mfaRequerido: true, mfaToken: 'mfa-1' });
    fixture.detectChanges();
    expect(texto()).toContain('Código de verificación');
    expect(inactividad.estado()).toBe('bloqueado');
  });
});
