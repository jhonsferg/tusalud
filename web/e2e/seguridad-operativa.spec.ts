import { Page, expect, test } from '@playwright/test';

import { CUENTAS, ir, iniciarSesion, tokenDe } from './support';

interface Pendiente {
  ingresoId: string;
  paciente: string;
}

async function ingresarPacienteDelMedico(
  page: Page,
  motivo: string,
): Promise<{ id: string; camaId: string }> {
  const tokenMedico = await tokenDe(page, 'medico');
  const tokenEnfermeria = await tokenDe(page, 'enfermeria');
  const camas = (await (
    await page.request.get('/api/v1/hospitalizacion/camas?area=HOSPITALIZACION&estado=LIBRE', {
      headers: { Authorization: `Bearer ${tokenEnfermeria}` },
    })
  ).json()) as { id: string }[];
  expect(camas.length).toBeGreaterThan(0);
  const pacientes = (await (
    await page.request.get('/api/v1/medicos/me/pacientes', {
      headers: { Authorization: `Bearer ${tokenMedico}` },
    })
  ).json()) as { pacienteId: string }[];
  for (const paciente of pacientes) {
    const ingreso = await page.request.post('/api/v1/hospitalizacion/ingresos', {
      headers: { Authorization: `Bearer ${tokenMedico}` },
      data: { pacienteId: paciente.pacienteId, camaId: camas[0].id, motivo },
    });
    if (ingreso.status() === 201) {
      const { id } = (await ingreso.json()) as { id: string };
      return { id, camaId: camas[0].id };
    }
  }
  throw new Error('Ningún paciente del médico puede ingresar ahora');
}

test.describe('seguridad operativa', () => {
  test('el administrador ve un inicio de sesión fallido sin el correo ni la clave intentada', async ({
    page,
  }) => {
    const clave = `Incorrecta${Date.now()}!`;
    const respuesta = await page.request.post('/api/v1/auth/login', {
      data: { email: CUENTAS.paciente.email, password: clave },
    });
    expect(respuesta.status()).toBe(401);

    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/seguridad');
    const fila = page.getByRole('row').filter({ hasText: 'Inicio de sesión fallido' }).first();
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('Credenciales inválidas');
    const contenido = await page.locator('app-seguridad').innerText();
    expect(contenido).not.toContain(clave);
    expect(contenido).not.toContain(CUENTAS.paciente.email);

    await page.getByRole('combobox', { name: 'Filtrar por tipo de evento' }).click();
    await page.getByRole('option', { name: 'Inicio de sesión fallido' }).click();
    await expect(
      page.getByRole('row').filter({ hasText: 'Inicio de sesión fallido' }).first(),
    ).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: 'Cuenta desactivada' })).toHaveCount(0);
  });

  test('el inicio del administrador muestra el estado del sistema y el almacenamiento', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/inicio');
    await expect(page.getByRole('heading', { name: 'Estado del sistema' })).toBeVisible();
    await expect(page.getByText('Operativo').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Almacenamiento' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Seguridad', exact: true })).toBeVisible();
    await expect(page.getByText('requieren un endpoint del backend')).toHaveCount(0);
  });

  test('ningún otro rol entra a la página de seguridad', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    const respuesta = await page.request.get('/api/v1/admin/seguridad/eventos', {
      headers: { Authorization: `Bearer ${await tokenDe(page, 'medico')}` },
    });
    expect(respuesta.status()).toBe(403);
  });

  test('enfermería ve en su inicio las evoluciones pendientes que devuelve el sistema', async ({
    page,
  }) => {
    await iniciarSesion(page, 'enfermeria');
    const token = await tokenDe(page, 'enfermeria');
    const pendientes = (await (
      await page.request.get('/api/v1/hospitalizacion/evoluciones-pendientes', {
        headers: { Authorization: `Bearer ${token}` },
      })
    ).json()) as Pendiente[];

    await ir(page, '/inicio');
    await expect(page.getByRole('heading', { name: 'Evoluciones pendientes' })).toBeVisible();
    if (pendientes.length === 0) {
      await expect(page.getByText('Las evoluciones de enfermería están al día.')).toBeVisible();
    } else {
      await expect(page.getByText(pendientes[0].paciente).first()).toBeVisible();
    }
  });

  test('enfermería registra la evolución de un ingreso nuevo y queda en la línea de tiempo', async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await iniciarSesion(page, 'enfermeria');
    const tokenEnfermeria = await tokenDe(page, 'enfermeria');
    const tokenMedico = await tokenDe(page, 'medico');
    const { id, camaId } = await ingresarPacienteDelMedico(page, 'Observación por dolor abdominal');

    try {
      await ir(page, `/hospitalizacion/ingresos/${id}`);
      await expect(page.getByRole('heading', { level: 1, name: /HOS-/ })).toBeVisible();
      await page.locator('#evo-nota').fill('Paciente estable, tolera la vía oral');
      await page.getByRole('button', { name: /Registrar$/ }).click();
      await expect(page.getByText('La evolución fue registrada.')).toBeVisible();
      await expect(page.getByText('Paciente estable, tolera la vía oral')).toBeVisible();
      await expect(page.getByText('Enfermería').first()).toBeVisible();
    } finally {
      await page.request.put(`/api/v1/hospitalizacion/ingresos/${id}/alta`, {
        headers: { Authorization: `Bearer ${tokenMedico}` },
        data: { diagnosticoAlta: 'Observación concluida', indicacionesAlta: 'Control ambulatorio' },
      });
      await page.request.put(`/api/v1/hospitalizacion/camas/${camaId}/estado`, {
        headers: { Authorization: `Bearer ${tokenEnfermeria}` },
        data: { estado: 'LIBRE' },
      });
    }
  });

  test('el médico cancela un ingreso sin evoluciones indicando el motivo', async ({ page }) => {
    test.setTimeout(90_000);
    await iniciarSesion(page, 'medico');
    const tokenMedico = await tokenDe(page, 'medico');
    const { id } = await ingresarPacienteDelMedico(page, 'Ingreso para cancelar en la prueba');

    await ir(page, `/hospitalizacion/ingresos/${id}`);
    await page.getByRole('button', { name: 'Cancelar ingreso' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Cancelar ingreso' });
    await dialogo.getByRole('textbox').fill('no');
    await dialogo.getByRole('button', { name: 'Cancelar ingreso' }).click();
    await expect(dialogo.getByText('Escribe al menos 5 caracteres.')).toBeVisible();
    await dialogo.getByRole('textbox').fill('Ingreso registrado por error');
    await dialogo.getByRole('button', { name: 'Cancelar ingreso' }).click();
    await expect(page.getByText('El ingreso fue cancelado y la cama quedó libre.')).toBeVisible();

    const detalle = await page.request.get(`/api/v1/hospitalizacion/ingresos/${id}`, {
      headers: { Authorization: `Bearer ${tokenMedico}` },
    });
    const cuerpo = (await detalle.json()) as { estado: string; motivoCancelacion: string };
    expect(cuerpo.estado).toBe('CANCELADO');
    expect(cuerpo.motivoCancelacion).toBe('Ingreso registrado por error');
  });
});
