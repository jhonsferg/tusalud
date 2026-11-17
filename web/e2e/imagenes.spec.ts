import { Page, expect, test } from '@playwright/test';

import { CUENTAS, ir, iniciarSesion, tokenDe } from './support';

const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);

interface OrdenCreada {
  id: string;
  codigo: string;
}

async function solicitarPorApi(page: Page, urgente = false): Promise<OrdenCreada> {
  const token = await tokenDe(page, 'medico');
  const headers = { Authorization: `Bearer ${token}` };
  const pacientes = (await (
    await page.request.get('/api/v1/medicos/me/pacientes', { headers })
  ).json()) as { pacienteId: string; dni: string }[];
  const juan = pacientes.find((p) => p.dni === '12345678')!;
  const estudios = (await (
    await page.request.get('/api/v1/imagenes/estudios?activo=true', { headers })
  ).json()) as { id: string; codigo: string }[];
  const rx = estudios.find((e) => e.codigo === 'RX-TORAX')!;
  const respuesta = await page.request.post('/api/v1/imagenes/ordenes', {
    headers,
    data: {
      pacienteId: juan.pacienteId,
      prioridad: urgente ? 'URGENTE' : 'NORMAL',
      indicacionClinica: 'Tos persistente de tres semanas',
      estudioIds: [rx.id],
    },
  });
  expect(respuesta.status()).toBe(201);
  return (await respuesta.json()) as OrdenCreada;
}

test.describe('imágenes diagnósticas', () => {
  test('el médico solicita un estudio desde el formulario', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/imagenes/solicitar');
    await page.locator('p-select').first().click();
    await page.getByRole('option', { name: /Juan Pérez/ }).click();
    await page.locator('p-multiselect').click();
    await page.getByRole('option', { name: /Radiografía de tórax/ }).click();
    await page.keyboard.press('Escape');
    await page.locator('#img-indicacion').fill('Dolor torácico al respirar, descartar neumonía');
    await page.getByRole('button', { name: /Enviar la orden$/ }).click();
    await expect(page).toHaveURL(/\/medico\/imagenes\/ordenes\/[0-9a-f-]{36}$/);
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(/IMG-\d+/);
    await expect(page.getByText('Dolor torácico al respirar')).toBeVisible();
  });

  test('el estudio recorre la programación, las imágenes y el informe hasta el paciente', async ({
    browser,
  }) => {
    test.setTimeout(120_000);
    const medico = await (await browser.newContext()).newPage();
    await iniciarSesion(medico, 'medico');
    const orden = await solicitarPorApi(medico);

    const personal = await (await browser.newContext()).newPage();
    await iniciarSesion(personal, 'imagenes');
    await personal.getByPlaceholder('Buscar por código, DNI o paciente').fill(orden.codigo);
    await expect(personal.getByText(orden.codigo).first()).toBeVisible();

    await ir(personal, `/imagenes/ordenes/${orden.id}`);
    await expect(personal.getByRole('heading', { level: 1 })).toHaveText(orden.codigo);
    await personal.getByRole('button', { name: /Programar$/ }).click();
    await personal.locator('#img-fecha').click();
    for (let mes = 1 + Math.floor(Math.random() * 4); mes > 0; mes--) {
      await personal.locator('.p-datepicker-next-button').click();
    }
    const dia = String(8 + Math.floor(Math.random() * 17));
    await personal.locator('.p-datepicker-calendar').getByText(dia, { exact: true }).click();
    await personal.locator('.p-dialog-title').click();
    await personal.locator('#img-sala').fill('Sala');
    await personal
      .getByRole('option')
      .nth(Math.floor(Math.random() * 5))
      .click();
    await personal.getByRole('button', { name: /Guardar$/ }).click();
    await expect(personal.getByText(/El estudio fue programado/)).toBeVisible();

    await personal.locator('#img-archivos').setInputFiles({
      name: 'toma.png',
      mimeType: 'image/png',
      buffer: PNG,
    });
    await expect(personal.getByText('toma.png')).toBeVisible();
    await personal.getByRole('button', { name: 'Ver toma.png' }).click();
    const visor = personal.getByRole('dialog', { name: 'Visor de imágenes' });
    await expect(visor).toBeVisible();
    const imagen = visor.locator('img.imagen');
    await expect(imagen).toBeVisible();
    expect(await imagen.evaluate((el: HTMLImageElement) => el.naturalWidth)).toBeGreaterThan(0);
    await visor.getByRole('button', { name: 'Acercar' }).click();
    await expect(visor.getByText('125%')).toBeVisible();
    await personal.keyboard.press('Escape');

    await personal.getByRole('button', { name: /Registrar toma realizada$/ }).click();
    await personal.getByRole('button', { name: /Confirmar toma$/ }).click();
    await expect(personal.getByText(/La toma quedó registrada/)).toBeVisible();

    await personal.getByRole('button', { name: /Redactar informe$/ }).click();
    await personal
      .locator('#inf-hallazgos')
      .fill('Campos pulmonares con transparencia conservada.');
    await personal
      .locator('#inf-conclusion')
      .fill('Radiografía de tórax sin alteraciones evidentes.');
    await personal.locator('#inf-colegiatura').fill('CMP-45210');
    await personal.getByRole('button', { name: /Emitir informe$/ }).click();
    await expect(personal.getByText(/El informe fue guardado/)).toBeVisible();
    await expect(
      personal.getByText('Radiografía de tórax sin alteraciones evidentes.'),
    ).toBeVisible();
    await expect(personal.getByRole('button', { name: /Imprimir informe$/ })).toBeVisible();

    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await ir(paciente, '/mis-estudios');
    const tarjeta = paciente.locator('article', { hasText: orden.codigo });
    await expect(tarjeta).toBeVisible();
    await expect(tarjeta.getByText('Informada')).toBeVisible();
    await tarjeta.getByRole('button', { name: /Ver informe$/ }).click();
    await expect(
      tarjeta.getByText('Radiografía de tórax sin alteraciones evidentes.'),
    ).toBeVisible();
    await expect(tarjeta.getByText('Tos persistente')).toHaveCount(0);
    await tarjeta.getByRole('button', { name: 'Ver toma.png' }).click();
    const visorPaciente = paciente.getByRole('dialog', { name: 'Visor de imágenes' });
    await expect(visorPaciente.locator('img.imagen')).toBeVisible();
  });

  test('solo quienes tienen relación o rol acceden a la orden y a sus imágenes', async ({
    browser,
  }) => {
    test.setTimeout(90_000);
    const base = await (await browser.newContext()).newPage();
    await iniciarSesion(base, 'medico');
    const orden = await solicitarPorApi(base, true);
    const tokenImagenes = await tokenDe(base, 'imagenes');
    const imagenes = { Authorization: `Bearer ${tokenImagenes}` };
    const programada = await base.request.put(`/api/v1/imagenes/ordenes/${orden.id}/programar`, {
      headers: imagenes,
      data: {
        programadaPara: `${new Date(Date.now() + 40 * 86_400_000).toISOString().slice(0, 10)}T11:00:00`,
        sala: `Sala seg ${Date.now()}`,
      },
    });
    expect(programada.status()).toBe(200);
    const subida = await base.request.post(`/api/v1/imagenes/ordenes/${orden.id}/archivos`, {
      headers: imagenes,
      multipart: { archivos: { name: 'toma.png', mimeType: 'image/png', buffer: PNG } },
    });
    expect(subida.status()).toBe(201);
    const archivoId = ((await subida.json()) as { id: string }[])[0].id;
    await base.request.put(`/api/v1/imagenes/ordenes/${orden.id}/realizar`, {
      headers: imagenes,
      data: {},
    });

    const rutaOrden = `/api/v1/imagenes/ordenes/${orden.id}`;
    const rutaArchivo = `${rutaOrden}/archivos/${archivoId}`;
    for (const rol of ['paciente2', 'medico2'] as const) {
      const headers = { Authorization: `Bearer ${await tokenDe(base, rol)}` };
      expect((await base.request.get(rutaOrden, { headers })).status()).toBe(403);
      expect((await base.request.get(rutaArchivo, { headers })).status()).toBe(403);
    }

    const recepcion = { Authorization: `Bearer ${await tokenDe(base, 'recepcion')}` };
    const vista = await base.request.get(rutaOrden, { headers: recepcion });
    expect(vista.status()).toBe(200);
    const cuerpo = (await vista.json()) as Record<string, unknown>;
    expect(cuerpo['indicacionClinica']).toBeUndefined();
    expect(cuerpo['informe']).toBeUndefined();
    expect(cuerpo['dni']).toBeUndefined();
    expect((await base.request.get(rutaArchivo, { headers: recepcion })).status()).toBe(403);

    const paciente = { Authorization: `Bearer ${await tokenDe(base, 'paciente')}` };
    expect((await base.request.get(rutaArchivo, { headers: paciente })).status()).toBe(403);
    const propia = (await (
      await base.request.get(rutaOrden, { headers: paciente })
    ).json()) as Record<string, unknown>;
    expect(propia['indicacionClinica']).toBeUndefined();
    expect(propia['archivos']).toEqual([]);
  });

  test('administración y recepción ven la agenda de salas sin entrar a la ficha clínica', async ({
    browser,
  }) => {
    for (const rol of ['recepcion', 'admin'] as const) {
      const pagina = await (await browser.newContext()).newPage();
      await iniciarSesion(pagina, rol);
      await ir(pagina, '/imagenes/agenda');
      await expect(
        pagina.getByRole('heading', { level: 1, name: 'Agenda de salas' }),
      ).toBeVisible();
      await expect(pagina.locator('app-agenda-salas .sala').first()).toBeVisible();
      await expect(pagina.getByText('Abrir orden')).toHaveCount(0);
    }
  });

  test('el paciente no accede al módulo de imágenes y el personal ve su menú', async ({
    browser,
  }) => {
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await paciente.goto('/imagenes/');
    await expect(paciente).toHaveURL(/\/paciente\/citas$/);
    await expect(paciente.getByRole('link', { name: 'Mis estudios' })).toBeVisible();

    const personal = await (await browser.newContext()).newPage();
    await iniciarSesion(personal, 'imagenes');
    await expect(personal.getByRole('link', { name: 'Agenda de salas' })).toBeVisible();
    await expect(personal.getByRole('link', { name: 'Catálogo de estudios' })).toBeVisible();
    await ir(personal, '/imagenes/catalogo');
    await personal.getByPlaceholder('Buscar estudio').fill('tórax');
    await expect(
      personal.getByRole('cell', { name: /Radiografía de tórax/ }).first(),
    ).toBeVisible();
    expect(CUENTAS.imagenes.email).toBe('imagenes@tusalud.com');
  });
});
