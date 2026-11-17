import { expect, test } from '@playwright/test';

import {
  formatoFecha,
  iniciarSesion,
  diaHabilAleatorio,
  reservarPrimeraHoraLibre,
} from './support';

test.describe('pagos del paciente', () => {
  test('agrega y elimina una tarjeta', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/pagos/metodos');
    await expect(page.getByText('•••• •••• •••• 4242')).toBeVisible();

    await page.getByRole('button', { name: 'Agregar tarjeta' }).first().click();
    await page.locator('#mp-titular').fill('Juan Perez');
    await page.locator('#mp-numero').fill('5555555555554444');
    await page.locator('#mp-mes').click();
    await page.getByRole('option', { name: '12', exact: true }).click();
    await page.locator('#mp-anio').click();
    await page.getByRole('option').last().click();
    await page.locator('#mp-cvv').fill('123');
    await page.getByRole('button', { name: 'Guardar tarjeta' }).click();
    await expect(page.getByText('Tu método de pago fue agregado.')).toBeVisible();
    await expect(page.getByText('•••• •••• •••• 4444')).toBeVisible();

    await page.getByRole('button', { name: 'Eliminar tarjeta terminada en 4444' }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByText('•••• •••• •••• 4444')).toHaveCount(0);
  });

  test('valida el número de tarjeta', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/pagos/metodos');
    await page.getByRole('button', { name: 'Agregar tarjeta' }).first().click();
    await page.locator('#mp-numero').fill('1234567812345678');
    await page.getByRole('button', { name: 'Guardar tarjeta' }).click();
    await expect(page.getByText('El número de tarjeta no es válido.')).toBeVisible();
  });

  test('reserva, paga con cupón y ve el comprobante', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/citas/nueva?medicoId=1');
    await page.locator('#cita-fecha').pressSequentially(formatoFecha(diaHabilAleatorio()));
    await page.keyboard.press('Escape');
    await reservarPrimeraHoraLibre(page, 'Pago con cupón e2e');

    await page.getByRole('button', { name: 'Pagar cita' }).click();
    await page.locator('#pago-cupon').fill('SALUD20');
    await page.getByRole('button', { name: 'Aplicar' }).click();
    await expect(page.getByText(/cupón/i).first()).toBeVisible();
    await expect(page.getByText('- S/ 30.00')).toBeVisible();
    await page.getByRole('button', { name: 'Pagar ahora' }).click();
    await expect(page.getByText(/Pago registrado/).first()).toBeVisible();
    await expect(page.getByText('Pagada', { exact: true }).first()).toBeVisible();

    await page.goto('/pagos');
    await expect(page.getByRole('cell', { name: /B001-/ }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Ver comprobante' }).first().click();
    await expect(page.getByText('Comprobante de pago')).toBeVisible();
  });
});

test.describe('historia clínica', () => {
  test('el paciente ve su historia y edita sus datos médicos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: 'Historia clínica' }).click();
    await expect(page.getByRole('heading', { name: 'Juan Pérez' })).toBeVisible();
    await expect(page.getByText('Penicilina')).toBeVisible();

    await page.getByRole('button', { name: 'Editar datos médicos' }).click();
    await page.locator('#hc-alergias').fill('Penicilina y polvo');
    await page.locator('form button[type=submit]').click();
    await expect(page.getByText('Tus datos médicos fueron actualizados.')).toBeVisible();
    await expect(page.getByText('Penicilina y polvo')).toBeVisible();
  });

  test('el médico ve sus pacientes y abre una historia', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.getByRole('link', { name: 'Mis pacientes' }).click();
    await expect(page.getByRole('cell', { name: /Carlos Mendoza/ })).toBeVisible();
    await page
      .getByRole('row', { name: /Carlos Mendoza/ })
      .getByRole('button', { name: 'Historia' })
      .click();
    await expect(page.getByRole('heading', { name: 'Carlos Mendoza' })).toBeVisible();
    await expect(page.getByText('Hipertensión arterial controlada')).toBeVisible();
  });
});

test.describe('notificaciones y perfil', () => {
  test('la bandeja lista avisos y permite marcarlos como leídos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/notificaciones');
    await expect(page.getByRole('heading', { name: 'Notificaciones' })).toBeVisible();
    await expect(page.getByText('Nuevo inicio de sesión').first()).toBeVisible();
    await page.getByRole('button', { name: 'Marcar todas como leídas' }).click();
    await expect(page.getByText('0 sin leer')).toBeVisible();
  });

  test('el panel de la campana abre y muestra el estado en vivo', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Abrir notificaciones' }).click();
    await expect(page.getByText('En vivo')).toBeVisible({ timeout: 10_000 });
  });

  test('lista las sesiones activas y marca la actual', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/perfil?tab=seguridad');
    await expect(page.getByText('Sesiones activas')).toBeVisible();
    await expect(page.getByText('Este dispositivo')).toBeVisible();
  });

  test('cambia el modo oscuro y el color principal', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Cambiar a modo oscuro' }).first().click();
    await expect(page.locator('html')).toHaveClass(/app-dark/);
    await page.goto('/perfil?tab=apariencia');
    await page.getByRole('radio', { name: 'Violeta' }).click();
    await expect(page.getByRole('radio', { name: 'Violeta' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });
});

test.describe('administración', () => {
  test('muestra el panel de control con indicadores', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await expect(page.getByRole('heading', { name: 'Panel de control' })).toBeVisible();
    await expect(page.getByText('Ingresos del mes')).toBeVisible();
    await expect(page.getByText('Médicos con más citas atendidas')).toBeVisible();
  });

  test('crea y elimina un cupón', async ({ page }) => {
    const codigo = `E2E${Date.now().toString().slice(-6)}`;
    await iniciarSesion(page, 'admin');
    await page.goto('/admin/cupones');
    await expect(page.getByRole('cell', { name: /BIENVENIDA10/ }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo cupón' }).first().click();
    await page.locator('#cu-codigo').fill(codigo);
    await page.locator('#cu-valor').pressSequentially('15');
    await page.getByRole('button', { name: 'Guardar cupón' }).click();
    await expect(page.getByText('El cupón fue guardado.')).toBeVisible();
    await expect(page.getByRole('cell', { name: new RegExp(codigo) }).first()).toBeVisible();

    await page.getByRole('button', { name: `Eliminar ${codigo}` }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByRole('button', { name: `Eliminar ${codigo}` })).toHaveCount(0);
  });

  test('lista pagos y gestiona usuarios', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/admin/pagos');
    await expect(page.getByRole('cell', { name: /B001-/ }).first()).toBeVisible();

    await page.goto('/admin/usuarios');
    await page.getByPlaceholder('Buscar por nombre o correo').fill('ana.paredes');
    const fila = page.getByRole('row', { name: /Ana Lucía Paredes/ });
    await expect(fila).toBeVisible();
    const reactivar = fila.getByRole('button', { name: 'Activar', exact: true });
    if (await reactivar.count()) {
      await reactivar.click();
      await page
        .locator('.p-confirmdialog')
        .getByRole('button', { name: 'Activar', exact: true })
        .click();
      await expect(page.getByText('El usuario fue activado.')).toBeVisible();
    }
    await fila.getByRole('button', { name: 'Desactivar' }).click();
    await page
      .locator('.p-confirmdialog')
      .getByRole('button', { name: 'Desactivar', exact: true })
      .click();
    await expect(page.getByText('El usuario fue desactivado.')).toBeVisible();
    await fila.getByRole('button', { name: 'Activar' }).click();
    await page
      .locator('.p-confirmdialog')
      .getByRole('button', { name: 'Activar', exact: true })
      .click();
    await expect(page.getByText('El usuario fue activado.')).toBeVisible();
  });
});
