import { expect, test } from '@playwright/test';

import { iniciarSesion } from './support';

test.describe('perfil', () => {
  test('el paciente ve y actualiza sus datos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: 'Mi perfil' }).first().click();
    await expect(page).toHaveURL(/\/perfil$/);
    await expect(page.locator('#per-email')).toHaveValue('juan.perez@correo.com');
    await expect(page.locator('#per-dni')).toHaveValue('12345678');
    await expect(page.getByText('Historia clínica')).toBeVisible();

    await expect(page.locator('#per-nombre')).toHaveValue('Juan');
    await page.locator('#per-telefono').fill('999888777');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Tus datos fueron actualizados.')).toBeVisible();

    await page.reload();
    await expect(page.locator('#per-telefono')).toHaveValue('999888777');
  });

  test('valida los datos personales', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/perfil');
    await page.locator('#per-nombre').fill('');
    await page.locator('#per-telefono').fill('123');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Este campo es obligatorio.')).toBeVisible();
    await expect(page.getByText('El teléfono debe tener 9 dígitos.')).toBeVisible();
  });

  test('el médico edita su consultorio y ve sus datos profesionales', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.goto('/perfil');
    await expect(page.locator('#per-colegiatura')).toHaveValue('CMP-12345');
    await expect(page.getByText('Cardiología')).toBeVisible();
    await page.locator('#per-consultorio').fill('Consultorio 101');
    await page.locator('#per-nombre').fill('Luis');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Tus datos fueron actualizados.')).toBeVisible();
  });

  test('el administrador ve su perfil sin datos de paciente ni médico', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/perfil');
    await expect(page.locator('#per-email')).toHaveValue('admin@tusalud.com');
    await expect(page.locator('#per-dni')).toHaveCount(0);
    await expect(page.locator('#per-colegiatura')).toHaveCount(0);
  });

  test('cambia la contraseña con validaciones y vuelve a la original', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/perfil');
    await page.getByRole('tab', { name: 'Seguridad' }).click();

    await page.locator('#per-actual').fill('Incorrecta123!');
    await page.locator('#per-nueva').fill('NuevaClave456!');
    await page.locator('#per-confirmar').fill('Distinta456!');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Las contraseñas no coinciden.')).toBeVisible();

    await page.locator('#per-confirmar').fill('NuevaClave456!');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('La contraseña actual no es correcta')).toBeVisible();

    await page.locator('#per-actual').fill('Paciente123!');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Tu contraseña fue actualizada.')).toBeVisible();

    await page.getByRole('button', { name: 'Cerrar sesión' }).first().click();
    await page.goto('/auth/login');
    await page.getByLabel('Email').fill('juan.perez@correo.com');
    await page.locator('#login-password').fill('Paciente123!');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page.getByText('Email o contraseña incorrectos.')).toBeVisible();

    await page.locator('#login-password').fill('NuevaClave456!');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page).toHaveURL(/\/citas$/);

    await page.goto('/perfil');
    await page.getByRole('tab', { name: 'Seguridad' }).click();
    await page.locator('#per-actual').fill('NuevaClave456!');
    await page.locator('#per-nueva').fill('Paciente123!');
    await page.locator('#per-confirmar').fill('Paciente123!');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Tu contraseña fue actualizada.')).toBeVisible();
  });
});
