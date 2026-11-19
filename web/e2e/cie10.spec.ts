import { readFileSync } from 'node:fs';

import { APIRequestContext, Download, Page, expect, test } from '@playwright/test';

import { CUENTAS, ir, iniciarSesion, tokenDe } from './support';

function contenido(descarga: Download): Promise<Buffer> {
  return descarga.path().then((ruta) => readFileSync(ruta));
}

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

function fechaPresencialAleatoria(): string {
  for (;;) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 2 + Math.floor(Math.random() * 40));
    if (fecha.getDay() !== 0 && fecha.getDay() !== 6) {
      const hora = 8 + Math.floor(Math.random() * 6);
      const minutos = Math.random() < 0.5 ? '00' : '30';
      const dia = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
      return `${dia}T${String(hora).padStart(2, '0')}:${minutos}:00`;
    }
  }
}

async function citaConfirmada(page: Page): Promise<string> {
  const request: APIRequestContext = page.request;
  const tokenPaciente = await tokenDe(page, 'paciente');
  const tokenMedico = await tokenDe(page, 'medico');
  const medicos = (await (
    await request.get('/api/v1/medicos', { headers: cabecera(tokenPaciente) })
  ).json()) as { id: string; apellido: string; especialidades: { id: string }[] }[];
  const garcia = medicos.find((m) => m.apellido.includes('García'))!;
  for (let intento = 0; intento < 15; intento++) {
    const respuesta = await request.post('/api/v1/citas', {
      headers: cabecera(tokenPaciente),
      data: {
        medicoId: garcia.id,
        especialidadId: garcia.especialidades[0].id,
        fechaHora: fechaPresencialAleatoria(),
        motivo: 'Control con codificación CIE-10 e2e',
        modalidad: 'PRESENCIAL',
      },
    });
    if (respuesta.status() === 201) {
      const { id } = (await respuesta.json()) as { id: string };
      const confirmada = await request.put(`/api/v1/citas/${id}/confirmar`, {
        headers: cabecera(tokenMedico),
      });
      expect(confirmada.status()).toBe(200);
      return id;
    }
  }
  throw new Error('No se pudo reservar una cita libre');
}

async function elegirDiagnostico(page: Page, consulta: string, codigo: string): Promise<void> {
  const campo = page.locator('#at-cie10');
  await campo.fill(consulta);
  const opcion = page.getByRole('option').filter({ hasText: codigo }).first();
  await expect(opcion).toBeVisible();
  await opcion.click();
}

test.describe('codificación clínica CIE-10', () => {
  test('el médico codifica la atención con principal y secundario y el paciente ve los códigos', async ({
    browser,
  }) => {
    const contextoMedico = await browser.newContext();
    const medico = await contextoMedico.newPage();
    const citaId = await citaConfirmada(medico);
    await iniciarSesion(medico, 'medico');
    await medico.goto(`/medico/citas/${citaId}/atencion`);

    await elegirDiagnostico(medico, 'hipertensión', 'I10');
    await elegirDiagnostico(medico, 'diabetes', 'E11');
    await expect(medico.getByRole('list', { name: 'Diagnósticos elegidos' })).toBeVisible();
    await expect(medico.getByRole('radio', { name: 'Principal' })).toHaveCount(2);
    await expect(medico.getByRole('radio', { name: 'Principal' }).first()).toBeChecked();

    await medico.getByRole('button', { name: 'Guardar atención' }).click();
    await expect(medico.getByText('La atención médica fue guardada.')).toBeVisible();

    const tokenPaciente = await tokenDe(medico, 'paciente');
    const atencion = (await (
      await medico.request.get(`/api/v1/citas/${citaId}/atencion`, {
        headers: cabecera(tokenPaciente),
      })
    ).json()) as { diagnosticos: { codigo: string; principal: boolean }[] };
    expect(atencion.diagnosticos.length).toBe(2);
    expect(atencion.diagnosticos.filter((d) => d.principal)).toHaveLength(1);
    expect(atencion.diagnosticos.find((d) => d.principal)!.codigo).toBe('I10');

    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await ir(paciente, '/historia-clinica');
    await expect(paciente.getByText('I10').first()).toBeVisible();
    await expect(paciente.getByText('E11').first()).toBeVisible();
  });

  test('recepción y administración no ven los diagnósticos de una atención', async ({ page }) => {
    const citaId = await citaConfirmada(page);
    const tokenMedico = await tokenDe(page, 'medico');
    const registro = await page.request.post(`/api/v1/citas/${citaId}/atencion`, {
      headers: cabecera(tokenMedico),
      data: {
        diagnosticos: [{ codigo: 'J00', tipo: 'DEFINITIVO', principal: true }],
      },
    });
    expect(registro.status()).toBe(201);

    for (const rol of ['recepcion', 'admin', 'caja'] as const) {
      const token = await tokenDe(page, rol);
      const respuesta = await page.request.get(`/api/v1/citas/${citaId}/atencion`, {
        headers: cabecera(token),
      });
      expect(respuesta.status(), `el rol ${rol} no debe ver diagnósticos`).toBe(403);
    }
    const tokenPaciente = await tokenDe(page, 'paciente');
    const propia = await page.request.get(`/api/v1/citas/${citaId}/atencion`, {
      headers: cabecera(tokenPaciente),
    });
    expect(propia.status()).toBe(200);
  });

  test('el administrador importa un CSV pequeño y encuentra el código en el catálogo', async ({
    page,
  }) => {
    const sufijo = String(Math.floor(Math.random() * 90) + 10);
    const codigo = `Y${sufijo}.${Math.floor(Math.random() * 9)}`;
    const descripcion = `Entidad de prueba e2e ${sufijo}`;
    const csv = `codigo,descripcion,capitulo,grupo,sexo,edad_minima,edad_maxima\n${codigo},${descripcion},Causas externas,Pruebas,,,\n`;

    await iniciarSesion(page, 'admin');
    await ir(page, '/cie10');
    await expect(page.getByRole('heading', { level: 1, name: 'Catálogo CIE-10' })).toBeVisible();

    await page.locator('#cie-archivo').setInputFiles({
      name: 'prueba-e2e.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv, 'utf8'),
    });
    await page.getByRole('button', { name: 'Vista previa' }).click();
    await expect(page.getByRole('heading', { name: 'Vista previa' })).toBeVisible();
    await expect(page.getByText('con errores')).toBeVisible();

    await page.locator('#cie-etiqueta').fill(`E2E ${Date.now()}`);
    await page.getByRole('button', { name: 'Importar catálogo' }).click();
    await expect(page.getByText('Importación aplicada')).toBeVisible();

    await page.locator('#cie-buscar').fill(descripcion);
    await expect(page.getByRole('cell', { name: codigo, exact: true })).toBeVisible();

    const token = await tokenDe(page, 'admin');
    const importada = await page.request.get(`/api/v1/cie10?q=${encodeURIComponent(codigo)}`, {
      headers: cabecera(token),
    });
    expect(importada.status()).toBe(200);
  });

  test('gerencia ve el reporte de morbilidad agregado y lo exporta', async ({ page }) => {
    await iniciarSesion(page, 'gerencia');
    await ir(page, '/reportes/morbilidad');
    await expect(page.getByRole('heading', { level: 1, name: 'Morbilidad' })).toBeVisible();
    await expect(page.getByRole('note')).toContainText('Solo datos agregados');

    const pdf = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a PDF' }).click();
    const descargaPdf = await pdf;
    expect(descargaPdf.suggestedFilename()).toMatch(/\.pdf$/);
    expect((await contenido(descargaPdf)).subarray(0, 5).toString()).toBe('%PDF-');

    const excel = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Exportar a Excel' }).click();
    const descargaExcel = await excel;
    expect(descargaExcel.suggestedFilename()).toMatch(/\.xlsx$/);
    expect((await contenido(descargaExcel)).subarray(0, 2).toString()).toBe('PK');
  });

  test('el rol sin permiso no accede al reporte de morbilidad ni al catálogo', async ({ page }) => {
    const token = await tokenDe(page, 'recepcion');
    const morbilidad = await page.request.get(
      '/api/v1/reportes/morbilidad?desde=2026-01-01&hasta=2026-12-31&agrupacion=MES',
      { headers: cabecera(token) },
    );
    expect(morbilidad.status()).toBe(403);
    const catalogo = await page.request.get('/api/v1/admin/cie10/resumen', {
      headers: cabecera(token),
    });
    expect(catalogo.status()).toBe(403);
    expect(CUENTAS.recepcion.email).toContain('@');
  });
});
