import { Page, expect, test } from '@playwright/test';

import { iniciarSesion, ir, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

function unico(): string {
  return `${Date.now().toString().slice(-6)}${Math.floor(Math.random() * 90 + 10)}`;
}

function fechaLejana(): string {
  const anio = 2060 + Math.floor(Math.random() * 30);
  const mes = String(1 + Math.floor(Math.random() * 12)).padStart(2, '0');
  const dia = String(1 + Math.floor(Math.random() * 28)).padStart(2, '0');
  return `${anio}-${mes}-${dia}T09:00:00`;
}

interface Persona {
  usuarioId: string;
  nombre: string;
}

async function personal(page: Page, token: string, rol: string, nombre: string): Promise<string> {
  const respuesta = await page.request.get(`/api/v1/quirofano/personal?rol=${rol}`, {
    headers: cabecera(token),
  });
  const lista = (await respuesta.json()) as Persona[];
  const persona = lista.find((p) => p.nombre.includes(nombre));
  if (!persona) {
    throw new Error(`No se encontró a ${nombre}`);
  }
  return persona.usuarioId;
}

async function cirugiaProgramada(page: Page): Promise<{ id: string; codigo: string }> {
  const medico = await tokenDe(page, 'medico');
  const enfermeria = await tokenDe(page, 'enfermeria');
  const admin = await tokenDe(page, 'admin');

  const codigo = `E${unico()}`;
  const quirofano = await page.request.post('/api/v1/quirofano/quirofanos', {
    headers: cabecera(admin),
    data: {
      codigo,
      nombre: `Quirófano e2e ${codigo}`,
      estado: 'DISPONIBLE',
      activo: true,
      equipos: [],
    },
  });
  expect(quirofano.status()).toBe(201);
  const quirofanoId = ((await quirofano.json()) as { id: string }).id;

  const pacientes = await page.request.get('/api/v1/vacunacion/pacientes?q=12345678', {
    headers: cabecera(medico),
  });
  const pacienteId = ((await pacientes.json()) as { id: string }[])[0].id;
  const solicitud = await page.request.post('/api/v1/quirofano/cirugias', {
    headers: cabecera(medico),
    data: {
      pacienteId,
      diagnosticoCodigo: 'K35.8',
      procedimiento: `Apendicectomía e2e ${codigo}`,
      tipo: 'ELECTIVA',
      tipoAnestesia: 'GENERAL',
    },
  });
  expect(solicitud.status()).toBe(201);
  const creada = (await solicitud.json()) as { resumen: { id: string; codigo: string } };

  const cirujano = await personal(page, medico, 'CIRUJANO', 'García');
  const anestesiologo = await personal(page, medico, 'ANESTESIOLOGO', 'López');
  const instrumentista = await personal(page, enfermeria, 'INSTRUMENTISTA', 'Campos');
  const programada = await page.request.post(
    `/api/v1/quirofano/cirugias/${creada.resumen.id}/programar`,
    {
      headers: cabecera(enfermeria),
      data: {
        quirofanoId,
        inicio: fechaLejana(),
        duracionMin: 90,
        equipo: [
          { usuarioId: cirujano, rol: 'CIRUJANO' },
          { usuarioId: anestesiologo, rol: 'ANESTESIOLOGO' },
          { usuarioId: instrumentista, rol: 'INSTRUMENTISTA' },
        ],
      },
    },
  );
  expect(programada.status()).toBe(200);
  return creada.resumen;
}

test.describe('quirófano', () => {
  test('administración crea un quirófano con su equipamiento desde el catálogo', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/quirofanos');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Quirófanos y equipos' }),
    ).toBeVisible();

    const codigo = `E${unico()}`;
    await page.getByLabel('Código').fill(codigo);
    await page.getByLabel('Nombre').fill(`Sala e2e ${codigo}`);
    await page.getByRole('button', { name: /Agregar equipo$/ }).click();
    await page.getByLabel('Nombre del equipo 1').fill('Electrobisturí');
    await page.getByRole('button', { name: /Crear quirófano$/ }).click();

    await expect(page.getByText('El quirófano se creó')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(codigo) })).toBeVisible();
  });

  test('el médico solicita una cirugía con diagnóstico CIE-10', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/quirofano/nueva');
    await expect(page.getByRole('heading', { level: 1, name: 'Solicitar cirugía' })).toBeVisible();

    await page.getByLabel('Buscar por nombre o DNI').fill('12345678');
    await page.getByRole('button', { name: /Buscar$/ }).click();
    await page.getByRole('option', { name: /Juan/ }).click();

    await page.getByRole('combobox', { name: 'Diagnóstico CIE-10' }).fill('K35');
    await page.getByRole('option', { name: /K35/ }).first().click();
    const procedimiento = `Apendicectomía ${unico()}`;
    await page.getByLabel('Procedimiento').fill(procedimiento);
    await page.getByRole('button', { name: /Solicitar cirugía$/ }).click();

    await expect(page.getByText('La solicitud quirúrgica quedó registrada')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1, name: /^CIR-\d{6}$/ })).toBeVisible();
    await expect(page.getByText('Solicitada').first()).toBeVisible();
  });

  test('enfermería pasa la cirugía programada a preparación y ve lo que falta para iniciar', async ({
    page,
  }) => {
    const cirugia = await cirugiaProgramada(page);
    await iniciarSesion(page, 'enfermeria');
    await ir(page, `/quirofano/${cirugia.id}`);

    await expect(page.getByRole('heading', { level: 1, name: cirugia.codigo })).toBeVisible();
    await expect(page.getByText('Programada').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Equipo asignado' })).toBeVisible();

    await page.getByRole('button', { name: /Pasar a preparación$/ }).click();
    await expect(page.getByText('La cirugía está en preparación')).toBeVisible();
    await expect(page.getByText(/Para iniciar falta/)).toContainText('consentimiento quirúrgico');
    await expect(page.getByText(/Para iniciar falta/)).toContainText('antes de la inducción');
  });

  test('la agenda quirúrgica muestra la cirugía y el médico del equipo ve su detalle clínico', async ({
    page,
  }) => {
    const cirugia = await cirugiaProgramada(page);
    await iniciarSesion(page, 'medico');
    await ir(page, `/quirofano/${cirugia.id}`);

    await expect(page.getByRole('heading', { level: 1, name: cirugia.codigo })).toBeVisible();
    await expect(page.getByText(/K35\.8/)).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Lista de verificación de cirugía segura' }),
    ).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Nota operatoria' })).toBeVisible();

    await ir(page, '/quirofano');
    await expect(page.getByRole('heading', { level: 1, name: 'Agenda quirúrgica' })).toBeVisible();
    await expect(page.getByRole('button', { name: /Solicitar cirugía$/ })).toBeVisible();
  });

  test('el paciente ve su cirugía programada en Mis cirugías', async ({ page }) => {
    const cirugia = await cirugiaProgramada(page);
    await iniciarSesion(page, 'paciente');
    await ir(page, '/mis-cirugias');

    await expect(page.getByRole('heading', { level: 1, name: 'Mis cirugías' })).toBeVisible();
    await expect(
      page.getByRole('heading', { name: new RegExp(`Apendicectomía e2e`) }).first(),
    ).toBeVisible();
    await expect(page.getByText(cirugia.codigo)).toBeVisible();
  });

  test('un médico sin relación con el paciente no abre el detalle de la cirugía', async ({
    page,
  }) => {
    const cirugia = await cirugiaProgramada(page);
    const ajeno = await page.request.post('/api/v1/auth/login', {
      data: { email: 'dr.delgado@tusalud.com', password: 'Medico123!' },
    });
    const { token } = (await ajeno.json()) as { token: string };
    const detalle = await page.request.get(`/api/v1/quirofano/cirugias/${cirugia.id}`, {
      headers: cabecera(token),
    });
    expect(detalle.status()).toBe(403);
  });
});

test.describe('esterilización', () => {
  async function cicloEnProceso(page: Page): Promise<{ numero: string }> {
    const admin = await tokenDe(page, 'admin');
    const enfermeria = await tokenDe(page, 'enfermeria');
    const codigo = unico();
    const esterilizador = await page.request.post('/api/v1/esterilizacion/esterilizadores', {
      headers: cabecera(admin),
      data: {
        codigo: `A${codigo}`,
        nombre: `Autoclave e2e ${codigo}`,
        tipo: 'VAPOR',
        activo: true,
      },
    });
    expect(esterilizador.status()).toBe(201);
    const set = await page.request.post('/api/v1/esterilizacion/sets', {
      headers: cabecera(admin),
      data: { codigo: `S${codigo}`, nombre: `Set e2e ${codigo}`, vigenciaDias: 30, activo: true },
    });
    expect(set.status()).toBe(201);
    const ciclo = await page.request.post('/api/v1/esterilizacion/ciclos', {
      headers: cabecera(enfermeria),
      data: {
        esterilizadorId: ((await esterilizador.json()) as { id: string }).id,
        iniciadoEn: new Date(Date.now() - 7 * 3600 * 1000).toISOString().substring(0, 19),
        temperaturaC: 134,
        duracionMin: 30,
        sets: [{ setId: ((await set.json()) as { id: string }).id, cantidad: 1 }],
      },
    });
    expect(ciclo.status()).toBe(201);
    return (await ciclo.json()) as { numero: string };
  }

  test('enfermería cierra un ciclo con indicadores conformes y los lotes quedan estériles', async ({
    page,
  }) => {
    const ciclo = await cicloEnProceso(page);
    await iniciarSesion(page, 'enfermeria');
    await ir(page, '/esterilizacion');
    await expect(page.getByRole('heading', { level: 1, name: 'Esterilización' })).toBeVisible();

    const fila = page.getByRole('row', { name: new RegExp(ciclo.numero) });
    await expect(fila.getByText('En proceso')).toBeVisible();
    await fila
      .getByRole('button', { name: `Registrar indicadores del ciclo ${ciclo.numero}` })
      .click();
    await page.getByRole('button', { name: /Guardar indicadores$/ }).click();

    await expect(page.getByText(/quedó conforme/)).toBeVisible();
    await expect(
      page
        .getByRole('row', { name: new RegExp(ciclo.numero) })
        .getByText('Conforme')
        .first(),
    ).toBeVisible();

    await page.getByText('Lotes', { exact: true }).click();
    await expect(
      page.getByRole('row', { name: new RegExp(ciclo.numero) }).getByText('Estéril'),
    ).toBeVisible();
  });

  test('calidad consulta la esterilización sin poder registrar ciclos', async ({ page }) => {
    await cicloEnProceso(page);
    await iniciarSesion(page, 'calidad');
    await ir(page, '/esterilizacion');

    await expect(page.getByRole('heading', { level: 1, name: 'Esterilización' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Registrar un ciclo' })).toHaveCount(0);
    await expect(
      page.getByRole('table', { name: 'Ciclos de esterilización recientes' }),
    ).toBeVisible();
    await page.getByText('Alertas', { exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Alertas de esterilización' })).toBeVisible();
  });
});
