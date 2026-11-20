import { readFileSync } from 'node:fs';

import { Page, expect, test } from '@playwright/test';

import { iniciarSesion, ir, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

async function elegirPaciente(page: Page, texto: string, nombre: string): Promise<void> {
  await page.getByLabel('Buscar por nombre o DNI').fill(texto);
  await page.getByRole('button', { name: /Buscar$/ }).click();
  await page.getByRole('option', { name: new RegExp(nombre) }).click();
}

test.describe('vacunación y crecimiento', () => {
  test('la pediatra consulta el carné de la lactante con dosis atrasadas y la próxima dosis', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico2');
    await ir(page, '/vacunacion');
    await expect(page.getByRole('heading', { level: 1, name: 'Vacunación' })).toBeVisible();

    await elegirPaciente(page, 'Luciana', 'Luciana Ramos');

    await expect(page.getByRole('heading', { name: 'Luciana Ramos' })).toBeVisible();
    await expect(page.getByText('Próxima dosis:')).toBeVisible();
    await expect(
      page.getByRole('table', { name: 'Dosis del esquema de vacunación' }),
    ).toBeVisible();
    await expect(page.getByText('Atrasada').first()).toBeVisible();
    await expect(page.getByText('Aplicada').first()).toBeVisible();
    await expect(page.getByText(/pendiente de validación/)).toBeVisible();
  });

  test('la pediatra registra una dosis y la anula, y el carné lo refleja', async ({ page }) => {
    await iniciarSesion(page, 'medico2');
    await ir(page, '/vacunacion');
    await elegirPaciente(page, 'Luciana', 'Luciana Ramos');

    await page.getByRole('button', { name: 'Registrar la dosis 3 de Pentavalente' }).click();
    await page.getByLabel('Lote').fill(`E2E-${Date.now()}`);
    await page.getByRole('button', { name: 'Guardar dosis' }).click();
    await expect(page.getByText('Dosis registrada en el carné')).toBeVisible();

    const anular = page.getByRole('button', { name: 'Anular la dosis 3 de Pentavalente' });
    await expect(anular).toBeVisible();
    await anular.click();
    await page.getByLabel('Motivo de la anulación').fill('Cierre de la prueba e2e');
    await page.getByRole('button', { name: 'Anular dosis' }).click();
    await expect(page.getByText('Dosis anulada')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Registrar la dosis 3 de Pentavalente' }),
    ).toBeVisible();
  });

  test('la paciente ve su carné y descarga el PDF con la marca de verificación', async ({
    page,
  }) => {
    await iniciarSesion(page, 'lactante');
    await ir(page, '/mi-vacunacion');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Mi carné de vacunación' }),
    ).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Luciana Ramos' })).toBeVisible();

    const descarga = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Descargar carné en PDF' }).click();
    const archivo = await descarga;

    expect(archivo.suggestedFilename()).toMatch(/^carne-vacunacion-.*\.pdf$/);
    const bytes = readFileSync(await archivo.path());
    expect(bytes.subarray(0, 4).toString()).toBe('%PDF');
  });

  test('la pediatra ve las curvas con la alerta de IMC de la niña con aumento de peso', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico2');
    await ir(page, '/crecimiento');
    await expect(page.getByRole('heading', { level: 1, name: 'Crecimiento' })).toBeVisible();

    await elegirPaciente(page, 'Valentina', 'Valentina Torres');

    await expect(page.getByText('Índice de masa corporal para la edad').first()).toBeVisible();
    await expect(page.getByText(/IMC por encima del rango esperado/).first()).toBeVisible();
    await expect(page.getByRole('img', { name: /Peso para la edad/ })).toBeVisible();
    await expect(
      page.getByText(/referencia resumida|resumen de los patrones/i).first(),
    ).toBeVisible();
  });

  test('la paciente solo ve sus propias curvas y no puede registrar mediciones', async ({
    page,
  }) => {
    await iniciarSesion(page, 'lactante');
    await ir(page, '/mi-crecimiento');
    await expect(page.getByRole('heading', { level: 1, name: 'Mi crecimiento' })).toBeVisible();
    await expect(page.getByRole('img', { name: /Talla para la edad/ })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Guardar medición' })).toHaveCount(0);

    const token = await tokenDe(page, 'lactante');
    const propias = await page.request.get('/api/v1/crecimiento/mio', { headers: cabecera(token) });
    expect(propias.status()).toBe(200);
    const respuesta = await page.request.post('/api/v1/crecimiento/mediciones', {
      headers: cabecera(token),
      data: { pacienteId: (await propias.json()).pacienteId, fecha: '2026-01-01', pesoKg: 8 },
    });
    expect(respuesta.status()).toBe(403);
  });

  test('un paciente no accede al carné de otro y enfermería sin atención vigente tampoco', async ({
    page,
  }) => {
    await iniciarSesion(page, 'lactante');
    const tokenLactante = await tokenDe(page, 'lactante');
    const propio = await page.request.get('/api/v1/vacunacion/mio', {
      headers: cabecera(tokenLactante),
    });
    const { pacienteId } = (await propio.json()) as { pacienteId: string };

    const tokenOtro = await tokenDe(page, 'paciente');
    const ajeno = await page.request.get(`/api/v1/vacunacion/pacientes/${pacienteId}/carne`, {
      headers: cabecera(tokenOtro),
    });
    expect(ajeno.status()).toBe(403);

    const tokenEnfermeria = await tokenDe(page, 'enfermeria');
    const sinAtencion = await page.request.get(`/api/v1/vacunacion/pacientes/${pacienteId}/carne`, {
      headers: cabecera(tokenEnfermeria),
    });
    expect(sinAtencion.status()).toBe(403);
  });

  test('enfermería y administración tienen sus accesos a vacunación en el menú', async ({
    page,
  }) => {
    await iniciarSesion(page, 'enfermeria');
    await expect(page.getByRole('link', { name: 'Vacunación' }).first()).toBeVisible();
    await expect(page.getByRole('link', { name: 'Crecimiento' }).first()).toBeVisible();

    const contexto = await page.context().browser()!.newContext();
    const admin = await contexto.newPage();
    await iniciarSesion(admin, 'admin');
    await ir(admin, '/esquemas-vacunacion');
    await expect(
      admin.getByRole('heading', { level: 1, name: 'Esquemas de vacunación' }),
    ).toBeVisible();
    await expect(admin.getByText('Por validar').first()).toBeVisible();
    await contexto.close();
  });
});
