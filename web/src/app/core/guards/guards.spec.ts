import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ConfirmationService, MessageService } from 'primeng/api';

import { routes } from '@tusalud/app/app.routes';
import { createSession } from '@tusalud/testing/auth-fixtures';
import { Rol } from '@tusalud/app/core/models';
import { SESSION_STORAGE_KEY } from '@tusalud/app/core/services/auth.service';
import { authGuard } from '@tusalud/app/core/guards/auth.guard';
import { guestGuard } from '@tusalud/app/core/guards/guest.guard';
import { roleGuard } from '@tusalud/app/core/guards/role.guard';

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
      provideHttpClient(),
      provideHttpClientTesting(),
      ConfirmationService,
      MessageService,
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
    expect(await navigateAs(['ROLE_PACIENTE'], '/admin')).toBe('/citas');
    expect(await navigateAs(['ROLE_ADMIN'], '/admin')).toBe('/admin');
  });

  it('roleGuard envía al login a quien no inició sesión', async () => {
    expect(await navigateAs(null, '/admin')).toBe('/auth/login?returnUrl=%2Fadmin');
  });
});

describe('rutas de la aplicación', () => {
  it('redirige la raíz según el rol', async () => {
    expect(await navigateAs(['ROLE_ADMIN'], '/', true)).toBe('/admin/panel');
    expect(await navigateAs(['ROLE_MEDICO'], '/', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_PACIENTE'], '/', true)).toBe('/citas');
    expect(await navigateAs(null, '/', true)).toBe('/auth/login');
  });

  it('protege las secciones por rol', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/admin/auditoria', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_PACIENTE'], '/agenda', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_MEDICO'], '/citas/nueva', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_MEDICO'], '/especialidades', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_MEDICO'], '/medicos', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_ADMIN'], '/pagos/metodos', true)).toBe('/admin/panel');
    expect(await navigateAs(['ROLE_PACIENTE'], '/pacientes', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_ADMIN'], '/especialidades/3/editar', true)).toBe(
      '/especialidades/3/editar',
    );
    expect(await navigateAs(['ROLE_PACIENTE'], '/medicos/1/disponibilidad', true)).toBe(
      '/medicos/1/disponibilidad',
    );
  });

  it('permite al médico y al administrador abrir el detalle de una cita', async () => {
    expect(await navigateAs(['ROLE_MEDICO'], '/citas/5', true)).toBe('/citas/5');
    expect(await navigateAs(['ROLE_ADMIN'], '/citas/5', true)).toBe('/citas/5');
  });

  it('lleva al personal nuevo a su módulo y le niega las demás secciones', async () => {
    expect(await navigateAs(['ROLE_LABORATORIO'], '/', true)).toBe('/laboratorio');
    expect(await navigateAs(['ROLE_FARMACIA'], '/', true)).toBe('/farmacia');
    expect(await navigateAs(['ROLE_ALMACEN'], '/', true)).toBe('/almacen');
    expect(await navigateAs(['ROLE_FARMACIA'], '/almacen/ingresos', true)).toBe('/farmacia');
    expect(await navigateAs(['ROLE_ALMACEN'], '/farmacia/dispensar', true)).toBe('/almacen');
    expect(await navigateAs(['ROLE_PACIENTE'], '/almacen/productos', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_MEDICO'], '/farmacia', true)).toBe('/agenda');
    expect(await navigateAs(['ROLE_PACIENTE'], '/mis-resultados', true)).toBe('/mis-resultados');
    expect(await navigateAs(['ROLE_MEDICO'], '/mis-medicamentos', true)).toBe('/agenda');
  });

  it('lleva a recepción, caja y enfermería a su módulo y les niega los demás', async () => {
    expect(await navigateAs(['ROLE_RECEPCION'], '/', true)).toBe('/recepcion');
    expect(await navigateAs(['ROLE_CAJA'], '/', true)).toBe('/caja');
    expect(await navigateAs(['ROLE_ENFERMERIA'], '/', true)).toBe('/hospitalizacion');
    expect(await navigateAs(['ROLE_CAJA'], '/recepcion', true)).toBe('/caja');
    expect(await navigateAs(['ROLE_RECEPCION'], '/caja/cobrar', true)).toBe('/recepcion');
    expect(await navigateAs(['ROLE_ENFERMERIA'], '/caja', true)).toBe('/hospitalizacion');
    expect(await navigateAs(['ROLE_PACIENTE'], '/hospitalizacion', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_MEDICO'], '/cola', true)).toBe('/cola');
    expect(await navigateAs(['ROLE_PACIENTE'], '/cola', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_PACIENTE'], '/mis-ingresos', true)).toBe('/mis-ingresos');
  });

  it('retira al administrador de los datos clínicos y a recepción de lo que no le corresponde', async () => {
    const id = '11111111-1111-4111-8111-111111111111';
    expect(await navigateAs(['ROLE_ADMIN'], `/pacientes/${id}/historia-clinica`, true)).toBe(
      '/admin/panel',
    );
    expect(await navigateAs(['ROLE_ADMIN'], '/pacientes', true)).toBe('/admin/panel');
    expect(await navigateAs(['ROLE_ADMIN'], `/laboratorio/ordenes/${id}`, true)).toBe(
      '/admin/panel',
    );
    expect(await navigateAs(['ROLE_ADMIN'], '/farmacia', true)).toBe('/admin/panel');
    expect(await navigateAs(['ROLE_ADMIN'], '/farmacia/dispensar', true)).toBe('/admin/panel');
    expect(await navigateAs(['ROLE_ADMIN'], '/farmacia/historial', true)).toBe(
      '/farmacia/historial',
    );
    expect(await navigateAs(['ROLE_MEDICO'], `/pacientes/${id}/historia-clinica`, true)).toBe(
      `/pacientes/${id}/historia-clinica`,
    );
    expect(await navigateAs(['ROLE_PACIENTE'], '/pacientes', true)).toBe('/citas');
    expect(await navigateAs(['ROLE_RECEPCION'], `/hospitalizacion/ingresos/${id}`, true)).toBe(
      `/hospitalizacion/ingresos/${id}`,
    );
    expect(await navigateAs(['ROLE_RECEPCION'], '/hospitalizacion/camas', true)).toBe('/recepcion');
  });

  it('muestra la página no encontrada para rutas desconocidas', async () => {
    expect(await navigateAs(['ROLE_PACIENTE'], '/no-existe', true)).toBe('/no-existe');
  });
});
