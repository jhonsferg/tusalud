import { expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, ir, tokenDe } from './support';

const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
  'base64',
);

function pngReal(): Buffer {
  return PNG;
}

test.describe('archivos y almacenamiento', () => {
  test('el paciente sube su foto de perfil, la ve tras recargar y la quita', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/perfil');
    await page.getByRole('button', { name: 'Cambiar foto de perfil' }).click();
    const guardado = page.waitForResponse(
      (r) => r.url().includes('/api/v1/perfil/avatar') && r.request().method() === 'PUT',
    );
    await page.locator('#per-avatar').setInputFiles({
      name: 'yo.png',
      mimeType: 'image/png',
      buffer: pngReal(),
    });
    const respuesta = await guardado;
    expect(respuesta.status()).toBe(200);
    await expect(page.getByText('Tu foto de perfil fue actualizada.')).toBeVisible();

    const avatar = page.locator('app-perfil .avatar-box img');
    await expect(avatar).toBeVisible();
    expect(await avatar.getAttribute('src')).toMatch(/^blob:/);

    await page.reload();
    await expect(page.locator('app-perfil .avatar-box img')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Quitar foto' })).toBeVisible();

    await page.getByRole('button', { name: 'Quitar foto' }).click();
    await expect(page.getByText('Tu foto de perfil fue eliminada.')).toBeVisible();
    await expect(page.locator('app-perfil .avatar-box img')).toHaveCount(0);
  });

  test('el selector rechaza formatos no permitidos y archivos demasiado grandes', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/perfil');
    await page.getByRole('button', { name: 'Cambiar foto de perfil' }).click();

    await page.locator('#per-avatar').setInputFiles({
      name: 'virus.exe',
      mimeType: 'application/octet-stream',
      buffer: Buffer.from('MZ'),
    });
    await expect(page.getByRole('alert').getByText(/virus\.exe.*formato permitido/)).toBeVisible();

    await page.locator('#per-avatar').setInputFiles({
      name: 'enorme.png',
      mimeType: 'image/png',
      buffer: Buffer.alloc(9 * 1024 * 1024, 1),
    });
    await expect(page.getByRole('alert').getByText(/enorme\.png.*supera el máximo/)).toBeVisible();
  });

  test('el servidor rechaza un archivo disfrazado aunque la extensión parezca válida', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    const token = await tokenDe(page, 'paciente');
    const cabeceras = { Authorization: `Bearer ${token}` };

    const falso = await page.request.put('/api/v1/perfil/avatar', {
      headers: cabeceras,
      multipart: {
        archivo: {
          name: 'foto.png',
          mimeType: 'image/png',
          buffer: Buffer.from('<html>hola</html>'),
        },
      },
    });
    expect(falso.status()).toBe(400);
    const svg = await page.request.put('/api/v1/perfil/avatar', {
      headers: cabeceras,
      multipart: {
        archivo: {
          name: 'foto.svg',
          mimeType: 'image/svg+xml',
          buffer: Buffer.from("<svg xmlns='http://www.w3.org/2000/svg'><script/></svg>"),
        },
      },
    });
    expect(svg.status()).toBe(400);
    const ejecutable = await page.request.put('/api/v1/perfil/avatar', {
      headers: cabeceras,
      multipart: {
        archivo: {
          name: 'foto.png',
          mimeType: 'image/png',
          buffer: Buffer.from([0x4d, 0x5a, 0x90, 0, 3, 0]),
        },
      },
    });
    expect(ejecutable.status()).toBe(400);
  });

  test('las descargas exigen sesión y el almacén no se expone por el gateway', async ({
    page,
    request,
  }) => {
    await iniciarSesion(page, 'paciente');
    const token = await tokenDe(page, 'paciente');
    const subida = await page.request.put('/api/v1/perfil/avatar', {
      headers: { Authorization: `Bearer ${token}` },
      multipart: { archivo: { name: 'yo.png', mimeType: 'image/png', buffer: pngReal() } },
    });
    expect(subida.status()).toBe(200);
    const { avatarId } = (await subida.json()) as { avatarId: string };

    expect((await request.get(`/api/v1/archivos/${avatarId}`)).status()).toBe(401);
    const conSesion = await page.request.get(`/api/v1/archivos/${avatarId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(conSesion.status()).toBe(200);
    expect(conSesion.headers()['content-type']).toBe('image/png');
    expect(conSesion.headers()['x-content-type-options']).toBe('nosniff');
    expect(conSesion.headers()['etag']).toBeTruthy();

    for (const ruta of ['/storage/', '/storage/avatares/x', '/storage', '/.git/config']) {
      expect((await request.get(ruta)).status(), ruta).toBe(404);
    }
    await expect(request.get('http://localhost:8333/', { timeout: 3000 })).rejects.toThrow();

    await page.request.delete('/api/v1/perfil/avatar', {
      headers: { Authorization: `Bearer ${token}` },
    });
  });

  test('un paciente no descarga el documento de otro ni la imagen de una orden ajena', async ({
    browser,
  }) => {
    const medico = await (await browser.newContext()).newPage();
    await iniciarSesion(medico, 'medico');
    const tokenMedico = await tokenDe(medico, 'medico');
    const pacientes = await medico.request.get('/api/v1/medicos/me/pacientes', {
      headers: { Authorization: `Bearer ${tokenMedico}` },
    });
    const lista = (await pacientes.json()) as { pacienteId: string }[];
    const pacienteId = lista[0].pacienteId;
    const documento = await medico.request.post('/api/v1/archivos', {
      headers: { Authorization: `Bearer ${tokenMedico}` },
      multipart: {
        proposito: 'DOCUMENTO_PACIENTE',
        referenciaId: pacienteId,
        archivo: {
          name: 'informe.pdf',
          mimeType: 'application/pdf',
          buffer: Buffer.from('%PDF-1.4\ndocumento de prueba'),
        },
      },
    });
    expect(documento.status()).toBe(201);
    const { id } = (await documento.json()) as { id: string };

    const otro = await (await browser.newContext()).newPage();
    await iniciarSesion(otro, 'admin');
    const tokenAdmin = await tokenDe(otro, 'admin');
    const admin = await otro.request.get(`/api/v1/archivos/${id}`, {
      headers: { Authorization: `Bearer ${tokenAdmin}` },
    });
    expect(admin.status()).toBe(403);

    const eliminado = await medico.request.delete(`/api/v1/archivos/${id}`, {
      headers: { Authorization: `Bearer ${tokenMedico}` },
    });
    expect(eliminado.status()).toBe(204);
  });
});

test.describe('gateway', () => {
  test('entrega las cabeceras de seguridad sin duplicarlas y oculta la versión', async ({
    request,
  }) => {
    const portal = await request.get('/');
    const cabeceras = portal.headers();
    expect(cabeceras['x-content-type-options']).toBe('nosniff');
    expect(cabeceras['x-frame-options']).toBe('DENY');
    expect(cabeceras['referrer-policy']).toBe('strict-origin-when-cross-origin');
    expect(cabeceras['content-security-policy']).toContain("default-src 'self'");
    expect(cabeceras['content-security-policy']).toContain("frame-ancestors 'none'");
    expect(cabeceras['x-request-id']).toMatch(/^[0-9a-f]{32}$/);
    expect(cabeceras['server']).toBe('nginx');
    expect(cabeceras['x-content-type-options']).not.toContain(',');

    const api = await request.get('/api/v1/especialidades');
    expect(api.status()).toBe(401);
    expect(api.headers()['x-content-type-options']).toBe('nosniff');
    expect(api.headers()['x-request-id']).toMatch(/^[0-9a-f]{32}$/);
  });

  test('los endpoints de salud responden y los métodos raros se rechazan', async ({ request }) => {
    expect((await request.get('/healthz')).status()).toBe(200);
    expect((await request.get('/healthz/api')).status()).toBe(200);
    expect((await request.fetch('/api/v1/especialidades', { method: 'TRACE' })).status()).toBe(405);
  });

  test('rechaza cuerpos que superan el límite de la ruta', async ({ request }) => {
    const respuesta = await request.post('/api/v1/auth/login', {
      headers: { 'Content-Type': 'application/json' },
      data: JSON.stringify({ email: 'x@x.com', password: 'x'.repeat(2 * 1024 * 1024) }),
    });
    expect(respuesta.status()).toBe(413);
  });

  test('limita la tasa de intentos de inicio de sesión sin afectar a otros clientes', async ({
    request,
  }) => {
    test.setTimeout(90_000);
    const sonda = `sonda-${Date.now()}`;
    const estados: number[] = [];
    for (let i = 0; i < 160; i++) {
      const respuesta = await request.post('/api/v1/auth/login', {
        headers: { 'X-Rate-Probe': sonda },
        data: { email: 'nadie@tusalud.com', password: 'incorrecta' },
      });
      estados.push(respuesta.status());
      if (respuesta.status() === 429) {
        break;
      }
    }
    expect(estados).toContain(429);
    expect(estados.filter((estado) => estado === 401).length).toBeGreaterThan(10);

    const normal = await request.post('/api/v1/auth/login', {
      data: { email: 'nadie@tusalud.com', password: 'incorrecta' },
    });
    expect(normal.status()).toBe(401);
  });

  test('la política de seguridad de contenido no rompe el portal ni las aplicaciones', async ({
    page,
  }) => {
    const violaciones: string[] = [];
    page.on('console', (mensaje) => {
      if (/Content Security Policy|Refused to (apply|load|execute|connect)/i.test(mensaje.text())) {
        violaciones.push(mensaje.text());
      }
    });
    page.on('pageerror', (error) => violaciones.push(error.message));
    await iniciarSesion(page, 'paciente');
    await ir(page, '/perfil');
    await expect(page.locator('app-perfil')).toBeVisible();
    await ir(page, '/calendario');
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();
    await cerrarSesion(page);
    expect(violaciones).toEqual([]);
  });
});
