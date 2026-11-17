import { expect, test } from '@playwright/test';

import { iniciarSesion } from './support';

const ID_AJENO = '11111111-1111-4111-8111-111111111111';

test.describe('permisos por rol', () => {
  test('el médico edita los datos médicos de su paciente y queda quién lo hizo', async ({
    page,
  }) => {
    const alergia = `Polen ${Date.now().toString().slice(-6)}`;
    await iniciarSesion(page, 'medico');
    await page.getByRole('link', { name: 'Mis pacientes' }).click();
    await page
      .getByRole('row', { name: /Carlos Mendoza/ })
      .getByRole('button', { name: 'Historia' })
      .click();
    await expect(page.getByRole('heading', { name: 'Carlos Mendoza' })).toBeVisible();
    await page.getByRole('button', { name: 'Editar datos médicos' }).click();
    await page.locator('#hc-alergias').fill(alergia);
    await page.locator('form button[type=submit]').click();
    await expect(
      page.getByText('Los datos médicos del paciente fueron actualizados.'),
    ).toBeVisible();
    await expect(page.getByText(alergia)).toBeVisible();
    await expect(page.locator('#hc-actualizado')).toContainText('Actualizado por');
  });

  test('el administrador no ve ni abre los datos clínicos ni la dispensación', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await expect(page.getByRole('link', { name: 'Mis pacientes' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Dispensar' })).toHaveCount(0);
    await page.goto(`/pacientes/${ID_AJENO}/historia-clinica`);
    await expect(page).toHaveURL(/\/admin\/panel$/);
    await page.goto('/farmacia/dispensar');
    await expect(page).toHaveURL(/\/admin\/panel$/);
    await page.goto(`/laboratorio/ordenes/${ID_AJENO}`);
    await expect(page).toHaveURL(/\/admin\/panel$/);
  });

  test('recepción ve la hospitalización sin poder ingresar ni registrar triaje', async ({
    page,
  }) => {
    await iniciarSesion(page, 'recepcion');
    await page.goto('/hospitalizacion');
    const libre = page.getByRole('button', { name: /^Cama .* Libre$/ }).first();
    await expect(libre).toBeVisible();
    await libre.click();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await page.goto('/hospitalizacion/emergencias');
    await expect(page.getByRole('heading', { level: 1, name: 'Emergencias' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Registrar triaje' })).toHaveCount(0);
  });

  test('el médico ingresa desde el mapa pero no registra triaje', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.goto('/hospitalizacion');
    const libre = page.getByRole('button', { name: /^Cama .* Libre$/ }).first();
    await expect(libre).toBeVisible();
    await libre.click();
    await expect(page.getByRole('dialog')).toBeVisible();
    await page.keyboard.press('Escape');
    await page.goto('/hospitalizacion/emergencias');
    await expect(page.getByRole('button', { name: 'Registrar triaje' })).toHaveCount(0);
  });
});
