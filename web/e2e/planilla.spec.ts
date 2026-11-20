import { APIRequestContext, Page, expect, test } from '@playwright/test';

import { iniciarSesion, ir, tokenDe } from './support';

const MESES = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Septiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
];

function cabeceras(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function isoHoy(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

function sufijo(): string {
  return `${Date.now().toString().slice(-7)}${Math.floor(Math.random() * 90 + 10)}`;
}

async function periodoLibre(
  request: APIRequestContext,
  token: string,
): Promise<{ anio: number; mes: number }> {
  const respuesta = await request.get('/api/v1/planilla/planillas', {
    headers: cabeceras(token),
  });
  const usados = new Set(
    ((await respuesta.json()) as { anio: number; mes: number }[]).map((p) => p.anio * 100 + p.mes),
  );
  const hoy = new Date();
  let anio = hoy.getFullYear();
  let mes = hoy.getMonth();
  if (mes === 0) {
    mes = 12;
    anio--;
  }
  while (usados.has(anio * 100 + mes)) {
    mes--;
    if (mes === 0) {
      mes = 12;
      anio--;
    }
  }
  return { anio, mes };
}

async function segundoAprobador(request: APIRequestContext, admin: string): Promise<string> {
  const marca = sufijo();
  const email = `e2e.pl.${marca}@prueba.com`;
  const registro = await request.post('/api/v1/auth/register', {
    data: {
      nombre: 'Segundo',
      apellido: 'Aprobador',
      email,
      password: 'Paciente123!',
      telefono: '987654321',
      dni: `7${marca.slice(-7)}`,
      fechaNacimiento: '1988-04-05',
    },
  });
  expect(registro.status(), await registro.text()).toBe(201);
  const lista = await request.get(`/api/v1/admin/usuarios?q=${encodeURIComponent(email)}`, {
    headers: cabeceras(admin),
  });
  const usuario = ((await lista.json()) as { id: string; email: string }[]).find(
    (u) => u.email === email,
  );
  expect(usuario).toBeTruthy();
  const roles = await request.put(`/api/v1/admin/usuarios/${usuario?.id}/roles`, {
    headers: cabeceras(admin),
    data: { roles: ['ROLE_FINANZAS'] },
  });
  expect(roles.status(), await roles.text()).toBe(200);
  const login = await request.post('/api/v1/auth/login', {
    data: { email, password: 'Paciente123!' },
  });
  expect(login.status()).toBe(200);
  return ((await login.json()) as { token: string }).token;
}

async function elegirOpcion(
  page: Page,
  combo: ReturnType<Page['locator']>,
  texto: string | RegExp,
) {
  await combo.click();
  await page.getByRole('option', { name: texto }).first().click();
}

test.describe('planilla y asistencia', () => {
  test('recursos humanos recorre sus pantallas de planilla', async ({ page }) => {
    await iniciarSesion(page, 'rrhh');
    const menu = page.getByRole('navigation').first();
    for (const nombre of [
      'Planilla mensual',
      'Contratos laborales',
      'Asistencia',
      'Terminales de asistencia',
      'Conceptos de planilla',
      'Parámetros laborales',
      'Mis boletas',
      'Mi asistencia',
    ]) {
      await expect(menu.getByRole('link', { name: nombre, exact: true })).toBeVisible();
    }

    await ir(page, '/contratos-laborales');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Contratos laborales' }),
    ).toBeVisible();
    await expect(page.getByRole('row', { name: /Régimen general/ }).first()).toBeVisible();

    await ir(page, '/conceptos-planilla');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Conceptos de planilla' }),
    ).toBeVisible();
    await expect(page.getByRole('row', { name: /BASICO/ })).toBeVisible();

    await ir(page, '/parametros-planilla');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Parámetros del régimen laboral' }),
    ).toBeVisible();
    await expect(page.getByRole('row', { name: /^AFP_APORTE_TASA / }).first()).toBeVisible();
    await expect(page.getByText('Por validar').first()).toBeVisible();

    await ir(page, '/asistencia');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Asistencia del personal' }),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: 'Consolidar' })).toBeVisible();

    await ir(page, '/terminales-asistencia');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Terminales de asistencia' }),
    ).toBeVisible();
  });

  test('una terminal nueva muestra su token una sola vez', async ({ page }) => {
    const marca = sufijo();
    await iniciarSesion(page, 'rrhh');
    await ir(page, '/terminales-asistencia');
    await page.getByRole('button', { name: 'Nueva terminal' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Nueva terminal' });
    await dialogo.getByLabel('Nombre de la terminal').fill(`Terminal E2E ${marca}`);
    await dialogo.getByRole('button', { name: 'Crear' }).click();
    await expect(dialogo.getByText('Solo se muestra una vez')).toBeVisible();
    await expect(dialogo.locator('pre')).toContainText('tm_');
    await dialogo.getByRole('button', { name: 'Cerrar' }).click();
    await expect(
      page.getByRole('row', { name: new RegExp(`Terminal E2E ${marca}`) }),
    ).toBeVisible();
  });

  test('recursos humanos calcula, finanzas aprueba y otro aprobador paga y el empleado ve su boleta', async ({
    page,
    browser,
  }) => {
    test.setTimeout(180_000);
    await iniciarSesion(page, 'rrhh');
    const tokenRrhh = await tokenDe(page, 'rrhh');
    const { anio, mes } = await periodoLibre(page.request, tokenRrhh);

    await ir(page, '/planilla');
    await expect(page.getByRole('heading', { level: 1, name: 'Planilla mensual' })).toBeVisible();
    await page.getByRole('button', { name: 'Calcular planilla' }).click();
    const calculo = page.getByRole('dialog', { name: 'Calcular planilla' });
    await elegirOpcion(page, calculo.getByRole('combobox'), MESES[mes - 1]);
    await calculo.getByRole('spinbutton').fill(String(anio));
    await calculo.getByRole('spinbutton').press('Tab');
    await calculo.getByRole('button', { name: 'Calcular' }).click();
    await expect(
      page.getByRole('heading', { level: 1, name: `Planilla ${MESES[mes - 1]} ${anio}` }),
    ).toBeVisible();
    await expect(page.getByText('Calculada').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Aprobar' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Recalcular' })).toBeVisible();
    const codigo = `PLA-${anio}-${String(mes).padStart(2, '0')}`;

    const contextoFinanzas = await browser.newContext();
    const paginaFinanzas = await contextoFinanzas.newPage();
    await iniciarSesion(paginaFinanzas, 'finanzas');
    const tokenFinanzas = await tokenDe(paginaFinanzas, 'finanzas');
    await ir(paginaFinanzas, '/planilla');
    await paginaFinanzas
      .getByRole('row', { name: new RegExp(`${MESES[mes - 1]} ${anio}`) })
      .getByRole('button', { name: /Abrir/ })
      .click();
    await expect(paginaFinanzas.getByRole('button', { name: 'Aprobar' })).toBeVisible();
    await expect(paginaFinanzas.getByRole('button', { name: 'Recalcular' })).toHaveCount(0);
    await paginaFinanzas.getByRole('button', { name: 'Aprobar' }).click();
    await paginaFinanzas.getByRole('button', { name: 'Aprobar', exact: true }).last().click();
    await expect(paginaFinanzas.getByText('La planilla fue aprobada.')).toBeVisible();
    await expect(paginaFinanzas.getByRole('button', { name: 'Registrar pago' })).toBeVisible();

    const planillas = await page.request.get('/api/v1/planilla/planillas', {
      headers: cabeceras(tokenFinanzas),
    });
    const planilla = ((await planillas.json()) as { id: string; codigo: string }[]).find(
      (p) => p.codigo === codigo,
    );
    expect(planilla).toBeTruthy();
    const mismoAprobador = await page.request.post(
      `/api/v1/planilla/planillas/${planilla?.id}/pagar`,
      {
        headers: cabeceras(tokenFinanzas),
        data: { fechaPago: isoHoy(), referencia: 'MISMA-PERSONA' },
      },
    );
    expect(mismoAprobador.status()).toBe(409);

    const tokenAdmin = await (async () => {
      const ctx = await browser.newContext();
      const p = await ctx.newPage();
      await iniciarSesion(p, 'admin');
      const t = await tokenDe(p, 'admin');
      await ctx.close();
      return t;
    })();
    const tokenSegundo = await segundoAprobador(page.request, tokenAdmin);
    const pago = await page.request.post(`/api/v1/planilla/planillas/${planilla?.id}/pagar`, {
      headers: cabeceras(tokenSegundo),
      data: { fechaPago: isoHoy(), referencia: `E2E-${sufijo()}` },
    });
    expect(pago.status(), await pago.text()).toBe(200);
    await contextoFinanzas.close();

    const contextoMedico = await browser.newContext();
    const paginaMedico = await contextoMedico.newPage();
    await iniciarSesion(paginaMedico, 'medico');
    await ir(paginaMedico, '/mis-boletas');
    await expect(
      paginaMedico.getByRole('heading', { level: 1, name: 'Mis boletas de pago' }),
    ).toBeVisible();
    const fila = paginaMedico.getByRole('row', { name: new RegExp(`${MESES[mes - 1]} ${anio}`) });
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: /Ver la boleta/ }).click();
    const boleta = paginaMedico.getByRole('dialog', { name: /^Boleta BP-/ });
    await expect(boleta.getByText('Remuneración básica')).toBeVisible();
    await expect(boleta.getByText(/Neto/)).toBeVisible();
    await expect(boleta.getByText('Banco')).toHaveCount(0);
    await contextoMedico.close();
  });

  test('el personal ve su asistencia y su código de marcación', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/mi-asistencia');
    await expect(page.getByRole('heading', { level: 1, name: 'Mi asistencia' })).toBeVisible();
    await expect(page.getByText('Tu código de marcación en las terminales es')).toBeVisible();
    await expect(page.getByText('Días asistidos')).toBeVisible();
  });

  test('gerencia ve los totales de la planilla pero no sus boletas individuales', async ({
    page,
  }) => {
    await iniciarSesion(page, 'gerencia');
    await ir(page, '/planilla');
    await expect(page.getByRole('heading', { level: 1, name: 'Planilla mensual' })).toBeVisible();
    await page
      .getByRole('button', { name: /Abrir la planilla/ })
      .first()
      .click();
    await expect(page.getByText('Gerencia ve los totales de la planilla')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Boletas de pago' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Aprobar' })).toHaveCount(0);
  });
});
