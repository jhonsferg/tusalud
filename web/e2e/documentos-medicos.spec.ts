import { readFileSync } from 'node:fs';

import { APIRequestContext, Download, Page, expect, test } from '@playwright/test';

import { CUENTAS, confirmarMotivo, ir, iniciarSesion, tokenDe } from './support';

const DNI_JUAN = '12345678';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

function aleatorio(): string {
  return Math.random().toString(36).slice(2, 8);
}

function fechaLocal(desplazamientoDias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + desplazamientoDias);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}

function fechaPresencialAleatoria(): string {
  for (;;) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 2 + Math.floor(Math.random() * 40));
    if (fecha.getDay() !== 0 && fecha.getDay() !== 6) {
      const hora = 8 + Math.floor(Math.random() * 6);
      const minutos = Math.random() < 0.5 ? '00' : '30';
      return `${fechaLocal(Math.round((fecha.getTime() - Date.now()) / 86_400_000))}T${String(hora).padStart(2, '0')}:${minutos}:00`;
    }
  }
}

async function pacienteConRelacion(page: Page): Promise<string> {
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
        motivo: `Control para documentos ${aleatorio()}`,
        modalidad: 'PRESENCIAL',
      },
    });
    if (respuesta.status() === 201) {
      const cita = (await respuesta.json()) as { id: string; pacienteId: string };
      const confirmada = await request.put(`/api/v1/citas/${cita.id}/confirmar`, {
        headers: cabecera(tokenMedico),
      });
      expect(confirmada.status()).toBe(200);
      return cita.pacienteId;
    }
  }
  throw new Error('No se pudo reservar una cita libre');
}

interface Detalle {
  resumen: {
    id: string;
    estado: string;
    numero: string | null;
    referencia?: { id: string; estado: string };
  };
  codigoVerificacion: string | null;
}

async function emitir(
  page: Page,
  tokenMedico: string,
  pacienteId: string,
  tipo: string,
  contenido: Record<string, unknown>,
  extra: Record<string, unknown> = {},
): Promise<Detalle> {
  const creado = await page.request.post('/api/v1/documentos-medicos', {
    headers: cabecera(tokenMedico),
    data: { pacienteId, tipo, autorizaDiagnostico: false, contenido, ...extra },
  });
  expect(creado.status(), await creado.text()).toBe(201);
  const borrador = (await creado.json()) as Detalle;
  const emitido = await page.request.post(
    `/api/v1/documentos-medicos/${borrador.resumen.id}/emitir`,
    {
      headers: cabecera(tokenMedico),
    },
  );
  expect(emitido.status(), await emitido.text()).toBe(200);
  return (await emitido.json()) as Detalle;
}

async function contenido(descarga: Download): Promise<Buffer> {
  return readFileSync(await descarga.path());
}

test.describe('documentos médicos y consentimientos', () => {
  test('el médico emite un certificado y un descanso, el paciente los descarga y los verifica, y el médico anula', async ({
    page,
    browser,
  }) => {
    const pacienteId = await pacienteConRelacion(page);
    const motivo = `Reposo por cuadro viral ${aleatorio()}`;

    await iniciarSesion(page, 'medico');
    await ir(page, `/documentos/nuevo?tipo=CERTIFICADO&pacienteId=${pacienteId}`);
    await page.locator('#doc-motivo').fill(motivo);
    await page.locator('#doc-vigenciaDias input, input#doc-vigenciaDias').first().fill('7');
    await page.getByRole('button', { name: 'Guardar borrador' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Certificado médico' })).toBeVisible();
    await page.getByRole('button', { name: 'Emitir documento' }).click();
    await expect(page.getByText(/CM-\d{4}-\d{6}/).first()).toBeVisible();
    await expect(page.getByText(motivo)).toBeVisible();

    const base = 10 + Math.floor(Math.random() * 20000);
    const tokenMedico = await tokenDe(page, 'medico');
    const descanso = await emitir(page, tokenMedico, pacienteId, 'DESCANSO', {
      tipoDescanso: 'ENFERMEDAD',
      desde: fechaLocal(base),
      hasta: fechaLocal(base + 2),
      contingencia: 'Enfermedad común',
      indicacion: 'Reposo absoluto en domicilio',
    });
    expect(descanso.resumen.numero).toMatch(/^DM-\d{4}-\d{6}$/);
    expect(descanso.codigoVerificacion).toMatch(/^[A-Z0-9]{4}(-[A-Z0-9]{4}){3}$/);

    const tokenPaciente = await tokenDe(page, 'paciente');
    const propios = (await (
      await page.request.get('/api/v1/documentos-medicos/mios', {
        headers: cabecera(tokenPaciente),
      })
    ).json()) as { id: string; tipo: string; numero: string; estado: string }[];
    const certificado = propios.find((d) => d.tipo === 'CERTIFICADO' && d.estado === 'EMITIDO')!;
    expect(certificado).toBeTruthy();
    expect(propios.some((d) => d.id === descanso.resumen.id)).toBe(true);

    const pdf = await page.request.get(`/api/v1/documentos-medicos/${descanso.resumen.id}/pdf`, {
      headers: cabecera(tokenPaciente),
    });
    expect(pdf.status()).toBe(200);
    expect((await pdf.body()).subarray(0, 4).toString()).toBe('%PDF');

    const anonimo = await (await browser.newContext()).newPage();
    const verificacion = await anonimo.request.get(
      `/api/v1/verificacion/${descanso.codigoVerificacion}`,
    );
    expect(verificacion.status()).toBe(200);
    const valida = (await verificacion.json()) as Record<string, unknown>;
    expect(valida['resultado']).toBe('VALIDO');
    expect(valida['medico']).toBeTruthy();
    expect(JSON.stringify(valida)).not.toMatch(/Pérez|12345678|paciente/i);

    const publico = await (await browser.newContext()).newPage();
    await publico.goto(`/verificar/${descanso.codigoVerificacion}`);
    await expect(publico.getByRole('heading', { name: 'Documento válido' })).toBeVisible();

    const portalPaciente = await (await browser.newContext()).newPage();
    await iniciarSesion(portalPaciente, 'paciente');
    await ir(portalPaciente, '/mis-documentos');
    await expect(portalPaciente.getByText(certificado.numero).first()).toBeVisible();
    const descarga = portalPaciente.waitForEvent('download');
    await portalPaciente
      .getByRole('button', { name: `Descargar PDF de Certificado médico ${certificado.numero}` })
      .click();
    expect((await contenido(await descarga)).subarray(0, 4).toString()).toBe('%PDF');

    await ir(page, `/documentos/${descanso.resumen.id}`);
    await page.getByRole('button', { name: 'Anular' }).click();
    await confirmarMotivo(
      page,
      'Anular documento',
      'Se emitió con fechas equivocadas',
      'Anular documento',
    );
    await expect(page.getByText('Se emitió con fechas equivocadas').first()).toBeVisible();

    const tras = (await (
      await anonimo.request.get(`/api/v1/verificacion/${descanso.codigoVerificacion}`)
    ).json()) as { resultado: string };
    expect(tras.resultado).toBe('ANULADO');
  });

  test('el paciente firma un consentimiento con el lienzo y luego lo revoca', async ({
    page,
    browser,
  }) => {
    const pacienteId = await pacienteConRelacion(page);
    const tokenMedico = await tokenDe(page, 'medico');
    const plantillas = (await (
      await page.request.get('/api/v1/consentimientos/plantillas', {
        headers: cabecera(tokenMedico),
      })
    ).json()) as { id: string; requiereTestigo: boolean; activa: boolean }[];
    const plantilla = plantillas.find((p) => p.activa && !p.requiereTestigo)!;
    const procedimiento = `Sutura menor ${aleatorio()}`;
    const solicitud = await page.request.post('/api/v1/consentimientos', {
      headers: cabecera(tokenMedico),
      data: { plantillaId: plantilla.id, pacienteId, procedimiento },
    });
    expect(solicitud.status(), await solicitud.text()).toBe(201);
    const { id } = (await solicitud.json()) as { id: string };

    const ajeno = await page.request.post(`/api/v1/consentimientos/${id}/firma`, {
      headers: cabecera(await tokenDe(page, 'paciente2')),
      data: {
        firmaPng: 'x',
        firmanteTipo: 'PACIENTE',
        nombre: 'Intruso',
        dni: DNI_JUAN,
        via: 'APP_PACIENTE',
      },
    });
    expect([403, 404]).toContain(ajeno.status());

    const sesion = await (await browser.newContext()).newPage();
    await iniciarSesion(sesion, 'paciente');
    await ir(sesion, '/mis-consentimientos');
    await expect(sesion.getByText(procedimiento)).toBeVisible();
    await sesion
      .getByRole('button', { name: `Leer y firmar el consentimiento de ${procedimiento}` })
      .click();
    const dialogo = sesion.getByRole('dialog', { name: 'Firmar consentimiento' });
    await expect(dialogo).toBeVisible();
    await dialogo.getByRole('button', { name: 'Firmar consentimiento' }).click();
    await expect(dialogo.getByText('Dibuje la firma')).toBeVisible();

    await dialogo.locator('#firma-dni').fill(DNI_JUAN);
    const lienzo = dialogo.locator('canvas');
    await lienzo.hover();
    const caja = (await lienzo.boundingBox())!;
    await sesion.mouse.move(caja.x + 30, caja.y + 80);
    await sesion.mouse.down();
    for (let paso = 1; paso <= 14; paso++) {
      await sesion.mouse.move(caja.x + 30 + paso * 12, caja.y + 40 + (paso % 2) * 50);
    }
    await sesion.mouse.up();
    await dialogo.getByRole('button', { name: 'Firmar consentimiento' }).click();
    await expect(dialogo).toBeHidden();
    const tarjeta = sesion.locator('article', { hasText: procedimiento });
    await expect(tarjeta.getByText('Firmado', { exact: true })).toBeVisible();

    const tokenPaciente = await tokenDe(page, 'paciente');
    const pdf = await page.request.get(`/api/v1/consentimientos/${id}/pdf`, {
      headers: cabecera(tokenPaciente),
    });
    expect(pdf.status()).toBe(200);
    expect((await pdf.body()).subarray(0, 4).toString()).toBe('%PDF');

    await sesion
      .getByRole('button', { name: `Revocar el consentimiento de ${procedimiento}` })
      .click();
    await confirmarMotivo(sesion, 'Revocar consentimiento', 'Cambié de opinión', 'Revocar');
    await expect(tarjeta.getByText('Revocado', { exact: true })).toBeVisible();
  });

  test('la referencia pasa por recibida y la contrarreferencia la deja atendida', async ({
    page,
  }) => {
    const pacienteId = await pacienteConRelacion(page);
    const tokenMedico = await tokenDe(page, 'medico');
    const establecimientos = (await (
      await page.request.get('/api/v1/establecimientos-salud', { headers: cabecera(tokenMedico) })
    ).json()) as { id: string; activo: boolean }[];
    const destino = establecimientos.find((e) => e.activo)!;

    const referencia = await emitir(page, tokenMedico, pacienteId, 'REFERENCIA', {
      destinoId: destino.id,
      especialidad: 'Cardiología',
      prioridad: 'PREFERENTE',
      motivo: 'Evaluación especializada',
      resumenClinico: `Cuadro estable con control pendiente ${aleatorio()}`,
    });
    expect(referencia.resumen.referencia?.estado).toBe('ENVIADA');

    const recibida = await page.request.put(
      `/api/v1/documentos-medicos/${referencia.resumen.id}/referencia/estado`,
      { headers: cabecera(tokenMedico), data: { estado: 'RECIBIDA' } },
    );
    expect(recibida.status(), await recibida.text()).toBe(200);

    await emitir(
      page,
      tokenMedico,
      pacienteId,
      'CONTRARREFERENCIA',
      { resumen: 'Se atendió en el establecimiento y se indicó seguimiento' },
      { referenciaId: referencia.resumen.referencia!.id },
    );

    const actual = (await (
      await page.request.get(`/api/v1/documentos-medicos/${referencia.resumen.id}`, {
        headers: cabecera(tokenMedico),
      })
    ).json()) as Detalle;
    expect(actual.resumen.referencia?.estado).toBe('ATENDIDA');
  });

  test('un tercero, otro médico y el personal sin rol clínico no acceden al contenido', async ({
    page,
    browser,
  }) => {
    const pacienteId = await pacienteConRelacion(page);
    const tokenMedico = await tokenDe(page, 'medico');
    const documento = await emitir(page, tokenMedico, pacienteId, 'CERTIFICADO', {
      motivo: `Atención ambulatoria ${aleatorio()}`,
      vigenciaDias: 5,
    });
    const ruta = `/api/v1/documentos-medicos/${documento.resumen.id}`;

    for (const rol of ['paciente2', 'medico2', 'recepcion', 'caja', 'admin', 'rrhh'] as const) {
      if (!(rol in CUENTAS)) {
        continue;
      }
      const token = await tokenDe(page, rol);
      for (const sufijo of ['', '/pdf']) {
        const respuesta = await page.request.get(ruta + sufijo, { headers: cabecera(token) });
        expect([403, 404], `${rol} no debe acceder a ${ruta}${sufijo}`).toContain(
          respuesta.status(),
        );
      }
    }

    const intruso = await page.request.post(`${ruta}/anular`, {
      headers: cabecera(await tokenDe(page, 'medico2')),
      data: { motivo: 'Intento de anulación ajena' },
    });
    expect([403, 404]).toContain(intruso.status());

    const anonimo = await (await browser.newContext()).newPage();
    const inexistente = (await (
      await anonimo.request.get('/api/v1/verificacion/ZZZZ-ZZZZ-ZZZZ-ZZZZ')
    ).json()) as Record<string, unknown>;
    const real = (await (
      await anonimo.request.get(`/api/v1/verificacion/${documento.codigoVerificacion}`)
    ).json()) as Record<string, unknown>;
    expect(Object.keys(inexistente).sort()).toEqual(Object.keys(real).sort());
    expect(inexistente['resultado']).toBe('NO_ENCONTRADO');
  });
});
