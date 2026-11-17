import { expect, test } from '@playwright/test';

import { iniciarSesion } from './support';

test.describe('calendario', () => {
  test('el paciente ve sus citas en las vistas de mes, semana y día', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByText('Calendario', { exact: true }).click();

    const vistas = page.getByRole('group', { name: 'Vista del calendario' });
    await vistas.getByRole('button', { name: 'Mes' }).click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();

    await vistas.getByRole('button', { name: 'Semana' }).click();
    await expect(page.getByRole('button', { name: /^Abrir / }).first()).toBeVisible();

    await vistas.getByRole('button', { name: 'Día' }).click();
    await page.getByRole('button', { name: 'Hoy' }).click();
    await expect(page.locator('.ahora')).toBeVisible();

    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect(page.locator('.ahora')).toHaveCount(0);
  });

  test('el paciente elige un hueco libre en el calendario para reservar', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/citas/nueva?medicoId=1');
    await expect(page.getByText('Por elegir')).toBeVisible();

    await page.getByRole('button', { name: 'Siguiente' }).click();
    const huecos = page.getByRole('button', { name: /^Elegir / });
    await expect(huecos.first()).toBeVisible();
    await huecos.first().click();
    await expect(huecos.first()).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByText('Por elegir')).toHaveCount(0);

    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    await expect(page.getByText(/\d+ libres/).first()).toBeVisible();
  });

  test('exige elegir una hora antes de confirmar la reserva', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/citas/nueva?medicoId=1');
    await page.locator('#cita-motivo').fill('Sin hora elegida');
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();
    await expect(page.getByText('Elige una hora libre en el calendario.')).toBeVisible();
    await expect(page).toHaveURL(/\/citas\/nueva/);
  });

  test('el médico alterna entre el día y el calendario de su agenda', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.getByText('Calendario', { exact: true }).click();
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();
    await page.getByText('Día', { exact: true }).first().click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toHaveCount(0);
  });
});
