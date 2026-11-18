import { readFileSync } from 'node:fs';

import { Download, Page, expect, test } from '@playwright/test';

import { CUENTAS, cerrarSesion, ir, iniciarSesion, tokenDe } from './support';

function contenido(descarga: Download): Promise<Buffer> {
  return descarga.path().then((ruta) => readFileSync(ruta));
}

async function iniciarSesionCon(page: Page, email: string, password: string): Promise<string> {
  const respuesta = await page.request.post('/api/v1/auth/login', { data: { email, password } });
  return ((await respuesta.json()) as { token: string }).token;
}

async function atencionConReceta(page: Page, email: string, password: string): Promise<string> {
  const token = await iniciarSesionCon(page, email, password);
  const respuesta = await page.request.get('/api/v1/historia-clinica', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const historia = (await respuesta.json()) as { atenciones: { id: string; receta: unknown[] }[] };
  const atencion = historia.atenciones.find((item) => item.receta.length > 0);
  if (!atencion) {
    throw new Error('El paciente no tiene una atención con receta');
  }
  return atencion.id;
}

async function eliminarExportaciones(page: Page, rol: keyof typeof CUENTAS): Promise<void> {
  const token = await tokenDe(page, rol);
  const cabecera = { Authorization: `Bearer ${token}` };
  const lista = await page.request.get('/api/v1/exportaciones', { headers: cabecera });
  for (const { id } of (await lista.json()) as { id: string }[]) {
    await page.request.delete(`/api/v1/exportaciones/${id}`, { headers: cabecera });
  }
}

test.describe('documentos y exportaciones', () => {
  test('gerencia exporta el reporte ejecutivo a PDF y a Excel y lo encuentra en Mis exportaciones', async ({
    page,
  }) => {
    await iniciarSesion(page, 'gerencia');
    await eliminarExportaciones(page, 'gerencia');
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte ejecutivo' })).toBeVisible();

    const pdf = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a PDF' }).click();
    const descargaPdf = await pdf;
    expect(descargaPdf.suggestedFilename()).toMatch(/\.pdf$/);
    expect((await contenido(descargaPdf)).subarray(0, 5).toString()).toBe('%PDF-');

    const excel = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a Excel' }).click();
    const descargaExcel = await excel;
    expect(descargaExcel.suggestedFilename()).toMatch(/\.xlsx$/);
    expect((await contenido(descargaExcel)).subarray(0, 2).toString()).toBe('PK');

    await ir(page, '/mis-exportaciones');
    await expect(page.getByRole('heading', { level: 1, name: 'Mis exportaciones' })).toBeVisible();
    await expect(page.getByText('Lista').first()).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: 'Excel' }).first()).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: 'PDF' }).first()).toBeVisible();

    const redescarga = page.waitForEvent('download');
    await page
      .getByRole('row')
      .filter({ hasText: 'PDF' })
      .first()
      .getByRole('button', { name: /Descargar/ })
      .click();
    expect((await contenido(await redescarga)).subarray(0, 5).toString()).toBe('%PDF-');

    await page
      .getByRole('row')
      .filter({ hasText: 'PDF' })
      .first()
      .getByRole('button', { name: /Eliminar/ })
      .click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByText('La exportación fue eliminada.')).toBeVisible();

    await eliminarExportaciones(page, 'gerencia');
  });

  test('caja descarga el comprobante en PDF y exporta el listado a Excel', async ({ page }) => {
    await iniciarSesion(page, 'caja');
    await eliminarExportaciones(page, 'caja');
    await ir(page, '/comprobantes');
    await expect(page.getByRole('heading', { level: 1, name: 'Comprobantes' })).toBeVisible();

    const pdf = page.waitForEvent('download');
    await page
      .getByRole('button', { name: /^Descargar el PDF de / })
      .first()
      .click();
    const descargaPdf = await pdf;
    expect(descargaPdf.suggestedFilename()).toMatch(/\.pdf$/);
    expect((await contenido(descargaPdf)).subarray(0, 5).toString()).toBe('%PDF-');

    const excel = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a Excel' }).click();
    const descargaExcel = await excel;
    expect(descargaExcel.suggestedFilename()).toMatch(/\.xlsx$/);
    expect((await contenido(descargaExcel)).subarray(0, 2).toString()).toBe('PK');

    await eliminarExportaciones(page, 'caja');
  });

  test('el paciente descarga sus resultados de laboratorio y su receta, y nadie más puede', async ({
    page,
  }) => {
    await page.goto('/auth/login');
    const ATENCION_CARLOS = await atencionConReceta(
      page,
      'carlos.mendoza@correo.com',
      'Paciente123!',
    );
    const ATENCION_SOFIA = await atencionConReceta(
      page,
      'sofia.ramirez@correo.com',
      'Paciente123!',
    );
    const token = await iniciarSesionCon(page, 'carlos.mendoza@correo.com', 'Paciente123!');
    const cabecera = { Authorization: `Bearer ${token}` };

    const receta = await page.request.get(`/api/v1/documentos/receta/${ATENCION_CARLOS}`, {
      headers: cabecera,
    });
    expect(receta.status()).toBe(200);
    expect(receta.headers()['content-type']).toContain('application/pdf');
    expect((await receta.body()).subarray(0, 5).toString()).toBe('%PDF-');

    const ajena = await page.request.get(`/api/v1/documentos/receta/${ATENCION_SOFIA}`, {
      headers: cabecera,
    });
    expect(ajena.status()).toBe(403);

    const sinSesion = await page.request.get(`/api/v1/documentos/receta/${ATENCION_CARLOS}`);
    expect(sinSesion.status()).toBe(401);

    const tokenEnUrl = await page.request.get(
      `/api/v1/documentos/receta/${ATENCION_CARLOS}?token=${token}`,
    );
    expect(tokenEnUrl.status()).toBe(401);
  });

  test('el paciente descarga desde la pantalla sus resultados de laboratorio', async ({ page }) => {
    await omitirGuiaYEntrar(page, 'carlos.mendoza@correo.com', 'Paciente123!');
    await ir(page, '/mis-resultados');

    const pdf = page.waitForEvent('download');
    await page
      .getByRole('button', { name: /^Descargar el PDF de resultados de / })
      .first()
      .click();
    const descarga = await pdf;
    expect(descarga.suggestedFilename()).toMatch(/\.pdf$/);
    expect((await contenido(descarga)).subarray(0, 5).toString()).toBe('%PDF-');
  });

  test('un médico sin relación con el paciente no obtiene su receta', async ({ page }) => {
    await page.goto('/auth/login');
    const ATENCION_CARLOS = await atencionConReceta(
      page,
      'carlos.mendoza@correo.com',
      'Paciente123!',
    );
    const ATENCION_SOFIA = await atencionConReceta(
      page,
      'sofia.ramirez@correo.com',
      'Paciente123!',
    );
    const token = await iniciarSesionCon(page, CUENTAS.medico.email, CUENTAS.medico.password);
    const cabecera = { Authorization: `Bearer ${token}` };

    const propia = await page.request.get(`/api/v1/documentos/receta/${ATENCION_CARLOS}`, {
      headers: cabecera,
    });
    expect(propia.status()).toBe(200);

    const ajena = await page.request.get(`/api/v1/documentos/receta/${ATENCION_SOFIA}`, {
      headers: cabecera,
    });
    expect(ajena.status()).toBe(403);
  });

  test('un rol sin acceso no puede pedir el reporte ni ver exportaciones ajenas', async ({
    page,
  }) => {
    await page.goto('/auth/login');
    const caja = await iniciarSesionCon(page, CUENTAS.caja.email, CUENTAS.caja.password);
    const gerencia = await iniciarSesionCon(
      page,
      CUENTAS.gerencia.email,
      CUENTAS.gerencia.password,
    );

    const prohibido = await page.request.post('/api/v1/exportaciones', {
      headers: { Authorization: `Bearer ${caja}` },
      data: { tipo: 'reporte-ejecutivo', formato: 'PDF' },
    });
    expect(prohibido.status()).toBe(403);

    const propia = await page.request.post('/api/v1/exportaciones', {
      headers: { Authorization: `Bearer ${gerencia}` },
      data: { tipo: 'reporte-ejecutivo', formato: 'PDF' },
    });
    expect(propia.status()).toBeLessThan(300);
    const { id } = (await propia.json()) as { id: string };

    const ajena = await page.request.get(`/api/v1/exportaciones/${id}`, {
      headers: { Authorization: `Bearer ${caja}` },
    });
    expect([403, 404]).toContain(ajena.status());

    const admin = await tokenDe(page, 'admin');
    const deAdmin = await page.request.get(`/api/v1/exportaciones/${id}/descargar`, {
      headers: { Authorization: `Bearer ${admin}` },
    });
    expect([403, 404]).toContain(deAdmin.status());

    await page.request.delete(`/api/v1/exportaciones/${id}`, {
      headers: { Authorization: `Bearer ${gerencia}` },
    });
  });

  test('el administrador sube una plantilla válida, rechaza una inválida y la restaura', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    const token = await tokenDe(page, 'admin');
    const original = await page.request.get('/api/v1/plantillas/constancia/descargar', {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(original.status()).toBe(200);
    const jrxml = await original.body();

    await ir(page, '/plantillas');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Plantillas de documentos' }),
    ).toBeVisible();
    const fila = page.getByRole('row').filter({ hasText: 'constancia.jrxml' });

    await fila.getByRole('button', { name: /^Reemplazar la plantilla/ }).click();
    await page.locator('#plantilla-archivo').setInputFiles({
      name: 'constancia.jrxml',
      mimeType: 'application/xml',
      buffer: Buffer.from('<jasperReport><scriptlet class="x"/></jasperReport>'),
    });
    await page.getByRole('button', { name: 'Validar y reemplazar' }).click();
    await expect(page.getByRole('dialog').locator('p-message')).toBeVisible();

    await page.locator('#plantilla-archivo').setInputFiles({
      name: 'constancia.jrxml',
      mimeType: 'application/xml',
      buffer: jrxml,
    });
    const motivo = 'Prueba e2e ' + Date.now();
    await page.locator('#plantilla-motivo').fill(motivo);
    await page.getByRole('button', { name: 'Validar y reemplazar' }).click();
    await expect(page.getByText(/pasó a la versión/)).toBeVisible();
    await expect(fila.getByText('Personalizada')).toBeVisible();

    await fila.getByRole('button', { name: /^Historial de la plantilla/ }).click();
    await expect(page.getByRole('dialog').getByText(motivo)).toBeVisible();
    await page.keyboard.press('Escape');

    await fila.getByRole('button', { name: /^Restaurar la plantilla/ }).click();
    await page.getByRole('button', { name: 'Restaurar', exact: true }).click();
    await expect(page.getByText('Se restauró la plantilla predeterminada.')).toBeVisible();
    await expect(fila.getByText('Predeterminada').first()).toBeVisible();
  });

  test('las plantillas son solo del administrador', async ({ page }) => {
    await page.goto('/auth/login');
    const gerencia = await iniciarSesionCon(
      page,
      CUENTAS.gerencia.email,
      CUENTAS.gerencia.password,
    );
    const respuesta = await page.request.get('/api/v1/plantillas', {
      headers: { Authorization: `Bearer ${gerencia}` },
    });
    expect(respuesta.status()).toBe(403);

    await iniciarSesion(page, 'gerencia');
    await cerrarSesion(page);
  });
});

async function omitirGuiaYEntrar(page: Page, email: string, password: string): Promise<void> {
  const { omitirGuia } = await import('./support');
  await omitirGuia(page);
  await page.goto('/auth/login');
  await page.getByLabel('Email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(/\/paciente\/citas$/);
}
