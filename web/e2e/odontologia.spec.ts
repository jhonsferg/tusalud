import { Page, expect, test } from '@playwright/test';

import { iniciarSesion, ir, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

async function elegirPaciente(page: Page, texto: string, nombre: string): Promise<void> {
  await page.getByLabel('Buscar por nombre o DNI').fill(texto);
  await page.getByRole('button', { name: 'Buscar', exact: true }).click();
  await page.getByRole('option', { name: new RegExp(nombre) }).click();
}

test.describe('odontología', () => {
  test('la odontóloga ve el odontograma con el estado de cada pieza y su historial', async ({
    page,
  }) => {
    await iniciarSesion(page, 'odontologa');
    await ir(page, '/odontograma');
    await expect(page.getByRole('heading', { level: 1, name: 'Odontograma' })).toBeVisible();

    await elegirPaciente(page, '12345678', 'Juan');

    await expect(page.getByRole('group', { name: 'Odontograma' })).toBeVisible();
    await expect(
      page.getByRole('button', { name: /^Pieza 16,.*Oclusal o incisal obturado/ }),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: /^Pieza 36,.*Ausente/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /^Pieza 11,.*Corona/ })).toBeVisible();

    await page.getByRole('button', { name: /^Pieza 16,/ }).click();
    await expect(page.getByRole('heading', { name: 'Pieza 16' })).toBeVisible();
    await expect(page.getByText('solo lectura')).toBeVisible();
    await expect(page.getByText('Caries oclusal')).toBeVisible();
  });

  test('el odontograma se recorre con el teclado', async ({ page }) => {
    await iniciarSesion(page, 'odontologa');
    await ir(page, '/odontograma');
    await elegirPaciente(page, '12345678', 'Juan');

    const pieza16 = page.getByRole('button', { name: /^Pieza 16,/ });
    await pieza16.focus();
    await page.keyboard.press('ArrowRight');
    await expect(page.getByRole('button', { name: /^Pieza 15,/ })).toBeFocused();
    await page.keyboard.press('ArrowDown');
    await expect(page.getByRole('button', { name: /^Pieza 45,/ })).toBeFocused();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('heading', { name: 'Pieza 45' })).toBeVisible();
  });

  test('la odontóloga registra un cambio en una pieza y queda en el historial', async ({
    page,
  }) => {
    await iniciarSesion(page, 'odontologa');
    await ir(page, '/odontograma');
    await elegirPaciente(page, '12345678', 'Juan');

    await page.getByRole('button', { name: /^Pieza 47,/ }).click();
    await page.getByLabel('Observación').fill(`Control e2e ${Date.now()}`);
    await page.getByRole('button', { name: 'Registrar cambio' }).click();

    await expect(page.getByText('Cambio registrado en el historial')).toBeVisible();
    await expect(page.getByText(/Control e2e/).first()).toBeVisible();
  });

  test('el paciente acepta un plan presentado y la odontóloga lo cierra', async ({ page }) => {
    await iniciarSesion(page, 'odontologa');
    const tokenOdontologa = await tokenDe(page, 'odontologa');
    const pacientes = await page.request.get('/api/v1/vacunacion/pacientes?q=12345678', {
      headers: cabecera(tokenOdontologa),
    });
    const [{ id: pacienteId }] = (await pacientes.json()) as { id: string }[];
    const procedimientos = (await (
      await page.request.get('/api/v1/odontologia/procedimientos', {
        headers: cabecera(tokenOdontologa),
      })
    ).json()) as { id: string; codigo: string }[];
    const profilaxis = procedimientos.find((p) => p.codigo === 'PROFILAXIS')!;

    const plan = (await (
      await page.request.post('/api/v1/odontologia/planes', {
        headers: cabecera(tokenOdontologa),
        data: { pacienteId, notas: 'Plan e2e' },
      })
    ).json()) as { id: string };
    await page.request.post(`/api/v1/odontologia/planes/${plan.id}/items`, {
      headers: cabecera(tokenOdontologa),
      data: { procedimientoId: profilaxis.id },
    });
    await page.request.post(`/api/v1/odontologia/planes/${plan.id}/presentar`, {
      headers: cabecera(tokenOdontologa),
    });

    const contexto = await page.context().browser()!.newContext();
    const paciente = await contexto.newPage();
    await iniciarSesion(paciente, 'paciente');
    await ir(paciente, '/mi-salud-dental');
    await expect(
      paciente.getByRole('heading', { level: 1, name: 'Mi salud dental' }),
    ).toBeVisible();
    await paciente
      .getByRole('button', { name: /Presentado/ })
      .first()
      .click();
    await paciente.getByRole('button', { name: 'Aceptar el plan' }).click();
    await expect(paciente.getByText('Aceptaste el plan de tratamiento')).toBeVisible();
    await contexto.close();

    const cierre = await page.request.post(`/api/v1/odontologia/planes/${plan.id}/cancelar`, {
      headers: cabecera(tokenOdontologa),
      data: { motivo: 'Cierre de la prueba e2e' },
    });
    expect(cierre.status()).toBe(200);
  });

  test('caja ve los planes con saldo y recepción no accede a los datos odontológicos', async ({
    page,
  }) => {
    await iniciarSesion(page, 'caja');
    await ir(page, '/cobros-odontologicos');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Cobros odontológicos' }),
    ).toBeVisible();
    const tabla = page.getByRole('table', { name: 'Planes odontológicos por cobrar' });
    await expect(tabla).toBeVisible();
    await expect(tabla.getByRole('row', { name: /Juan/ }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: /^Cobrar el plan de/ }).first()).toBeVisible();

    const tokenRecepcion = await tokenDe(page, 'recepcion');
    const bloqueado = await page.request.get('/api/v1/odontologia/mio/odontograma', {
      headers: cabecera(tokenRecepcion),
    });
    expect(bloqueado.status()).toBe(403);
  });

  test('administración gestiona el catálogo de procedimientos con precios', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/procedimientos-odontologicos');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Procedimientos odontológicos' }),
    ).toBeVisible();
    const tabla = page.getByRole('table', { name: 'Catálogo de procedimientos odontológicos' });
    await expect(tabla.getByRole('row', { name: /Obturación con resina/ })).toBeVisible();
    await expect(
      tabla.getByRole('row', { name: /Corona dental.*Requiere consentimiento/ }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Editar Obturación con resina' }).click();
    await expect(page.getByRole('heading', { name: 'Editar procedimiento' })).toBeVisible();
  });
});
