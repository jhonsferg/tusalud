import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { createSession } from '../../testing/auth-fixtures';
import { Rol } from '../models';
import { SESSION_STORAGE_KEY } from './auth.service';
import {
  CLAVE_ACTIVIDAD,
  CLAVE_BLOQUEO,
  CLAVE_INICIO_SESION,
  InactividadService,
  provideInactividad,
} from './inactividad.service';
import {
  PRIVACIDAD_STORAGE_KEY,
  PrivacidadService,
  enmascararCorreo,
  enmascararDni,
  enmascararTelefono,
} from './privacidad.service';

describe('enmascarado de datos personales', () => {
  it('oculta el centro del DNI y deja los extremos', () => {
    expect(enmascararDni('12345678')).toBe('12****78');
    expect(enmascararDni('1234')).toBe('****');
    expect(enmascararDni(null)).toBe('');
  });

  it('deja solo los tres últimos dígitos del teléfono', () => {
    expect(enmascararTelefono('999 888 777')).toBe('*** *** 777');
    expect(enmascararTelefono('12')).toBe('**');
    expect(enmascararTelefono(undefined)).toBe('');
  });

  it('conserva la inicial y el dominio del correo', () => {
    expect(enmascararCorreo('juan.perez@correo.com')).toBe('j*********@correo.com');
    expect(enmascararCorreo('sin-arroba')).toBe('**********');
  });
});

describe('PrivacidadService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('sensible-activo');
    TestBed.resetTestingModule();
  });

  it('empieza desactivado y alterna la marca global y su persistencia', () => {
    const privacidad = TestBed.inject(PrivacidadService);
    TestBed.tick();
    expect(privacidad.activa()).toBe(false);
    expect(document.documentElement.classList.contains('sensible-activo')).toBe(false);

    privacidad.alternar();
    TestBed.tick();
    expect(privacidad.activa()).toBe(true);
    expect(document.documentElement.classList.contains('sensible-activo')).toBe(true);
    expect(localStorage.getItem(PRIVACIDAD_STORAGE_KEY)).toBe('true');
  });

  it('recupera el modo guardado al abrir otra pestaña', () => {
    localStorage.setItem(PRIVACIDAD_STORAGE_KEY, 'true');
    expect(TestBed.inject(PrivacidadService).activa()).toBe(true);
  });
});

describe('InactividadService', () => {
  const MINUTO = 60_000;

  function crear(roles: Rol[] | null = ['ROLE_MEDICO']): InactividadService {
    TestBed.resetTestingModule();
    localStorage.clear();
    if (roles) {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(roles)));
    }
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
        provideInactividad({ bloqueoMinutos: 10, avisoSegundos: 60, sesionMaximaHoras: 2 }),
      ],
    });
    return TestBed.inject(InactividadService);
  }

  afterEach(() => {
    TestBed.inject(InactividadService).detener();
  });

  it('avisa un minuto antes y luego bloquea la pantalla', () => {
    const servicio = crear();
    const t0 = Date.now();
    servicio.iniciar();
    servicio.registrarActividad(t0);

    servicio.verificar(t0 + 5 * MINUTO);
    expect(servicio.estado()).toBe('activo');

    servicio.verificar(t0 + 9 * MINUTO + 30_000);
    expect(servicio.estado()).toBe('aviso');
    expect(servicio.segundosRestantes()).toBe(30);

    servicio.verificar(t0 + 10 * MINUTO);
    expect(servicio.estado()).toBe('bloqueado');
    expect(localStorage.getItem(CLAVE_BLOQUEO)).toBe('1');
  });

  it('seguir trabajando reinicia el contador', () => {
    const servicio = crear();
    const t0 = Date.now();
    servicio.iniciar();
    servicio.registrarActividad(t0);
    servicio.verificar(t0 + 9 * MINUTO + 30_000);
    expect(servicio.estado()).toBe('aviso');

    servicio.seguirTrabajando();
    servicio.verificar(Date.now());
    expect(servicio.estado()).toBe('activo');
  });

  it('desbloquear limpia el bloqueo compartido entre pestañas', () => {
    const servicio = crear();
    servicio.iniciar();
    servicio.bloquearAhora();
    expect(servicio.estado()).toBe('bloqueado');

    servicio.desbloquear();
    expect(servicio.estado()).toBe('activo');
    expect(localStorage.getItem(CLAVE_BLOQUEO)).toBe('0');
  });

  it('otra pestaña bloqueada bloquea también esta', () => {
    const servicio = crear();
    servicio.iniciar();
    localStorage.setItem(CLAVE_BLOQUEO, '1');
    servicio.verificar();
    expect(servicio.estado()).toBe('bloqueado');
  });

  it('cierra la sesión al superar el máximo de horas', () => {
    const servicio = crear();
    servicio.iniciar();
    localStorage.setItem(CLAVE_INICIO_SESION, String(Date.now() - 3 * 60 * MINUTO));
    servicio.verificar();
    expect(localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull();
    expect(localStorage.getItem(CLAVE_ACTIVIDAD)).toBeNull();
  });

  it('no hace nada si no hay sesión', () => {
    const servicio = crear(null);
    servicio.iniciar();
    servicio.verificar(Date.now() + 60 * MINUTO);
    expect(servicio.estado()).toBe('activo');
  });
});
