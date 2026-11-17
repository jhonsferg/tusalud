import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ConfirmationService, MessageService } from 'primeng/api';

import { APP_ACTUAL, NAVEGADOR_EXTERNO, Rol, SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { provideAppShell } from '@tusalud/ui/shell-config';
import { APP } from './app.id';
import { NAV_GROUPS } from './nav.config';
import { routes } from './app.routes';

async function navegar(
  roles: Rol[] | null,
  url: string,
): Promise<{ url: string; externo: string[] }> {
  TestBed.resetTestingModule();
  localStorage.clear();
  if (roles) {
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(roles)));
  }
  const externo: string[] = [];
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideAppShell({ brand: 'TuSalud', grupos: NAV_GROUPS }),
      ConfirmationService,
      MessageService,
      { provide: APP_ACTUAL, useValue: APP },
      { provide: NAVEGADOR_EXTERNO, useValue: (destino: string) => externo.push(destino) },
      provideRouter(routes),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return { url: TestBed.inject(Router).url, externo };
}

describe('aplicación paciente', () => {
  it('envía al inicio de sesión del portal a quien no tiene sesión', async () => {
    const { externo } = await navegar(null, '/');
    expect(externo.length).toBe(1);
    expect(externo[0]).toContain('/auth/login');
  });

  it('abre su página inicial a su rol', async () => {
    const { url, externo } = await navegar(['ROLE_PACIENTE'], '/');
    expect(externo).toEqual([]);
    expect(url).not.toBe('/');
  });

  it('manda a otro rol a su propio módulo', async () => {
    const otro: Rol = 'ROLE_CAJA';
    const { externo } = await navegar([otro], '/');
    expect(externo).toEqual(['/caja/']);
  });

  it('resuelve todas sus rutas perezosas', async () => {
    const hijos = routes.find((ruta) => ruta.path === '')?.children ?? [];
    const cargadas = await Promise.all(
      hijos
        .filter((ruta) => ruta.loadChildren)
        .map((ruta) => (ruta.loadChildren as () => Promise<unknown>)()),
    );
    expect(cargadas.every((rutas) => Array.isArray(rutas) && rutas.length > 0)).toBe(true);
  });

  it('aplica los permisos de su rol dentro de la aplicación', async () => {
    expect((await navegar(['ROLE_PACIENTE'], '/')).url).toBe('/citas');
    expect((await navegar(['ROLE_PACIENTE'], '/medicos/1/disponibilidad')).url).toBe(
      '/medicos/1/disponibilidad',
    );
    expect((await navegar(['ROLE_PACIENTE'], '/agenda')).url).toBe('/agenda');
    expect((await navegar(['ROLE_PACIENTE'], '/mis-resultados')).url).toBe('/mis-resultados');
    expect((await navegar(['ROLE_PACIENTE'], '/mis-ingresos')).url).toBe('/mis-ingresos');
  });
});
