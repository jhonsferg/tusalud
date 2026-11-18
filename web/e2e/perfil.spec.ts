import { Page, expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, omitirGuia, registrarPaciente, ir } from './support';

test.describe('perfil', () => {
  test('el paciente ve y actualiza sus datos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: 'Mi perfil' }).first().click();
    await expect(page).toHaveURL(/\/perfil$/);
    await expect(page.locator('#per-email')).toHaveValue('juan.perez@correo.com');
    await expect(page.locator('#per-dni')).toHaveValue('12345678');
    await expect(page.locator('app-perfil aside').getByText('Historia clínica')).toBeVisible();

    await expect(page.locator('#per-nombre')).toHaveValue('Juan');
    await page.locator('#per-telefono').fill('999888777');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Tus datos fueron actualizados.')).toBeVisible();

    await page.reload();
    await expect(page.locator('#per-telefono')).toHaveValue('999888777');
  });

  test('la anticipación del recordatorio se elige de una lista y se conserva', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await fijarAnticipacion(page, 1440);
    await ir(page, '/perfil');
    await page.getByRole('tab', { name: /Avisos/ }).click();
    await expect(page.locator('#per-anticipacion')).toBeVisible();
    await expect(page.getByRole('spinbutton')).toHaveCount(0);
    await page.locator('p-select[inputid="per-anticipacion"]').click();
    await page.getByRole('option', { name: '12 horas antes' }).click();
    await page.getByRole('button', { name: /Guardar preferencias$/ }).click();
    await expect(page.getByText(/preferencias/i).first()).toBeVisible();
    await page.reload();
    await page.getByRole('tab', { name: /Avisos/ }).click();
    await expect(page.locator('p-select[inputid="per-anticipacion"]')).toContainText(
      '12 horas antes',
    );
    await fijarAnticipacion(page, 1440);
  });

  test('valida los datos personales', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/perfil');
    await page.locator('#per-nombre').fill('');
    await page.locator('#per-telefono').fill('123');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Este campo es obligatorio.')).toBeVisible();
    await expect(page.getByText('El teléfono debe tener 9 dígitos.')).toBeVisible();
  });

  test('el médico edita su consultorio y ve sus datos profesionales', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/perfil');
    await expect(page.locator('#per-colegiatura')).toHaveValue('CMP-12345');
    await expect(page.getByText('Cardiología')).toBeVisible();
    await page.locator('#per-consultorio').fill('Consultorio 101');
    await page.locator('#per-nombre').fill('Luis');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Tus datos fueron actualizados.')).toBeVisible();
  });

  test('el administrador ve su perfil sin datos de paciente ni médico', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/perfil');
    await expect(page.locator('#per-email')).toHaveValue('admin@tusalud.com');
    await expect(page.locator('#per-dni')).toHaveCount(0);
    await expect(page.locator('#per-colegiatura')).toHaveCount(0);
  });

  test('cambia la contraseña con validaciones y vuelve a la original', async ({ page }) => {
    await omitirGuia(page);
    const cuenta = await registrarPaciente(page, 'clave');
    await ir(page, '/perfil');
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

    await cerrarSesion(page);
    await page.goto('/auth/login');
    await page.getByLabel('Email').fill(cuenta.email);
    await page.locator('#login-password').fill('Paciente123!');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page.getByText('Email o contraseña incorrectos.')).toBeVisible();

    await page.locator('#login-password').fill('NuevaClave456!');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);

    await ir(page, '/perfil');
    await page.getByRole('tab', { name: 'Seguridad' }).click();
    await page.locator('#per-actual').fill('NuevaClave456!');
    await page.locator('#per-nueva').fill('Paciente123!');
    await page.locator('#per-confirmar').fill('Paciente123!');
    await page.getByRole('button', { name: 'Cambiar contraseña' }).click();
    await expect(page.getByText('Tu contraseña fue actualizada.')).toBeVisible();
  });
});

async function fijarAnticipacion(page: Page, minutos: number): Promise<void> {
  const token = await page.evaluate(
    () => JSON.parse(localStorage.getItem('tusalud.session') ?? '{}').token as string,
  );
  const headers = { Authorization: `Bearer ${token}` };
  const actual = await (await page.request.get('/api/v1/perfil/preferencias', { headers })).json();
  await page.request.put('/api/v1/perfil/preferencias', {
    headers,
    data: { ...actual, minutosAntelacionRecordatorio: minutos },
  });
}
