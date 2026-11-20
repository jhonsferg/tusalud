import { readFileSync } from 'node:fs';

import { Page, expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, ir, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

interface Medicamento {
  id: string;
  dci: string;
  concentracion: string;
  formaFarmaceutica: string;
}

async function medicamento(
  page: Page,
  token: string,
  dci: string,
  concentracion: string,
  forma: string,
): Promise<Medicamento> {
  const respuesta = await page.request.get(`/api/v1/medicamentos?q=${encodeURIComponent(dci)}`, {
    headers: cabecera(token),
  });
  const lista = (await respuesta.json()) as Medicamento[];
  const encontrado = lista.find(
    (m) => m.dci === dci && m.concentracion === concentracion && m.formaFarmaceutica === forma,
  );
  if (!encontrado) {
    throw new Error(`No existe ${dci} ${concentracion} ${forma} en el catálogo`);
  }
  return encontrado;
}

async function atencionConReceta(
  page: Page,
  lineas: { medicamento: string; dosis: string }[],
): Promise<{ citaId: string; atencionId: string }> {
  const pacienteToken = await tokenDe(page, 'paciente');
  const medicoToken = await tokenDe(page, 'medico');
  const medicos = (await (
    await page.request.get('/api/v1/medicos', { headers: cabecera(pacienteToken) })
  ).json()) as { id: string; apellido: string; especialidades: { id: string }[] }[];
  const garcia = medicos.find((m) => m.apellido === 'García')!;
  for (let intento = 0; intento < 8; intento++) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 140 + Math.floor(Math.random() * 200));
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
        motivo: 'Receta electrónica e2e',
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
        diagnostico: 'Control con receta',
        diagnosticos: [{ codigo: 'I10', tipo: 'DEFINITIVO', principal: true }],
        pesoKg: 72,
        receta: lineas.map((l) => ({
          medicamento: l.medicamento,
          dosis: l.dosis,
          frecuencia: 'Cada 8 horas',
          duracion: '5 días',
        })),
      },
    });
    expect(atencion.status()).toBe(201);
    return { citaId, atencionId: ((await atencion.json()) as { id: string }).id };
  }
  throw new Error('No se pudo crear una cita libre para la receta');
}

async function emitirPorApi(
  page: Page,
  atencionId: string,
  items: { atencionPosicion: number; medicamentoId: string; cantidad: number }[],
): Promise<{ id: string; numero: string; codigoVerificacion: string }> {
  const medicoToken = await tokenDe(page, 'medico');
  const respuesta = await page.request.post('/api/v1/recetas', {
    headers: cabecera(medicoToken),
    data: { atencionId, items },
  });
  expect(respuesta.status()).toBe(201);
  return (await respuesta.json()) as { id: string; numero: string; codigoVerificacion: string };
}

test.describe('recetas electrónicas', () => {
  test('el médico asocia los medicamentos, justifica la alergia y emite la receta firmada que la paciente descarga', async ({
    page,
  }) => {
    const { citaId } = await atencionConReceta(page, [
      { medicamento: 'Paracetamol 500 mg', dosis: '1 tableta' },
      { medicamento: 'Amoxicilina 500 mg', dosis: '1 cápsula' },
    ]);

    await iniciarSesion(page, 'medico');
    await ir(page, `/recetas/emitir/${citaId}`);
    await expect(
      page.getByRole('heading', { level: 1, name: 'Emitir receta electrónica' }),
    ).toBeVisible();

    await page.getByLabel('Buscar el medicamento de la línea 1').fill('paracetamol');
    await page.getByRole('option', { name: 'Paracetamol 500 mg tableta' }).first().click();
    await expect(page.getByText('Sin alertas para esta prescripción')).toBeVisible();

    await page.getByLabel('Buscar el medicamento de la línea 2').fill('amoxicilina');
    await page.getByRole('option', { name: 'Amoxicilina 500 mg cápsula' }).first().click();
    await expect(page.getByText('Contraindicada').first()).toBeVisible();
    await expect(page.getByText(/alergia a penicilina/i)).toBeVisible();

    const emitir = page.getByRole('button', { name: 'Firmar y emitir' });
    await expect(emitir).toBeDisabled();
    await expect(page.getByText(/Faltan justificaciones/)).toBeVisible();

    await page
      .getByLabel(/Justificación para continuar/)
      .fill('Alergia descartada con una prueba cutánea negativa reciente');
    await expect(emitir).toBeEnabled();
    await emitir.click();

    await expect(page).toHaveURL(/\/medico\/recetas\/[0-9a-f-]{36}$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Receta electrónica' })).toBeVisible();
    await expect(page.getByText(/RX-\d{4}-\d{6}/).first()).toBeVisible();
    await expect(page.getByText('Emitida', { exact: true })).toBeVisible();
    await expect(page.getByText('Válida', { exact: true })).toBeVisible();
    await expect(page.getByText(/no equivale a una firma digital/)).toBeVisible();
    await expect(page.getByText(/Alergia descartada con una prueba cutánea/)).toBeVisible();

    await cerrarSesion(page);
    await iniciarSesion(page, 'paciente');
    await ir(page, '/mis-recetas');
    await expect(page.getByRole('heading', { level: 1, name: 'Mis recetas' })).toBeVisible();
    const descarga = page.waitForEvent('download');
    await page
      .getByRole('button', { name: /Descargar el PDF de la receta RX-/ })
      .first()
      .click();
    const archivo = await descarga;
    expect(archivo.suggestedFilename()).toMatch(/\.pdf$/);
    const ruta = await archivo.path();
    expect(readFileSync(ruta).subarray(0, 5).toString()).toBe('%PDF-');
  });

  test('la verificación pública reconoce la receta y una anulada figura como anulada', async ({
    page,
  }) => {
    const { atencionId } = await atencionConReceta(page, [
      { medicamento: 'Paracetamol 500 mg', dosis: '1 tableta' },
    ]);
    const token = await tokenDe(page, 'medico');
    const paracetamol = await medicamento(page, token, 'Paracetamol', '500 mg', 'Tableta');
    const receta = await emitirPorApi(page, atencionId, [
      { atencionPosicion: 0, medicamentoId: paracetamol.id, cantidad: 10 },
    ]);

    await page.goto(`/verificar/${receta.codigoVerificacion}`);
    await expect(page.getByRole('heading', { name: 'Documento válido' })).toBeVisible();
    await expect(page.getByText('Receta electrónica')).toBeVisible();
    await expect(page.getByText('Juan')).toHaveCount(0);

    await page.request.post(`/api/v1/recetas/${receta.id}/anular`, {
      headers: cabecera(token),
      data: { motivo: 'Anulada en la prueba e2e' },
    });
    await page.goto(`/verificar/${receta.codigoVerificacion}`);
    await expect(page.getByRole('heading', { name: 'Documento anulado' })).toBeVisible();
  });

  test('farmacia dispensa una receta de forma parcial y queda lo pendiente', async ({ page }) => {
    const { atencionId } = await atencionConReceta(page, [
      { medicamento: 'Paracetamol 500 mg', dosis: '1 tableta' },
    ]);
    const token = await tokenDe(page, 'medico');
    const paracetamol = await medicamento(page, token, 'Paracetamol', '500 mg', 'Tableta');
    const receta = await emitirPorApi(page, atencionId, [
      { atencionPosicion: 0, medicamentoId: paracetamol.id, cantidad: 10 },
    ]);

    await iniciarSesion(page, 'farmacia');
    await ir(page, '/recetas-electronicas');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Recetas electrónicas' }),
    ).toBeVisible();
    await page.getByLabel('Número de receta, DNI o nombre del paciente').fill(receta.numero);
    await page.getByRole('button', { name: /Buscar$/ }).click();
    await page
      .getByRole('button', { name: new RegExp(`Abrir la receta ${receta.numero}`) })
      .click();

    await expect(page.getByText('10 de 10')).toBeVisible();
    const cantidad = page.getByLabel('Cantidad a dispensar de Paracetamol 500 mg tableta');
    await cantidad.fill('4');
    await cantidad.press('Tab');
    await page.getByRole('button', { name: /Dispensar$/ }).click();
    await expect(page.getByText(/Dispensación .* registrada/).first()).toBeVisible();

    await page.getByLabel('Número de receta, DNI o nombre del paciente').fill(receta.numero);
    await page.getByRole('button', { name: /Buscar$/ }).click();
    await page
      .getByRole('button', { name: new RegExp(`Abrir la receta ${receta.numero}`) })
      .click();
    await expect(page.getByText('6 de 10')).toBeVisible();
    await expect(page.getByText('Dispensada en parte')).toBeVisible();
  });

  test('administración consulta el catálogo, las interacciones y la plantilla de importación', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/catalogo-medicamentos');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Catálogo de medicamentos' }),
    ).toBeVisible();
    await expect(page.getByText(/por validar con un químico farmacéutico/)).toBeVisible();

    await page.getByLabel('Buscar por nombre o principio activo').fill('warfarina');
    await page.getByRole('button', { name: /Buscar$/ }).click();
    await expect(page.getByRole('rowheader', { name: 'Warfarina' }).first()).toBeVisible();

    await page.getByRole('tab', { name: 'Interacciones' }).click();
    await expect(
      page.getByRole('rowheader', { name: /Warfarina \+ |\+ Warfarina/ }).first(),
    ).toBeVisible();

    await page.getByRole('tab', { name: 'Importación CSV' }).click();
    const descarga = page.waitForEvent('download');
    await page.getByRole('button', { name: /Descargar plantilla/ }).click();
    const archivo = await descarga;
    expect(archivo.suggestedFilename()).toMatch(/\.csv$/);
  });

  test('el paciente ve su receta en la lista pero no accede al catálogo ni a la dispensación', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/mis-recetas');
    await expect(page.getByRole('heading', { level: 1, name: 'Mis recetas' })).toBeVisible();
    const token = await tokenDe(page, 'paciente');
    const catalogo = await page.request.get('/api/v1/medicamentos?q=paracetamol', {
      headers: cabecera(token),
    });
    expect(catalogo.status()).toBe(403);
    const farmacia = await page.request.get('/api/v1/recetas/farmacia', {
      headers: cabecera(token),
    });
    expect(farmacia.status()).toBe(403);
  });
});
