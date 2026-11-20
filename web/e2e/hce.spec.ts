import { Page, expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, ir, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

async function atencionNueva(page: Page): Promise<string> {
  const pacienteToken = await tokenDe(page, 'paciente');
  const medicoToken = await tokenDe(page, 'medico');
  const medicos = (await (
    await page.request.get('/api/v1/medicos', { headers: cabecera(pacienteToken) })
  ).json()) as { id: string; apellido: string; especialidades: { id: string }[] }[];
  const garcia = medicos.find((m) => m.apellido === 'García')!;
  for (let intento = 0; intento < 10; intento++) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 300 + Math.floor(Math.random() * 400));
    if (fecha.getDay() === 0 || fecha.getDay() === 6) {
      continue;
    }
    const dos = (n: number): string => String(n).padStart(2, '0');
    const hora = 8 + Math.floor(Math.random() * 4);
    const minutos = Math.random() < 0.5 ? '00' : '30';
    const fechaHora = `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(hora)}:${minutos}:00`;
    const cita = await page.request.post('/api/v1/citas', {
      headers: cabecera(pacienteToken),
      data: {
        medicoId: garcia.id,
        especialidadId: garcia.especialidades[0].id,
        fechaHora,
        motivo: 'Historia clínica e2e',
        modalidad: 'PRESENCIAL',
      },
    });
    if (cita.status() !== 201) {
      continue;
    }
    const citaId = ((await cita.json()) as { id: string }).id;
    await page.request.put(`/api/v1/citas/${citaId}/confirmar`, { headers: cabecera(medicoToken) });
    const atencion = await page.request.post(`/api/v1/citas/${citaId}/atencion`, {
      headers: cabecera(medicoToken),
      data: {
        diagnostico: 'Hipertensión leve',
        tratamiento: 'Dieta baja en sal',
        diagnosticos: [{ codigo: 'I10', tipo: 'DEFINITIVO', principal: true }],
      },
    });
    expect(atencion.status()).toBe(201);
    return ((await atencion.json()) as { id: string }).id;
  }
  throw new Error('No se pudo crear una cita libre para la historia clínica');
}

test.describe('historia clínica electrónica', () => {
  test('el médico cierra y firma la atención y la rectifica con una adenda y una corrección que dejan rastro', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    const atencionId = await atencionNueva(page);
    await ir(page, `/historial-clinico/${atencionId}`);

    await expect(page.getByText('Sin cerrar')).toBeVisible();
    await page.getByRole('button', { name: /Cerrar y firmar/ }).click();
    await expect(page.getByText('Cerrada y firmada', { exact: true })).toBeVisible();

    await page.getByLabel('Texto de la adenda').fill('El paciente refiere mejoría');
    await page.getByLabel('Motivo', { exact: true }).first().fill('Seguimiento telefónico');
    await page.getByRole('button', { name: /Agregar adenda/ }).click();
    await expect(page.getByText('Versión 1')).toBeVisible();
    await expect(page.getByText('El paciente refiere mejoría')).toBeVisible();

    await page.getByLabel('Valor corregido').fill('Hipertensión arterial controlada');
    await page.getByLabel('Motivo de la corrección').fill('Precisión del diagnóstico');
    await page.getByRole('button', { name: /Registrar corrección/ }).click();
    await expect(page.getByText('Versión 2')).toBeVisible();
    await expect(page.getByText(/antes «Hipertensión leve»/)).toBeVisible();
  });

  test('el paciente descarga su historia clínica en PDF', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/mi-historia-impresa');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Mi historia clínica' }),
    ).toBeVisible();
    const descarga = page.waitForEvent('download');
    await page.getByRole('button', { name: /Descargar mi historia en PDF/ }).click();
    expect((await descarga).suggestedFilename()).toMatch(/\.pdf$/);
    await cerrarSesion(page);
  });

  test('administración ve las políticas de conservación con la depuración bloqueada', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/hce');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Historia clínica electrónica' }),
    ).toBeVisible();
    await expect(page.getByRole('row', { name: /Historia clínica del paciente/ })).toContainText(
      'Bloqueada',
    );
    await expect(page.getByText(/deben confirmarse con el asesor legal/)).toBeVisible();
  });
});
