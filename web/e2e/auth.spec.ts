import { expect, test } from '@playwright/test';

import { CUENTAS, iniciarSesion, cerrarSesion, ir } from './support';

test.describe('autenticación', () => {
  test('redirige al login cuando no hay sesión', async ({ page }) => {
    await page.goto('/paciente/citas');
    await expect(page).toHaveURL(/\/auth\/login\?returnUrl=%2Fpaciente%2Fcitas/);
    await expect(page.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeVisible();
  });

  test('muestra el error de credenciales inválidas', async ({ page }) => {
    await page.goto('/auth/login');
    await page.getByLabel('Email').fill('nadie@correo.com');
    await page.locator('#login-password').fill('Incorrecta123!');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page.getByText('Email o contraseña incorrectos.')).toBeVisible();
  });

  test('valida los campos obligatorios', async ({ page }) => {
    await page.goto('/auth/login');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page.getByText('Este campo es obligatorio.').first()).toBeVisible();
  });

  for (const rol of ['paciente', 'medico', 'admin'] as const) {
    test(`el ${rol} llega a su página de inicio y puede cerrar sesión`, async ({ page }) => {
      await iniciarSesion(page, rol);
      await expect(page.getByText(CUENTAS[rol].email, { exact: false })).toHaveCount(0);
      await cerrarSesion(page);
      await expect(page).toHaveURL(/\/auth\/login/);
    });
  }

  test('registra a un paciente nuevo', async ({ page }) => {
    const sufijo = Date.now().toString();
    await page.goto('/auth/registro');
    await page.locator('#reg-nombre').fill('Ana');
    await page.locator('#reg-apellido').fill('Prueba');
    await page.locator('#reg-dni').fill(sufijo.slice(-8));
    await page.locator('#reg-nacimiento').pressSequentially('15/05/1990');
    await page.keyboard.press('Escape');
    await page.locator('#reg-email').fill(`ana.${sufijo}@correo.com`);
    await page.locator('#reg-password').fill('Paciente123!');
    await page.getByRole('button', { name: 'Crear cuenta' }).click();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
  });
});
