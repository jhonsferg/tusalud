import { expect, test } from '@playwright/test';

import { iniciarSesion, reservarPrimeraHoraLibre } from './support';

test.describe('paciente', () => {
  test('ve el catálogo de especialidades y médicos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: 'Especialidades' }).first().click();
    await expect(
      page.getByRole('cell', { name: 'Cardiología', exact: true }).first(),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nueva especialidad' })).toHaveCount(0);

    await page.getByRole('link', { name: 'Médicos' }).first().click();
    await expect(page.getByRole('heading', { name: 'Luis García' })).toBeVisible();
    await page.getByRole('button', { name: 'Ver perfil' }).first().click();
    await expect(page.getByText('Horarios de atención')).toBeVisible();
  });

  test('reserva una cita y la cancela', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/medicos');
    await page
      .locator('article', { hasText: 'Luis García' })
      .getByRole('button', { name: 'Reservar' })
      .click();
    await expect(page).toHaveURL(/\/citas\/nueva\?medicoId=/);

    await reservarPrimeraHoraLibre(page, 'Control de rutina e2e');

    await expect(page).toHaveURL(/\/citas\/[0-9a-f-]{36}$/);
    await expect(page.getByText('Control de rutina e2e')).toBeVisible();
    await expect(page.getByText('Pendiente', { exact: true })).toBeVisible();

    await page.getByRole('button', { name: 'Cancelar cita' }).click();
    await page.getByRole('button', { name: 'Sí, cancelar cita' }).click();
    await expect(page.getByText('Cancelada', { exact: true })).toBeVisible();
  });

  test('elegir un solo día en el filtro de fechas ya filtra las citas', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/citas');
    await expect(page.locator('p-datepicker input')).toBeVisible();
    await page.locator('p-datepicker input').click();
    const peticion = page.waitForRequest((r) => {
      const q = new URL(r.url());
      const inicio = q.searchParams.get('inicio');
      return q.pathname.endsWith('/mis-citas') && !!inicio && inicio === q.searchParams.get('fin');
    });
    await page.locator('.p-datepicker-panel .p-datepicker-today').first().click();
    await peticion;
  });

  test('no puede entrar a las secciones de administración', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/admin/auditoria');
    await expect(page).toHaveURL(/\/citas$/);
    await page.goto('/medicos/nuevo');
    await expect(page).toHaveURL(/\/citas$/);
  });
});

test.describe('médico', () => {
  test('ve su agenda y confirma una cita pendiente', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await expect(page.getByRole('heading', { name: 'Mi agenda' })).toBeVisible();
    await page.getByRole('button', { name: 'Día siguiente' }).click();
    const confirmar = page.getByRole('button', { name: 'Confirmar', exact: true });
    if (await confirmar.count()) {
      await confirmar.first().click();
      await page.getByRole('button', { name: 'Confirmar cita' }).click();
      await expect(page.getByText('La cita fue confirmada.')).toBeVisible();
    }
  });
});

test.describe('administrador', () => {
  test('ve la auditoría con sus indicadores', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/admin/auditoria');
    await expect(page.getByRole('heading', { name: 'Auditoría' })).toBeVisible();
    await expect(page.getByText('Citas por estado')).toBeVisible();
    await expect(page.getByText('Detalle de citas')).toBeVisible();
  });

  test('crea, edita y elimina una especialidad', async ({ page }) => {
    const nombre = `Prueba ${Date.now()}`;
    await iniciarSesion(page, 'admin');
    await page.goto('/especialidades/nueva');
    await page.locator('#esp-nombre').fill(nombre);
    await page.locator('#esp-descripcion').fill('Creada por e2e');
    await page.locator('#esp-precio').pressSequentially('100');
    await page.getByRole('button', { name: 'Crear especialidad' }).click();
    await expect(page.getByRole('cell', { name: nombre, exact: true })).toBeVisible();

    await page.getByRole('button', { name: `Editar ${nombre}` }).click();
    await expect(page.locator('#esp-nombre')).toHaveValue(nombre);
    await page.locator('#esp-descripcion').fill('Editada por e2e');
    await page.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(page.getByText('Editada por e2e')).toBeVisible();

    await page.getByRole('button', { name: `Eliminar ${nombre}` }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByRole('cell', { name: nombre, exact: true })).toHaveCount(0);
  });

  test('configura los horarios de un médico', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.goto('/medicos');
    await page.getByRole('button', { name: 'Ver perfil' }).first().click();
    await page.getByRole('button', { name: 'Gestionar horarios' }).click();
    await expect(page.getByRole('heading', { name: 'Bloques configurados' })).toBeVisible();
    await page.getByRole('button', { name: 'Agregar bloque' }).click();
    await expect(page.getByText('Este campo es obligatorio.')).toBeVisible();
  });
});

test.describe('diseño adaptable @responsive', () => {
  test('el menú lateral se abre desde el botón en móvil', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Abrir menú' }).click();
    await expect(page.getByRole('link', { name: 'Reservar cita' })).toBeVisible();
  });
});
