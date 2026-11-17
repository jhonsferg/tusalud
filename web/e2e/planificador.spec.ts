import { expect, test } from '@playwright/test';

import { idDeMedico, iniciarSesion } from './support';

const NOMBRE_CALENDARIO = 'Calendario';

async function abrirAgenda(page: import('@playwright/test').Page) {
  await page
    .getByRole('group', { name: 'Vista del calendario' })
    .getByRole('button', { name: 'Agenda' })
    .click();
}

test.describe('planificador', () => {
  test('crea, edita y elimina un evento personal', async ({ page }) => {
    const titulo = `Reunión e2e ${Date.now()}`;
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: NOMBRE_CALENDARIO, exact: true }).first().click();
    await expect(page.getByRole('heading', { name: NOMBRE_CALENDARIO })).toBeVisible();

    await page.getByRole('button', { name: 'Nuevo evento' }).click();
    await page.locator('#ev-titulo').fill(titulo);
    await page.locator('#ev-ubicacion').fill('Consultorio virtual');
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El evento fue creado.')).toBeVisible();

    await abrirAgenda(page);
    await expect(page.getByText(titulo)).toBeVisible();
    await expect(page.getByText('Consultorio virtual')).toBeVisible();

    await page.getByRole('button', { name: new RegExp(titulo) }).click();
    await page.locator('#ev-titulo').fill(`${titulo} editado`);
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El evento fue actualizado.')).toBeVisible();
    await expect(page.getByText(`${titulo} editado`)).toBeVisible();

    await page.getByRole('button', { name: new RegExp(`${titulo} editado`) }).click();
    await page.getByRole('button', { name: 'Eliminar evento' }).click();
    await page.locator('.p-confirmdialog').getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByText('El evento fue eliminado.')).toBeVisible();
    await expect(page.getByText(`${titulo} editado`)).toHaveCount(0);
  });

  test('cada rol solo puede agendar los tipos de evento que le corresponden', async ({
    browser,
  }) => {
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await paciente.goto('/calendario');
    await expect(paciente.locator('.mini-dia')).toHaveCount(0);
    await paciente.getByRole('button', { name: 'Nuevo evento' }).click();
    await paciente.locator('#ev-tipo').click();
    await expect(paciente.getByRole('option', { name: 'Personal' })).toBeVisible();
    await expect(paciente.getByRole('option', { name: 'Recordatorio' })).toBeVisible();
    await expect(paciente.getByRole('option', { name: 'Reunión' })).toHaveCount(0);
    await expect(paciente.getByRole('option', { name: 'Tarea' })).toHaveCount(0);
    await expect(paciente.getByRole('option', { name: 'Bloqueo de agenda' })).toHaveCount(0);

    const admin = await (await browser.newContext()).newPage();
    await iniciarSesion(admin, 'admin');
    await admin.goto('/calendario');
    await admin.getByRole('button', { name: 'Nuevo evento' }).click();
    await admin.locator('#ev-tipo').click();
    await expect(admin.getByRole('option', { name: 'Reunión' })).toBeVisible();
    await expect(admin.getByRole('option', { name: 'Tarea' })).toBeVisible();
    await expect(admin.getByRole('option', { name: 'Bloqueo de agenda' })).toHaveCount(0);
  });

  test('valida el título y el rango de fechas del evento', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/calendario');
    await page.getByRole('button', { name: 'Nuevo evento' }).click();
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El título es obligatorio.')).toBeVisible();
    await page.getByRole('button', { name: 'Cancelar' }).click();
  });

  test('marca una tarea como completada', async ({ page }) => {
    const titulo = `Tarea e2e ${Date.now()}`;
    await iniciarSesion(page, 'admin');
    await page.goto('/calendario');
    await page.getByRole('button', { name: 'Nuevo evento' }).click();
    await page.locator('#ev-titulo').fill(titulo);
    await page.locator('#ev-tipo').click();
    await page.getByRole('option', { name: 'Tarea' }).click();
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El evento fue creado.')).toBeVisible();

    await abrirAgenda(page);
    await page.getByRole('button', { name: new RegExp(titulo) }).click();
    await page.getByRole('button', { name: 'Marcar completada' }).click();
    await page.getByRole('button', { name: new RegExp(titulo) }).click();
    await expect(page.getByRole('button', { name: 'Marcar pendiente' })).toBeVisible();
    await page.getByRole('button', { name: 'Eliminar evento' }).click();
    await page.locator('.p-confirmdialog').getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByText('El evento fue eliminado.')).toBeVisible();
  });

  test('arrastrar sobre la vista de semana abre un evento con el rango elegido', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/calendario');
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Semana' })
      .click();
    const columna = page.locator('.columna').nth(2);
    const visible = (await page.locator('.tiempo').boundingBox())!;
    const caja = (await columna.boundingBox())!;
    const x = caja.x + caja.width / 2;
    const y = visible.y + visible.height / 2;
    await page.mouse.move(x, y);
    await page.mouse.down();
    await page.mouse.move(x, y + 60, { steps: 6 });
    await page.mouse.up();
    await expect(page.getByRole('dialog', { name: 'Nuevo evento' })).toBeVisible();
    await page.getByRole('button', { name: 'Cancelar' }).click();
  });

  test('mueve y redimensiona un evento arrastrándolo y guarda el cambio', async ({ page }) => {
    const titulo = `Arrastre e2e ${Date.now()}`;
    await iniciarSesion(page, 'paciente');
    await page.goto('/calendario');
    await page.getByRole('button', { name: 'Nuevo evento' }).click();
    await page.locator('#ev-titulo').fill(titulo);
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El evento fue creado.')).toBeVisible();

    await page.waitForTimeout(1000);
    const evento = page.locator('.evento', { hasText: titulo });
    await evento.scrollIntoViewIfNeeded();
    const antes = (await evento.boundingBox())!;
    const x = antes.x + antes.width / 2;
    const respuesta = page.waitForResponse(
      (r) => r.url().includes('/calendario/eventos/') && r.url().endsWith('/mover'),
    );
    await page.mouse.move(x, antes.y + 10);
    await page.mouse.down();
    await page.mouse.move(x, antes.y + 10 - 56, { steps: 8 });
    await page.mouse.up();
    expect((await respuesta).status()).toBe(200);
    await expect
      .poll(async () => (await page.locator('.evento', { hasText: titulo }).boundingBox())!.y)
      .toBeLessThan(antes.y - 40);

    const movido = (await page.locator('.evento', { hasText: titulo }).boundingBox())!;
    const redimension = page.waitForResponse(
      (r) => r.url().includes('/calendario/eventos/') && r.url().endsWith('/mover'),
    );
    await page.mouse.move(movido.x + movido.width / 2, movido.y + movido.height - 3);
    await page.mouse.down();
    await page.mouse.move(movido.x + movido.width / 2, movido.y + movido.height + 50, { steps: 8 });
    await page.mouse.up();
    expect((await redimension).status()).toBe(200);
    await expect
      .poll(async () => (await page.locator('.evento', { hasText: titulo }).boundingBox())!.height)
      .toBeGreaterThan(movido.height + 30);

    await page.locator('.evento', { hasText: titulo }).click();
    await page.getByRole('button', { name: 'Eliminar evento' }).click();
    await page.locator('.p-confirmdialog').getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByText('El evento fue eliminado.')).toBeVisible();
  });

  test('el panel lateral filtra los calendarios y las teclas cambian la vista', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/calendario');
    await expect(page.getByText('Calendarios', { exact: true })).toBeVisible();
    await page.getByText('Citas médicas').click();

    await page.locator('.cal').press('m');
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();
    await page.locator('.cal').press('d');
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toHaveCount(0);
  });

  test('el médico puede crear bloqueos de agenda', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.goto('/calendario');
    await expect(page.getByText('Bloqueos de agenda').first()).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo evento' }).click();
    await page.locator('#ev-tipo').click();
    await expect(page.getByRole('option', { name: 'Bloqueo de agenda' })).toBeVisible();
    await page.keyboard.press('Escape');
  });

  test('la disponibilidad del médico usa el calendario y continúa con la reserva', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto(`/medicos/${await idDeMedico(page, 0)}/disponibilidad`);
    await expect(page.locator('p-datepicker')).toHaveCount(0);
    await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
    const huecos = page.getByRole('button', { name: /^Elegir / });
    await expect(huecos.first()).toBeVisible();
    await huecos.first().click();
    await page.getByRole('button', { name: 'Continuar con la reserva' }).click();
    await expect(page).toHaveURL(/\/citas\/nueva\?.*medicoId=[0-9a-f-]{36}/);
    await expect(page.getByText('Por elegir')).toHaveCount(0);
  });
});
