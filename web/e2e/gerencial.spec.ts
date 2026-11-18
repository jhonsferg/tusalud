import { Page, expect, test } from '@playwright/test';

import { CUENTAS, cerrarSesion, iniciarSesion, tokenDe, ir } from './support';

function diaAleatorioFuturo(): string {
  const fecha = new Date(Date.now() + (20 + Math.floor(Math.random() * 100)) * 86_400_000);
  return fecha.toISOString().slice(0, 10);
}

async function solicitarAusenciaComo(
  page: Page,
  motivo: string,
): Promise<{ id: string; fecha: string }> {
  const token = await tokenDe(page, 'recepcion');
  for (let intento = 0; intento < 8; intento++) {
    const fecha = diaAleatorioFuturo();
    const respuesta = await page.request.post('/api/v1/rrhh/mis-ausencias', {
      headers: { Authorization: `Bearer ${token}` },
      data: { tipo: 'PERMISO', fechaInicio: fecha, fechaFin: fecha, motivo },
    });
    if (respuesta.status() === 201) {
      const { id } = (await respuesta.json()) as { id: string };
      return { id, fecha };
    }
  }
  throw new Error('No se pudo crear la solicitud de ausencia');
}

async function cancelarComoEmpleado(page: Page, id: string): Promise<void> {
  const token = await tokenDe(page, 'recepcion');
  await page.request.put(`/api/v1/rrhh/mis-ausencias/${id}/cancelar`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {},
  });
}

test.describe('reportes gerenciales', () => {
  test('gerencia ve el reporte ejecutivo y navega por los reportes de cada área', async ({
    page,
  }) => {
    await iniciarSesion(page, 'gerencia');
    await ir(page, '/reportes/ejecutivo');
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte ejecutivo' })).toBeVisible();
    await expect(page.getByText('Tasa de ausencia')).toBeVisible();
    await expect(page.getByText('Ocupación hospitalaria')).toBeVisible();
    await expect(page.locator('.variacion').first()).toBeVisible();

    await page.getByRole('link', { name: 'Financiero' }).first().click();
    await expect(page).toHaveURL(/\/gerencia\/reportes\/financiero$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte financiero' })).toBeVisible();
    await expect(page.getByText('Total cobrado')).toBeVisible();

    await page.getByRole('link', { name: 'Operativo' }).first().click();
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte operativo' })).toBeVisible();

    await page.getByRole('link', { name: 'Calidad' }).first().click();
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte de calidad' })).toBeVisible();

    await page.getByRole('link', { name: 'Resumen de plantilla' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Recursos humanos' })).toBeVisible();
    await expect(page.getByText('Masa salarial mensual')).toBeVisible();
  });

  test('el rango de un solo día y la agrupación recargan el reporte', async ({ page }) => {
    await iniciarSesion(page, 'gerencia');
    await ir(page, '/reportes/ejecutivo');
    await expect(page.getByText('Tasa de ausencia')).toBeVisible();
    const peticion = page.waitForRequest(
      (r) => r.url().includes('/reportes/ejecutivo') && r.url().includes('agrupacion=SEMANA'),
    );
    await page.getByRole('button', { name: 'Por semana' }).click();
    await peticion;
  });

  test('finanzas solo ve el reporte financiero y no entra al operativo', async ({ page }) => {
    await iniciarSesion(page, 'finanzas');
    await ir(page, '/reportes/financiero');
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte financiero' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Reporte operativo' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Reporte de calidad' })).toHaveCount(0);
    await page.goto('/finanzas/reportes/operativo');
    await expect(page).toHaveURL(/\/finanzas\/inicio$/);
    await page.goto('/finanzas/reportes/ejecutivo');
    await expect(page).toHaveURL(/\/finanzas\/inicio$/);
    await page.goto('/finanzas/rrhh/empleados');
    await expect(page.getByRole('heading', { name: /no encontrada/i })).toBeVisible();
  });

  test('operaciones ve el mapa de demanda y el inventario crítico', async ({ page }) => {
    await iniciarSesion(page, 'operaciones');
    await ir(page, '/reportes/operativo');
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte operativo' })).toBeVisible();
    await expect(page.getByText('Demanda de citas por día y hora')).toBeVisible();
    await expect(
      page.getByRole('table', { name: 'Mapa de calor de la demanda de citas' }),
    ).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Inventario crítico' })).toBeVisible();
    await expect(page.getByText('Camas por área')).toBeVisible();
  });

  test('calidad ve las valoraciones y las tasas de cancelación', async ({ page }) => {
    await iniciarSesion(page, 'calidad');
    await ir(page, '/reportes/calidad');
    await expect(page.getByRole('heading', { level: 1, name: 'Reporte de calidad' })).toBeVisible();
    await expect(page.getByText('Valoración promedio').first()).toBeVisible();
    await expect(page.getByText('Valoraciones por médico')).toBeVisible();
    await expect(page.getByText('Cancelaciones y ausencias por especialidad')).toBeVisible();
    await page.goto('/calidad/reportes/financiero');
    await expect(page).toHaveURL(/\/calidad\/inicio$/);
  });
});

test.describe('recursos humanos', () => {
  test('RR. HH. ve la plantilla, asigna un turno y lo retira', async ({ page }) => {
    await iniciarSesion(page, 'rrhh');
    await ir(page, '/rrhh');
    await expect(page.getByRole('heading', { level: 1, name: 'Recursos humanos' })).toBeVisible();
    await expect(page.getByText('Masa salarial mensual')).toBeVisible();
    await expect(page.getByText('Contratos por vencer')).toBeVisible();

    await page.getByRole('link', { name: 'Turnos' }).click();
    await expect(page).toHaveURL(/\/rrhh\/turnos$/);
    const semanas = 6 + Math.floor(Math.random() * 40);
    for (let i = 0; i < semanas; i++) {
      await page.getByRole('button', { name: 'Semana siguiente' }).click();
    }
    const agregar = page.locator('button[aria-label^="Asignar turno a"]').first();
    await expect(agregar).toBeVisible();
    await agregar.click();
    await page.locator('#tur-plantilla').click();
    await page.getByRole('option', { name: /^Mañana/ }).click();
    await page.getByRole('button', { name: /Asignar$/ }).click();
    await expect(page.getByText('El turno fue asignado.')).toBeVisible();

    const quitar = page.locator('button[aria-label^="Quitar el turno"]').first();
    await expect(quitar).toBeVisible();
    await quitar.click();
    await expect(page.getByText('El turno fue eliminado.')).toBeVisible();
  });

  test('RR. HH. busca empleados y ve la ficha con el sueldo', async ({ page }) => {
    await iniciarSesion(page, 'rrhh');
    await page.getByRole('link', { name: 'Empleados', exact: true }).click();
    await expect(page).toHaveURL(/\/rrhh\/empleados$/);
    await page.getByLabel('Buscar empleado').fill('Carla');
    await expect(page.getByRole('cell', { name: /Carla Núñez/ }).first()).toBeVisible();
    await page.getByRole('button', { name: /Ver ficha de Carla/ }).click();
    await expect(page.getByRole('dialog').getByText('Sueldo')).toBeVisible();
    await expect(page.getByRole('dialog').getByText(/días disponibles/)).toBeVisible();
  });

  test('RR. HH. aprueba y rechaza solicitudes de ausencia de otro empleado', async ({ page }) => {
    const aprobada = await solicitarAusenciaComo(page, `E2E aprobar ${Date.now()}`);
    const rechazada = await solicitarAusenciaComo(page, `E2E rechazar ${Date.now()}`);
    try {
      await iniciarSesion(page, 'rrhh');
      await page.getByRole('link', { name: 'Ausencias', exact: true }).click();
      await expect(page).toHaveURL(/\/rrhh\/ausencias$/);

      const fila1 = page.getByRole('row').filter({ hasText: `E2E aprobar` }).first();
      await expect(fila1).toBeVisible();
      await fila1.getByRole('button', { name: /^Aprobar/ }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Aprobar' }).click();
      await expect(page.getByText('La solicitud fue aprobada.')).toBeVisible();

      const fila2 = page.getByRole('row').filter({ hasText: `E2E rechazar` }).first();
      await expect(fila2).toBeVisible();
      await fila2.getByRole('button', { name: /^Rechazar/ }).click();
      await page.getByRole('dialog').getByRole('button', { name: 'Rechazar' }).click();
      await expect(page.getByText('Indica el motivo del rechazo.')).toBeVisible();
      await page.locator('#aus-resolucion').fill('Cobertura insuficiente ese día');
      await page.getByRole('dialog').getByRole('button', { name: 'Rechazar' }).click();
      await expect(page.getByText('La solicitud fue rechazada.')).toBeVisible();
    } finally {
      await cancelarComoEmpleado(page, aprobada.id);
      await cancelarComoEmpleado(page, rechazada.id);
    }
  });

  test('RR. HH. alterna al calendario de ausencias', async ({ page }) => {
    await iniciarSesion(page, 'rrhh');
    await page.getByRole('link', { name: 'Ausencias', exact: true }).click();
    await page.getByRole('button', { name: 'Calendario' }).click();
    await expect(page.getByRole('table', { name: 'Calendario de ausencias' })).toBeVisible();
  });

  test('el administrador ve la plantilla sin sueldos ni motivos', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await page.getByRole('link', { name: 'Resumen de plantilla' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Recursos humanos' })).toBeVisible();
    await expect(page.getByText('Masa salarial mensual')).toHaveCount(0);
    await page.getByRole('link', { name: 'Empleados', exact: true }).click();
    await expect(page.getByRole('columnheader', { name: /Sueldo/ })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Nuevo empleado' })).toHaveCount(0);
  });
});

test.describe('mi espacio del personal', () => {
  test('un empleado ve sus turnos y su ficha sin el sueldo', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    await page.getByRole('link', { name: 'Mi espacio' }).click();
    await expect(page).toHaveURL(/\/recepcion\/mi-espacio\/turnos$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Mi espacio' })).toBeVisible();
    await page.getByRole('link', { name: 'Mi ficha' }).click();
    await expect(page.getByText('Carla Núñez')).toBeVisible();
    await expect(page.getByText('Sueldo')).toHaveCount(0);
  });

  test('un empleado solicita una ausencia y la cancela', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    await page.goto('/recepcion/mi-espacio/ausencias');
    await expect(page.getByText(/Vacaciones disponibles/)).toBeVisible();
    await page.getByRole('button', { name: 'Solicitar ausencia' }).click();
    await page.locator('#aus-tipo').click();
    await page.getByRole('option', { name: 'Permiso' }).click();
    await page.locator('#aus-rango').click();
    const meses = 16 + Math.floor(Math.random() * 12);
    for (let i = 0; i < meses; i++) {
      await page.locator('.p-datepicker-next-button').click();
    }
    const dia = page
      .locator('.p-datepicker-day:not(.p-datepicker-day-other)', { hasText: /^12$/ })
      .first();
    await dia.click();
    await dia.click();
    await page.locator('#aus-motivo').fill('Trámite personal de la prueba e2e');
    await page.getByRole('button', { name: 'Enviar solicitud' }).click();
    await expect(page.getByText('Tu solicitud fue enviada a recursos humanos.')).toBeVisible();

    await page
      .getByRole('button', { name: /^Cancelar la ausencia del/ })
      .first()
      .click();
    await expect(page.getByText('La ausencia fue cancelada.')).toBeVisible();
  });

  test('el enlace de las notificaciones de ausencia lleva a la pantalla correcta', async ({
    page,
  }) => {
    await iniciarSesion(page, 'recepcion');
    await page.goto('/recepcion/ausencias');
    await expect(page).toHaveURL(/\/recepcion\/mi-espacio\/ausencias$/);
    await cerrarSesion(page);
    await iniciarSesion(page, 'rrhh');
    await page.goto('/rrhh/ausencias');
    await expect(page).toHaveURL(/\/rrhh\/ausencias$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Ausencias' })).toBeVisible();
  });
});

test.describe('acceso a los módulos gerenciales', () => {
  test('un paciente no entra a gerencia ni a recursos humanos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.goto('/gerencia/');
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
    await page.goto('/rrhh/');
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
    await page.goto('/finanzas/reportes/financiero');
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
  });

  test('un empleado de otra área vuelve a su módulo desde gerencia y recursos humanos', async ({
    page,
  }) => {
    await iniciarSesion(page, 'caja');
    await page.goto('/gerencia/');
    await expect(page).toHaveURL(/\/caja\/$/);
    await page.goto('/rrhh/empleados');
    await expect(page).toHaveURL(/\/caja\/$/);
  });
});
