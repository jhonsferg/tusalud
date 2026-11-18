import { expect, test } from '@playwright/test';

import { idDeMedico, iniciarSesion, reservarPrimeraHoraLibre, idDeGarcia, ir } from './support';

test.describe('administrador', () => {
  test('registra un médico y configura y elimina un horario', async ({ page }) => {
    const sufijo = Date.now().toString();
    await iniciarSesion(page, 'admin');
    await ir(page, '/medicos/nuevo');
    await page.locator('#med-nombre').fill('Rosa');
    await page.locator('#med-apellido').fill(`Torres${sufijo.slice(-4)}`);
    await page.locator('#med-email').fill(`rosa.${sufijo}@tusalud.com`);
    await page.locator('#med-password').fill('Medico123!');
    await page.locator('#med-colegiatura').fill(`CMP-${sufijo.slice(-6)}`);
    await page.locator('#med-consultorio').fill('Consultorio 300');
    await page.locator('p-multiselect').click();
    await page.getByRole('option').first().click();
    await page.getByRole('button', { name: 'Registrar médico' }).click();

    await expect(page).toHaveURL(/\/medicos\/[0-9a-f-]{36}\/horarios$/);
    await expect(page.getByText('Sin bloques de atención')).toBeVisible();

    await page.locator('#hor-dia').click();
    await page.getByRole('option', { name: 'Lunes' }).click();
    await page.getByRole('button', { name: 'Agregar bloque' }).click();
    await expect(page.getByRole('cell', { name: 'Lunes' })).toBeVisible();
    await expect(page.getByText('08:00 a 13:00')).toBeVisible();

    await page.locator('#hor-dia').click();
    await page.getByRole('option', { name: 'Lunes' }).click();
    await page.locator('#hor-inicio').fill('10:00');
    await page.locator('#hor-fin').fill('11:00');
    await page.getByRole('button', { name: 'Agregar bloque' }).click();
    await expect(page.getByRole('cell', { name: 'Lunes' })).toHaveCount(1);

    await page.getByRole('button', { name: 'Eliminar bloque' }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByText('Sin bloques de atención')).toBeVisible();
  });

  test('valida el formulario de médico y el rango de horas', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/medicos/nuevo');
    await page.getByRole('button', { name: 'Registrar médico' }).click();
    await expect(page.getByText('Este campo es obligatorio.').first()).toBeVisible();
  });

  test('filtra la auditoría por fechas', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/auditoria');
    await expect(page.getByRole('row').nth(1)).toBeVisible();
    await page.locator('p-datepicker input').click();
    const hoy = page.locator('.p-datepicker-panel .p-datepicker-today').first();
    await hoy.click();
    await hoy.click();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('filtra médicos por especialidad y búsqueda', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/medicos');
    const pediatria = page.getByRole('option', { name: 'Pediatría' });
    await expect(async () => {
      await page.locator('p-select.especialidad').click();
      await expect(pediatria).toBeVisible({ timeout: 1500 });
    }).toPass();
    await pediatria.click();
    await expect(page.getByRole('heading', { name: 'Carmen López' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Luis García' })).toHaveCount(0);
    await page.getByPlaceholder('Buscar por nombre').fill('zzzz');
    await expect(page.getByText('No encontramos médicos')).toBeVisible();
  });
});

test.describe('ciclo de vida de una cita', () => {
  test('el paciente reserva, el médico confirma y atiende', async ({ browser }) => {
    test.setTimeout(90_000);
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await ir(paciente, `/citas/nueva?medicoId=${await idDeGarcia(paciente)}`);
    await reservarPrimeraHoraLibre(paciente, 'Chequeo del ciclo completo');
    await expect(paciente).toHaveURL(/\/citas\/[0-9a-f-]{36}$/);
    const url = paciente.url();
    await expect(paciente.getByRole('button', { name: 'Confirmar cita' })).toHaveCount(0);

    const medico = await (await browser.newContext()).newPage();
    await iniciarSesion(medico, 'medico');
    await medico.goto(new URL(url).pathname.replace('/paciente/', '/medico/'));
    await medico.getByRole('button', { name: 'Confirmar cita' }).click();
    await medico
      .locator('.p-confirmdialog')
      .getByRole('button', { name: 'Confirmar cita' })
      .click();
    await expect(medico.getByText('La cita fue confirmada.')).toBeVisible();
    await expect(paciente.getByText('Confirmada', { exact: true })).toBeVisible({
      timeout: 10_000,
    });
    await medico.getByRole('button', { name: 'Registrar atención' }).click();
    await expect(medico).toHaveURL(/\/atencion$/);
    await medico.locator('#at-diagnostico').fill('Control sin hallazgos');
    await medico.getByRole('button', { name: 'Guardar atención' }).click();
    await expect(medico.getByText('La atención médica fue guardada.')).toBeVisible();
    await expect(medico.getByRole('button', { name: 'Editar atención' })).toBeVisible();

    await paciente.reload();
    await expect(paciente.getByText('Atendida', { exact: true })).toBeVisible();
    await expect(paciente.getByRole('button', { name: 'Cancelar cita' })).toHaveCount(0);
    await expect(paciente.getByRole('button', { name: 'Valorar atención' })).toBeVisible();
  });

  test('el paciente ve el historial y filtra por vista', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/citas');
    await page.getByText('Historial', { exact: true }).click();
    await expect(page.getByRole('table')).toBeVisible();
    await page.getByText('Todas', { exact: true }).click();
    await expect(page.getByRole('row').nth(1)).toBeVisible();
  });

  test('el paciente revisa la disponibilidad pública de un médico', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, `/medicos/${await idDeMedico(page, 1)}/disponibilidad`);
    await expect(page.getByRole('heading', { name: 'Disponibilidad' })).toBeVisible();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Siguiente', exact: true })).toBeVisible();
    await ir(page, '/especialidades');
    await expect(
      page.getByRole('cell', { name: 'Cardiología', exact: true }).first(),
    ).toBeVisible();
  });
});
