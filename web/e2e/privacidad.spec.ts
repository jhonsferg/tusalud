import { expect, test } from '@playwright/test';

import { iniciarSesion, ir, registrarPaciente } from './support';

test.describe('protección de datos personales', () => {
  test('la política vigente es pública y no pide sesión', async ({ page }) => {
    await page.goto('/privacidad');

    await expect(page.getByRole('heading', { level: 1 })).toContainText('Política de privacidad');
    await expect(page.getByText(/vigente desde/)).toBeVisible();
    await expect(page.getByText(/Huella de esta versión/)).toBeVisible();
  });

  test('el paciente autoriza y revoca una finalidad opcional y no puede revocar la necesaria', async ({
    page,
  }) => {
    await registrarPaciente(page, 'priv');
    await ir(page, '/privacidad');

    await expect(page.getByRole('heading', { name: 'Mi privacidad' })).toBeVisible();
    await expect(page.getByText('Necesario para su atención').first()).toBeVisible();

    await page.getByRole('button', { name: 'Autorizar Comunicaciones comerciales' }).click();
    await expect(page.getByText('Autorizado')).toBeVisible();
    await page
      .getByRole('button', { name: 'Revocar la autorización de Comunicaciones comerciales' })
      .click();
    await expect(page.getByText('Revocado')).toBeVisible();
    await expect(
      page.getByRole('button', { name: /Revocar la autorización de Atención/ }),
    ).toHaveCount(0);
  });

  test('una solicitud de acceso recorre todo el flujo hasta descargar los datos', async ({
    page,
    browser,
  }) => {
    await registrarPaciente(page, 'arco');
    await ir(page, '/privacidad');
    await page.getByRole('tab', { name: /Mis solicitudes/ }).click();
    await page.getByRole('button', { name: 'Nueva solicitud' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Nueva solicitud' });
    await dialogo.getByRole('button', { name: 'Enviar solicitud' }).click();
    await expect(dialogo.getByText(/al menos 10 caracteres/)).toBeVisible();
    await dialogo.locator('#sol-desc').fill('Quiero una copia de mis datos personales');
    await dialogo.getByRole('button', { name: 'Enviar solicitud' }).click();
    await expect(dialogo).toBeHidden();

    const fila = page.getByRole('row', { name: /ARCO-/ }).first();
    await expect(fila.getByText('Recibida')).toBeVisible();
    const codigo = (await fila.getByRole('cell').first().textContent())?.trim() ?? '';
    expect(codigo).toMatch(/^ARCO-/);

    const contexto = await browser.newContext();
    const calidad = await contexto.newPage();
    await iniciarSesion(calidad, 'calidad');
    await ir(calidad, '/privacidad/solicitudes');
    const filaCalidad = calidad.getByRole('row', { name: new RegExp(codigo) });
    await expect(filaCalidad).toBeVisible();
    await filaCalidad.getByRole('button', { name: /Gestionar la solicitud/ }).click();
    const gestion = calidad.getByRole('dialog', { name: new RegExp(codigo) });
    await gestion.getByRole('button', { name: 'Asumir la solicitud' }).click();
    await gestion.locator('#res-texto').fill('Se adjunta la copia de sus datos personales');
    await gestion.getByRole('button', { name: 'Registrar resolución' }).click();
    await expect(gestion).toBeHidden();
    await contexto.close();

    await page.reload();
    await page.getByRole('tab', { name: /Mis solicitudes/ }).click();
    const resuelta = page.getByRole('row', { name: new RegExp(codigo) });
    await expect(resuelta.getByText('Atendida')).toBeVisible();
    await resuelta.getByRole('button', { name: /Ver la solicitud/ }).click();
    const descarga = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Descargar mis datos' }).click();
    expect((await descarga).suggestedFilename()).toMatch(/\.json$/);
  });

  test('la cancelación muestra antes qué datos se conservan y por qué', async ({ page }) => {
    await registrarPaciente(page, 'canc');
    await ir(page, '/privacidad');
    await page.getByRole('tab', { name: /Mis solicitudes/ }).click();
    await page.getByRole('button', { name: 'Nueva solicitud' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Nueva solicitud' });
    await dialogo.getByRole('combobox').first().click();
    await page.getByRole('option', { name: 'Cancelación' }).click();

    await expect(dialogo.getByText('Qué se puede cancelar y qué se conserva')).toBeVisible();
    await expect(dialogo.getByText('Se conserva').first()).toBeVisible();
    await expect(dialogo.getByText(/Registros de auditoría/)).toBeVisible();
  });

  test('calidad administra el registro de tratamientos, la retención y los incidentes', async ({
    page,
  }) => {
    await iniciarSesion(page, 'calidad');

    await ir(page, '/privacidad/registro');
    await expect(page.getByRole('heading', { name: 'Registro de tratamientos' })).toBeVisible();
    await expect(page.getByText('Pacientes e historias clínicas').first()).toBeVisible();

    await ir(page, '/privacidad/retencion');
    await expect(page.getByText(/nunca se elimina de forma automática/)).toBeVisible();
    await page.getByRole('button', { name: 'Ejecutar ahora' }).click();
    await expect(page.getByText(/La retención procesó/)).toBeVisible();

    await ir(page, '/privacidad/incidentes');
    await page.getByRole('button', { name: 'Registrar incidente' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Registrar un incidente' });
    await dialogo
      .locator('#inc-desc')
      .fill('Un usuario consultó historias sin relación asistencial');
    await dialogo.locator('#inc-datos').fill('Historias clínicas');
    await dialogo.getByRole('button', { name: 'Registrar' }).click();
    await expect(dialogo).toBeHidden();
    await expect(page.getByText('INC-').first()).toBeVisible();
  });

  test('un paciente no entra a la gestión de privacidad', async ({ page }) => {
    await registrarPaciente(page, 'gest');
    await ir(page, '/privacidad/solicitudes');
    await expect(page.getByRole('heading', { name: 'Solicitudes de privacidad' })).toHaveCount(0);
    await expect(page.getByText(/no encontrada/i).first()).toBeVisible();
  });
});
