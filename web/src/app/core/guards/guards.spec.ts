import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from '../../app.routes';
import { createSession } from '../../../testing/auth-fixtures';
import { Rol } from '../models';
import { SESSION_STORAGE_KEY } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';
import { roleGuard } from './role.guard';

@Component({ template: '' })
class Empty {}

async function navigateAs(roles: Rol[] | null, url: string, useAppRoutes = false): Promise<string> {
  TestBed.resetTestingModule();
  localStorage.clear();
  if (roles) {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(roles)));
  }
  TestBed.configureTestingModule({
    providers: [
      provideRouter(
        useAppRoutes
          ? routes
          : [
              { path: 'auth/login', component: Empty, canActivate: [guestGuard] },
              { path: 'acceso-denegado', component: Empty },
              { path: 'privado', component: Empty, canActivate: [authGuard] },
              { path: 'admin', component: Empty, canMatch: [roleGuard('ROLE_ADMIN')] },
              { path: 'citas', component: Empty },
              { path: 'agenda', component: Empty },
              { path: 'admin/auditoria', component: Empty },
              { path: '**', component: Empty },
            ],
      ),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return TestBed.inject(Router).url;
}

describe('guards', () => {
  it('authGuard redirige al login conservando la URL de retorno', async () => {
    expect(await navigateAs(null, '/privado?x=1')).toBe('/auth/login?returnUrl=%2Fprivado%3Fx%3D1');
  });

  it('authGuard permite el acceso con sesión', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/privado')).toBe('/privado');
  });

  it('guestGuard envía a la página inicial del rol a quien ya inició sesión', async () => {
    expect(await navigateAs(['ROLE_MEDICO'], '/auth/login')).toBe('/agenda');
    expect(await navigateAs(null, '/auth/login')).toBe('/auth/login');
  });

  it('roleGuard deniega el acceso a roles no autorizados', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/admin')).toBe('/acceso-denegado');
    expect(await navigateAs(['ROLE_ADMIN'], '/admin')).toBe('/admin');
  });

  it('roleGuard envía al login a quien no inició sesión', async () => {
    expect(await navigateAs(null, '/admin')).toBe('/auth/login?returnUrl=%2Fadmin');
  });
});

describe('rutas de la aplicación', () => {
  it('redirige la raíz según el rol', async () => {
    expect(await navigateAs(['ROLE_ADMIN'], '/', true)).toBe('/admin/auditoria');
    expect(await navigateAs(['ROLE_MEDICO'], '/', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_PACIENTE'], '/', true)).toBe('/citas');
    expect(await navigateAs(null, '/', true)).toBe('/auth/login');
  });

  it('protege las secciones por rol', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/admin/auditoria', true)).toBe('/acceso-denegado');
    expect(await navigateAs(['ROLE_PACIENTE'], '/agenda', true)).toBe('/acceso-denegado');
    expect(await navigateAs(['ROLE_MEDICO'], '/citas/nueva', true)).toBe('/acceso-denegado');
    expect(await navigateAs(['ROLE_ADMIN'], '/especialidades/3/editar', true)).toBe(
      '/especialidades/3/editar',
    );
    expect(await navigateAs(['ROLE_PACIENTE'], '/medicos/1/disponibilidad', true)).toBe(
      '/medicos/1/disponibilidad',
    );
  });

  it('muestra la página no encontrada para rutas desconocidas', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/no-existe', true)).toBe('/no-existe');
  });
});
