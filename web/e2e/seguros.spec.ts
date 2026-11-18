import { APIRequestContext, Page, expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, ir, omitirGuia, tokenDe } from './support';

interface Contexto {
  recepcion: Record<string, string>;
  finanzas: Record<string, string>;
  medico: Record<string, string>;
  caja: Record<string, string>;
}

function aleatorio(digitos: number): string {
  return String(Math.floor(Math.random() * 10 ** digitos)).padStart(digitos, '0');
}

function fechaIso(dias: number): string {
  const f = new Date(Date.now() + dias * 86_400_000);
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, '0')}-${String(f.getDate()).padStart(2, '0')}`;
}

async function cabeceras(page: Page): Promise<Contexto> {
  const [recepcion, finanzas, medico, caja] = await Promise.all(
    (['recepcion', 'finanzas', 'medico', 'caja'] as const).map(async (rol) => ({
      Authorization: `Bearer ${await tokenDe(page, rol)}`,
    })),
  );
  return { recepcion, finanzas, medico, caja };
}

async function json<T>(respuesta: Awaited<ReturnType<APIRequestContext['get']>>): Promise<T> {
  expect(respuesta.status(), await respuesta.text()).toBeLessThan(300);
  return (await respuesta.json()) as T;
}

async function nuevoPaciente(page: Page, c: Contexto) {
  const dni = `7${aleatorio(7)}`;
  const nombre = `Elena${aleatorio(4)}`;
  const resultado = await json<{
    paciente: { pacienteId: string; nombreCompleto: string };
    passwordTemporal: string;
  }>(
    await page.request.post('/api/v1/recepcion/pacientes', {
      headers: c.recepcion,
      data: { nombre, apellido: 'Asegurada', dni, fechaNacimiento: '1988-04-12' },
    }),
  );
  return {
    id: resultado.paciente.pacienteId,
    nombreCompleto: resultado.paciente.nombreCompleto,
    dni,
    email: `${dni}@pacientes.tusalud.local`,
    password: resultado.passwordTemporal,
  };
}

async function nuevoConvenio(page: Page, c: Contexto, exigePreautorizacion: boolean) {
  const sufijo = aleatorio(5);
  const aseguradora = await json<{ id: string }>(
    await page.request.post('/api/v1/seguros/aseguradoras', {
      headers: c.finanzas,
      data: {
        codigo: `E${sufijo}`,
        nombre: `Aseguradora E2E ${sufijo}`,
        tipo: 'PRIVADO',
        ruc: '20111222333',
      },
    }),
  );
  const convenio = await json<{ id: string }>(
    await page.request.post('/api/v1/seguros/convenios', {
      headers: c.finanzas,
      data: {
        aseguradoraId: aseguradora.id,
        nombre: `Plan E2E ${sufijo}`,
        vigenciaDesde: fechaIso(-10),
        vigenciaHasta: fechaIso(300),
        topeAnual: 20000,
        activo: true,
        reglas: [
          { categoria: 'CONSULTA', tipo: 'PORCENTAJE', valor: 80, requierePreautorizacion: false },
          {
            categoria: 'IMAGEN',
            tipo: 'PORCENTAJE',
            valor: 70,
            requierePreautorizacion: exigePreautorizacion,
          },
        ],
        exclusiones: [],
      },
    }),
  );
  return {
    aseguradoraId: aseguradora.id,
    convenioId: convenio.id,
    aseguradora: `Aseguradora E2E ${sufijo}`,
    plan: `Plan E2E ${sufijo}`,
  };
}

async function nuevaPoliza(
  page: Page,
  c: Contexto,
  pacienteId: string,
  convenioId: string,
): Promise<string> {
  const numero = `E2E-${aleatorio(6)}`;
  await json(
    await page.request.post('/api/v1/seguros/polizas', {
      headers: c.recepcion,
      data: {
        pacienteId,
        convenioId,
        numero,
        titularNombre: 'Titular E2E',
        parentesco: 'TITULAR',
        vigenciaDesde: fechaIso(-5),
        vigenciaHasta: fechaIso(200),
      },
    }),
  );
  return numero;
}

async function citaConfirmada(
  page: Page,
  c: Contexto,
  pacienteId: string,
  confirmar: boolean,
): Promise<string> {
  const medicos = await json<
    { id: string; nombreCompleto: string; especialidades: { id: string }[] }[]
  >(await page.request.get('/api/v1/medicos', { headers: c.recepcion }));
  const garcia = medicos.find((m) => /García/.test(m.nombreCompleto))!;
  for (let dia = 2; dia < 40; dia++) {
    const fecha = fechaIso(dia);
    const disponibilidad = await page.request.get(`/api/v1/medicos/${garcia.id}/disponibilidad`, {
      headers: c.recepcion,
      params: { fecha },
    });
    if (!disponibilidad.ok()) {
      continue;
    }
    const { horariosDisponibles } = (await disponibilidad.json()) as {
      horariosDisponibles: string[];
    };
    if (horariosDisponibles.length === 0) {
      continue;
    }
    const hora = horariosDisponibles[0];
    const fechaHora = hora.includes('T')
      ? hora
      : `${fecha}T${hora.length === 5 ? `${hora}:00` : hora}`;
    const creada = await page.request.post('/api/v1/recepcion/citas', {
      headers: c.recepcion,
      data: {
        pacienteId,
        medicoId: garcia.id,
        especialidadId: garcia.especialidades[0].id,
        fechaHora,
        motivo: 'Consulta con seguro e2e',
      },
    });
    if (!creada.ok()) {
      continue;
    }
    const cita = (await creada.json()) as { id: string };
    if (confirmar) {
      await json(
        await page.request.put(`/api/v1/citas/${cita.id}/confirmar`, { headers: c.medico }),
      );
    }
    return cita.id;
  }
  throw new Error('No hubo un horario libre para reservar la cita de prueba');
}

test.describe('seguros y convenios', () => {
  test('recepción registra una póliza y verifica su cobertura', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    const c = await cabeceras(page);
    const convenio = await nuevoConvenio(page, c, true);
    const paciente = await nuevoPaciente(page, c);

    await ir(page, '/seguros/polizas');
    await page.locator('#pol-paciente').fill(paciente.dni);
    await page.getByRole('option').first().click();
    await page.getByRole('button', { name: 'Registrar póliza' }).click();
    await page.locator('#pol-convenio').click();
    await page.getByRole('option', { name: new RegExp(convenio.plan) }).click();
    const numero = `UI-${aleatorio(6)}`;
    await page.locator('#pol-numero').fill(numero);
    await page
      .getByRole('dialog', { name: 'Registrar póliza' })
      .getByRole('button', { name: /Registrar$/ })
      .click();
    await expect(page.getByText(/fue registrada/)).toBeVisible();

    const fila = page.getByRole('row', { name: new RegExp(numero) });
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: /Verificar la póliza/ }).click();
    await expect(page.locator('#pol-codigo-verificacion')).toHaveText(/^VC-[0-9A-F]{10}$/);
    await expect(page.locator('#pol-verificacion')).toContainText('Cobertura vigente');
  });

  test('caja cobra solo el copago y finanzas liquida la cartera de la aseguradora', async ({
    page,
  }) => {
    await iniciarSesion(page, 'recepcion');
    const c = await cabeceras(page);
    const convenio = await nuevoConvenio(page, c, true);
    const paciente = await nuevoPaciente(page, c);
    await nuevaPoliza(page, c, paciente.id, convenio.convenioId);
    await citaConfirmada(page, c, paciente.id, true);
    await page.request.post('/api/v1/caja/aperturas', {
      headers: c.caja,
      data: { montoApertura: 50 },
    });

    await cerrarSesion(page);
    await iniciarSesion(page, 'caja');
    await ir(page, '/cobrar');
    await page.getByRole('searchbox', { name: 'Buscar cuenta por cobrar' }).fill(paciente.dni);
    await page
      .getByRole('button', { name: /^Cobrar Consulta/ })
      .first()
      .click();
    await expect(page.locator('#cob-sin-cobertura')).toHaveCount(0);
    const copago = (await page.locator('#cob-copago').innerText()).trim();
    const cubierto = (await page.locator('#cob-cubierto').innerText()).trim();
    expect(cubierto).not.toEqual(copago);
    await expect(page.locator('#cob-total')).toHaveText(copago);
    await page.getByRole('button', { name: 'Emitir comprobante' }).click();
    const comprobante = page.getByRole('dialog', { name: 'Comprobante emitido' });
    await expect(comprobante).toBeVisible();
    await expect(comprobante).toContainText(copago);
    await comprobante.getByRole('button', { name: 'Listo' }).click();

    await cerrarSesion(page);
    await iniciarSesion(page, 'finanzas');
    await ir(page, '/seguros/cartera');
    await page.getByRole('button', { name: `Liquidar ${convenio.aseguradora}` }).click();
    await page.getByRole('button', { name: 'Armar liquidación' }).click();
    await expect(page.getByText(/quedó en borrador/)).toBeVisible();

    await ir(page, '/seguros/liquidaciones');
    const fila = page.getByRole('row', { name: new RegExp(convenio.aseguradora) });
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: /^Enviar LQ-/ }).click();
    await expect(page.getByText(/fue enviada/)).toBeVisible();
    await fila.getByRole('button', { name: /^Registrar el pago de LQ-/ }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /Registrar$/ })
      .click();
    await expect(page.getByText(/quedó pagada/)).toBeVisible();

    await page.request.post('/api/v1/caja/cierre', {
      headers: { Authorization: `Bearer ${await tokenDe(page, 'caja')}` },
      data: { montoContado: 0 },
    });
  });

  test('el cajero puede cobrar como particular indicando el motivo', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    const c = await cabeceras(page);
    const convenio = await nuevoConvenio(page, c, false);
    const paciente = await nuevoPaciente(page, c);
    await nuevaPoliza(page, c, paciente.id, convenio.convenioId);
    await citaConfirmada(page, c, paciente.id, true);
    await page.request.post('/api/v1/caja/aperturas', {
      headers: c.caja,
      data: { montoApertura: 50 },
    });

    await cerrarSesion(page);
    await iniciarSesion(page, 'caja');
    await ir(page, '/cobrar');
    await page.getByRole('searchbox', { name: 'Buscar cuenta por cobrar' }).fill(paciente.dni);
    await page
      .getByRole('button', { name: /^Cobrar Consulta/ })
      .first()
      .click();
    const copago = (await page.locator('#cob-copago').innerText()).trim();
    await page.locator('#cob-particular').check();
    await expect(page.locator('#cob-total')).not.toHaveText(copago);
    await page.getByRole('button', { name: 'Emitir comprobante' }).click();
    await expect(page.getByText(/Indica el motivo para cobrar como particular/)).toBeVisible();
    await page.locator('#cob-motivo-particular').fill('El paciente pidió pagar sin seguro');
    await page.getByRole('button', { name: 'Emitir comprobante' }).click();
    await expect(page.getByRole('dialog', { name: 'Comprobante emitido' })).toBeVisible();
    await page.request.post('/api/v1/caja/cierre', {
      headers: { Authorization: `Bearer ${await tokenDe(page, 'caja')}` },
      data: { montoContado: 0 },
    });
  });

  test('el médico solicita una preautorización y finanzas la aprueba', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    const c = await cabeceras(page);
    const convenio = await nuevoConvenio(page, c, true);
    const paciente = await nuevoPaciente(page, c);
    await nuevaPoliza(page, c, paciente.id, convenio.convenioId);
    await citaConfirmada(page, c, paciente.id, false);

    await cerrarSesion(page);
    await iniciarSesion(page, 'medico');
    await ir(page, '/preautorizaciones');
    await page.getByRole('button', { name: 'Nueva solicitud' }).click();
    await page.locator('#pre-paciente').fill(paciente.dni);
    await page.getByRole('option').first().click();
    await expect(page.locator('#pre-cobertura')).toContainText(convenio.aseguradora);
    await expect(page.locator('#pre-cobertura')).toContainText('imágenes diagnósticas');
    await page.locator('#pre-descripcion').fill('Resonancia magnética de columna');
    await page.locator('#pre-estimado').fill('650');
    await page.keyboard.press('Tab');
    await page.getByRole('button', { name: /Solicitar$/ }).click();
    await expect(page.getByText(/fue enviada/)).toBeVisible();
    await expect(
      page.getByRole('row', { name: new RegExp(paciente.nombreCompleto) }),
    ).toBeVisible();

    await cerrarSesion(page);
    await iniciarSesion(page, 'finanzas');
    await ir(page, '/seguros/preautorizaciones');
    const fila = page.getByRole('row', { name: new RegExp(paciente.nombreCompleto) });
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: /^Aprobar PA-/ }).click();
    await page
      .getByRole('dialog')
      .getByRole('button', { name: /Aprobar$/ })
      .click();
    await expect(page.getByText(/fue aprobada con el código AUT-/)).toBeVisible();
  });

  test('el paciente ve solo sus pólizas en solo lectura', async ({ page, browser }) => {
    await iniciarSesion(page, 'recepcion');
    const c = await cabeceras(page);
    const convenio = await nuevoConvenio(page, c, true);
    const paciente = await nuevoPaciente(page, c);
    const numero = await nuevaPoliza(page, c, paciente.id, convenio.convenioId);

    const propio = await (await browser.newContext()).newPage();
    await omitirGuia(propio);
    await propio.goto('/auth/login');
    await propio.getByLabel('Email').fill(paciente.email);
    await propio.locator('#login-password').fill(paciente.password);
    await propio.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(propio).toHaveURL(/\/paciente\/inicio$/);
    await ir(propio, '/mis-seguros');
    await expect(propio.getByText(convenio.aseguradora)).toBeVisible();
    await expect(propio.getByText(numero)).toBeVisible();
    await expect(propio.getByRole('button', { name: /Registrar|Suspender|Verificar/ })).toHaveCount(
      0,
    );

    const ajeno = await (await browser.newContext()).newPage();
    await iniciarSesion(ajeno, 'paciente');
    await ir(ajeno, '/mis-seguros');
    await expect(ajeno.getByRole('heading', { name: 'Mis seguros', level: 1 })).toBeVisible();
    await expect(ajeno.getByText(convenio.aseguradora)).toHaveCount(0);
    await expect(ajeno.getByText(numero)).toHaveCount(0);
  });

  test('cada rol ve en el menú solo lo que le corresponde de seguros', async ({ browser }) => {
    const finanzas = await (await browser.newContext()).newPage();
    await iniciarSesion(finanzas, 'finanzas');
    await expect(finanzas.getByRole('link', { name: 'Aseguradoras y convenios' })).toBeVisible();
    await expect(finanzas.getByRole('link', { name: 'Cartera', exact: true })).toBeVisible();
    await expect(finanzas.getByRole('link', { name: 'Pólizas' })).toHaveCount(0);

    const recepcion = await (await browser.newContext()).newPage();
    await iniciarSesion(recepcion, 'recepcion');
    await expect(recepcion.getByRole('link', { name: 'Pólizas' })).toBeVisible();
    await expect(recepcion.getByRole('link', { name: 'Cartera', exact: true })).toHaveCount(0);

    const caja = await (await browser.newContext()).newPage();
    await iniciarSesion(caja, 'caja');
    await expect(caja.getByRole('link', { name: 'Aseguradoras y convenios' })).toHaveCount(0);

    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await expect(paciente.getByRole('link', { name: 'Mis seguros' })).toBeVisible();
    await paciente.goto('/finanzas/seguros/cartera');
    await expect(paciente).toHaveURL(/\/paciente\/inicio$/);
  });
});
