import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EnvironmentProviders, Provider } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { createSession } from '../../testing/auth-fixtures';
import {
  APP_ACTUAL,
  NAVEGADOR_EXTERNO,
  destinoLogin,
  modulosDe,
  moduloPrincipal,
  retornoPermitido,
} from '../config/modulos';
import { RutaOcultaSerializer, primerosSegmentos } from '../config/ruta-oculta';
import { Rol } from '../models';
import { AuthService, SESSION_STORAGE_KEY } from '../services/auth.service';
import { authGuard, entradaModulo } from './auth.guard';
import { guestGuard } from './guest.guard';
import { roleGuard } from './role.guard';

@Component({ template: '' })
class Vacio {}

async function navegar(
  roles: Rol[] | null,
  url: string,
  opciones: { externo?: boolean; app?: string } = {},
): Promise<{ url: string; externo: string[] }> {
  TestBed.resetTestingModule();
  localStorage.clear();
  if (roles) {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(roles)));
  }
  const externo: string[] = [];
  const proveedores: (Provider | EnvironmentProviders)[] = [
    provideHttpClient(),
    provideHttpClientTesting(),
    provideRouter([
      { path: 'auth/login', component: Vacio, canActivate: [guestGuard] },
      { path: 'acceso-denegado', component: Vacio },
      { path: 'privado', component: Vacio, canActivate: [authGuard] },
      { path: 'modulo', component: Vacio, canActivate: [entradaModulo('medico')] },
      { path: 'admin', component: Vacio, canMatch: [roleGuard('ROLE_ADMIN')] },
      { path: 'citas', component: Vacio },
      { path: 'agenda', component: Vacio },
      { path: '**', component: Vacio },
    ]),
  ];
  if (opciones.externo) {
    proveedores.push({
      provide: NAVEGADOR_EXTERNO,
      useValue: (destino: string) => externo.push(destino),
    });
  }
  if (opciones.app) {
    proveedores.push({ provide: APP_ACTUAL, useValue: opciones.app });
  }
  TestBed.configureTestingModule({ providers: proveedores });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return { url: TestBed.inject(Router).url, externo };
}

describe('guards', () => {
  it('authGuard redirige al login conservando la URL de retorno', async () => {
    expect((await navegar(null, '/privado?x=1')).url).toBe(
      '/auth/login?returnUrl=%2Fprivado%3Fx%3D1',
    );
  });

  it('authGuard permite el acceso con sesión', async () => {
    expect((await navegar(['ROLE_PACIENTE'], '/privado')).url).toBe('/privado');
  });

  it('guestGuard envía a quien ya inició sesión a la página inicial de su rol', async () => {
    expect((await navegar(['ROLE_MEDICO'], '/auth/login')).url).toBe('/agenda');
    expect((await navegar(null, '/auth/login')).url).toBe('/auth/login');
  });

  it('roleGuard deniega el acceso a roles no autorizados', async () => {
    expect((await navegar(['ROLE_PACIENTE'], '/admin')).url).toBe('/citas');
    expect((await navegar(['ROLE_ADMIN'], '/admin')).url).toBe('/admin');
  });

  it('roleGuard envía al login a quien no inició sesión', async () => {
    expect((await navegar(null, '/admin')).url).toBe('/auth/login?returnUrl=%2Fadmin');
  });

  it('con navegador externo, los guards llevan al portal y al módulo con una navegación completa', async () => {
    const sinSesion = await navegar(null, '/privado?x=1', { externo: true });
    expect(sinSesion.externo).toEqual(['/auth/login?returnUrl=%2Fprivado%3Fx%3D1']);

    const conSesion = await navegar(['ROLE_MEDICO'], '/auth/login', { externo: true });
    expect(conSesion.externo).toEqual(['/medico/']);
  });

  it('entradaModulo deja pasar a su rol y manda a los demás a su módulo', async () => {
    expect((await navegar(['ROLE_MEDICO'], '/modulo', { externo: true })).externo).toEqual([]);
    expect((await navegar(['ROLE_PACIENTE'], '/modulo', { externo: true })).externo).toEqual([
      '/paciente/',
    ]);
    expect((await navegar(null, '/modulo', { externo: true })).externo[0]).toContain('/auth/login');
  });

  it('la página inicial depende de los roles de la aplicación actual', async () => {
    TestBed.resetTestingModule();
    localStorage.clear();
    localStorage.setItem(
      SESSION_STORAGE_KEY,
      JSON.stringify(createSession(['ROLE_ADMIN', 'ROLE_MEDICO'])),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: APP_ACTUAL, useValue: 'medico' },
      ],
    });
    expect(TestBed.inject(AuthService).homeUrl()).toBe('/agenda');
  });
});

describe('módulos', () => {
  it('ordena los módulos por prioridad y elige el principal', () => {
    expect(modulosDe(['ROLE_PACIENTE', 'ROLE_ADMIN', 'ROLE_CAJA']).map((m) => m.app)).toEqual([
      'admin',
      'caja',
      'paciente',
    ]);
    expect(moduloPrincipal(['ROLE_PACIENTE'])?.base).toBe('/paciente/');
    expect(moduloPrincipal([])).toBeNull();
  });

  it('solo acepta retornos locales de módulos a los que el usuario accede', () => {
    expect(retornoPermitido('/paciente/citas/5', ['ROLE_PACIENTE'])).toBe('/paciente/citas/5');
    expect(retornoPermitido('/admin/panel', ['ROLE_PACIENTE'])).toBeNull();
    expect(retornoPermitido('//externo.com', ['ROLE_PACIENTE'])).toBeNull();
    expect(retornoPermitido('https://externo.com', ['ROLE_PACIENTE'])).toBeNull();
    expect(retornoPermitido(null, ['ROLE_PACIENTE'])).toBeNull();
  });

  it('arma la URL del login con retorno y motivo', () => {
    expect(destinoLogin(null)).toBe('/auth/login');
    expect(destinoLogin('/caja/cobrar?x=1', 'expired')).toBe(
      '/auth/login?returnUrl=%2Fcaja%2Fcobrar%3Fx%3D1&motivo=expired',
    );
    expect(destinoLogin('//malo.com')).toBe('/auth/login');
  });
});

describe('ruta oculta', () => {
  const rutas = [
    { path: 'caja', children: [] },
    { path: '', children: [{ path: 'calendario' }, { path: 'perfil' }] },
    { path: '**' },
  ];
  const serializador = new RutaOcultaSerializer('caja', primerosSegmentos(rutas));

  it('quita el prefijo al escribir la URL y lo restaura al leerla', () => {
    expect(serializador.serialize(serializador.parse('/caja/cobrar?x=1'))).toBe('/cobrar?x=1');
    expect(serializador.serialize(serializador.parse('/caja'))).toBe('/');
    expect(serializador.parse('/cobrar').toString()).toBe('/caja/cobrar');
    expect(serializador.parse('/').toString()).toBe('/caja');
    expect(serializador.parse('/?x=1').toString()).toBe('/caja?x=1');
  });

  it('no toca las rutas de otros dominios de la aplicación', () => {
    expect(serializador.serialize(serializador.parse('/calendario'))).toBe('/calendario');
    expect(serializador.parse('/perfil').toString()).toBe('/perfil');
  });

  it('no altera las redirecciones relativas', () => {
    const segmentos = (url: string) =>
      serializador.parse(url).root.children['primary'].segments.map((segmento) => segmento.path);
    expect(segmentos('panel')).toEqual(['panel']);
    expect(segmentos('/panel')).toEqual(['caja', 'panel']);
  });

  it('acepta también la URL con el prefijo repetido', () => {
    expect(serializador.parse('/caja/cobrar').toString()).toBe('/caja/cobrar');
  });
});
