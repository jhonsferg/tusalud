import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { ConfirmationService, MessageService } from 'primeng/api';

import { NAVEGADOR_EXTERNO, Rol, SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
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
      ConfirmationService,
      MessageService,
      { provide: NAVEGADOR_EXTERNO, useValue: (destino: string) => externo.push(destino) },
      provideRouter(routes),
    ],
  });
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url);
  return { url: TestBed.inject(Router).url, externo };
}

describe('portal', () => {
  it('muestra el inicio de sesión a quien no tiene sesión', async () => {
    expect((await navegar(null, '/')).url).toBe('/auth/login');
    expect((await navegar(null, '/auth/login')).url).toBe('/auth/login');
  });

  it('lleva a cada rol a su módulo al entrar', async () => {
    const casos: [Rol, string][] = [
      ['ROLE_PACIENTE', '/paciente/'],
      ['ROLE_MEDICO', '/medico/'],
      ['ROLE_ADMIN', '/admin/'],
      ['ROLE_RECEPCION', '/recepcion/'],
      ['ROLE_CAJA', '/caja/'],
      ['ROLE_ENFERMERIA', '/enfermeria/'],
      ['ROLE_LABORATORIO', '/laboratorio/'],
      ['ROLE_FARMACIA', '/farmacia/'],
      ['ROLE_ALMACEN', '/almacen/'],
    ];
    for (const [rol, base] of casos) {
      expect((await navegar([rol], '/auth/login')).externo).toEqual([base]);
      expect((await navegar([rol], '/')).externo).toEqual([base]);
    }
  });

  it('prioriza el módulo cuando hay varios roles', async () => {
    expect((await navegar(['ROLE_PACIENTE', 'ROLE_ADMIN'], '/')).externo).toEqual(['/admin/']);
  });

  it('muestra la página no encontrada', async () => {
    expect((await navegar(null, '/nada/por/aqui')).url).toBe('/nada/por/aqui');
  });
});
