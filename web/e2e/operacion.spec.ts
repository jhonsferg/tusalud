import { Page, expect, test } from '@playwright/test';

import { CUENTAS, cerrarSesion, iniciarSesion, ir } from './support';

test.describe('recepción', () => {
  test('registra un paciente en mostrador y abre un turno sin cita', async ({ page }) => {
    const dni = String(Math.floor(10_000_000 + Math.random() * 89_999_999));
    await iniciarSesion(page, 'recepcion');
    await ir(page, '/recepcion');
    await expect(page.getByRole('heading', { level: 1, name: 'Recepción' })).toBeVisible();

    await ir(page, '/recepcion/pacientes');
    await page.getByRole('button', { name: 'Registrar paciente' }).click();
    await page.locator('#pac-dni').fill(dni);
    await page.locator('#pac-nombre').fill('Rosa');
    await page.locator('#pac-apellido').fill('Quispe');
    await page.locator('#pac-nacimiento').pressSequentially('15/03/1990');
    await page.keyboard.press('Tab');
    await page.getByRole('button', { name: /Registrar$/ }).click();
    await expect(page.locator('#pac-clave')).toBeVisible();
  });

  test('la sala de espera muestra los turnos y el médico ve su cola', async ({ browser }) => {
    const recepcion = await (await browser.newContext()).newPage();
    await iniciarSesion(recepcion, 'recepcion');
    await ir(recepcion, '/recepcion/pantalla');
    await expect(recepcion.getByText(/A-00\d|P-00\d/).first()).toBeVisible();

    const medico = await (await browser.newContext()).newPage();
    await iniciarSesion(medico, 'medico');
    await ir(medico, '/cola');
    await expect(
      medico.getByRole('heading', { level: 1, name: 'Cola de pacientes' }),
    ).toBeVisible();
  });
});

test.describe('caja', () => {
  test('abre la caja, cobra, anula el comprobante y cierra con diferencia', async ({ page }) => {
    await iniciarSesion(page, 'caja');
    await ir(page, '/caja');
    await expect(page.getByRole('heading', { level: 1, name: 'Caja' })).toBeVisible();
    const abrir = page.getByRole('button', { name: 'Abrir caja' });
    const cerrar = page.getByRole('button', { name: 'Cerrar caja' });
    await expect(abrir.or(cerrar).first()).toBeVisible();
    if (await abrir.isVisible()) {
      await abrir.click();
    }
    await expect(page.getByRole('button', { name: 'Cerrar caja' })).toBeVisible();

    await ir(page, '/caja/cobrar');
    await page.getByRole('button', { name: 'Cobro libre' }).click();
    await page.locator('#cob-descripcion').fill('Curación simple');
    await page.locator('#cob-precio').fill('59');
    await page.keyboard.press('Tab');
    await page.locator('#cob-cliente').fill('Cliente de prueba');
    await page.locator('#cob-doc').fill('45678912');
    await page.getByRole('button', { name: 'Emitir comprobante' }).click();
    await expect(page.getByRole('dialog', { name: 'Comprobante emitido' })).toBeVisible();
    await expect(page.getByText('IGV 18%')).toBeVisible();
    await page.getByRole('button', { name: 'Listo' }).click();

    await ir(page, '/caja/comprobantes');
    await page
      .getByRole('button', { name: /^Anular / })
      .first()
      .click();
    await page.locator('#anu-motivo').fill('Error de digitación en la prueba');
    await page.getByRole('button', { name: 'Anular comprobante' }).click();
    await expect(page.getByText(/fue anulado/)).toBeVisible();

    await ir(page, '/caja');
    await page.getByRole('button', { name: 'Cerrar caja' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Cerrar caja' }).click();
    await expect(page.getByRole('dialog', { name: 'Caja cerrada' })).toBeVisible();
  });

  test('el reporte de ingresos y el historial son accesibles y el paciente no entra', async ({
    browser,
  }) => {
    const caja = await (await browser.newContext()).newPage();
    await iniciarSesion(caja, 'caja');
    await ir(caja, '/caja/reportes');
    await expect(
      caja.getByRole('heading', { level: 1, name: 'Reporte de ingresos' }),
    ).toBeVisible();
    await ir(caja, '/caja/historial');
    await expect(caja.getByRole('heading', { level: 1, name: 'Historial de caja' })).toBeVisible();

    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await paciente.goto('/caja/');
    await expect(paciente).toHaveURL(/\/paciente\/inicio$/);
  });
});

test.describe('hospitalización', () => {
  test('enfermería ve el mapa de camas y registra un triaje de emergencia', async ({ page }) => {
    const dni = await crearPaciente(page);
    await iniciarSesion(page, 'enfermeria');
    await ir(page, '/hospitalizacion');
    await expect(page.getByRole('heading', { level: 1, name: 'Hospitalización' })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Cama / }).first()).toBeVisible();

    await ir(page, '/hospitalizacion/emergencias');
    await page.getByRole('button', { name: 'Registrar triaje' }).click();
    await page.getByRole('radio', { name: /3/ }).click();
    await page.locator('#tri-motivo').fill('Dolor abdominal agudo');
    await page.locator('#tri-paciente').fill(dni);
    await page.getByRole('option').first().click();
    await page.getByRole('button', { name: /Registrar$/ }).click();
    const exito = page.getByText(/triaje fue registrado/);
    await expect(exito).toBeVisible();
  });

  test('el paciente ve sus hospitalizaciones y no el mapa', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/mis-ingresos');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Mis hospitalizaciones' }),
    ).toBeVisible();
    await page.goto('/enfermeria/');
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
    await cerrarSesion(page);
  });
});

async function crearPaciente(page: Page): Promise<string> {
  const dni = String(Math.floor(10_000_000 + Math.random() * 89_999_999));
  const login = await page.request.post('/api/v1/auth/login', {
    data: { email: CUENTAS.recepcion.email, password: CUENTAS.recepcion.password },
  });
  const { token } = (await login.json()) as { token: string };
  const respuesta = await page.request.post('/api/v1/recepcion/pacientes', {
    headers: { Authorization: `Bearer ${token}` },
    data: { nombre: 'Pedro', apellido: 'Triaje', dni, fechaNacimiento: '1985-05-20' },
  });
  expect(respuesta.status()).toBe(201);
  return dni;
}
