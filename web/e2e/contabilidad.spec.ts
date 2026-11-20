import { APIRequestContext, Page, expect, test } from '@playwright/test';

import { confirmarMotivo, iniciarSesion, ir, tokenDe } from './support';

function sufijo(): string {
  return `${Date.now().toString().slice(-7)}${Math.floor(Math.random() * 90 + 10)}`;
}

function cabeceras(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function isoHoy(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

interface Cuenta {
  id: string;
  codigo: string;
  nombre: string;
}

async function crearCuenta(
  request: APIRequestContext,
  token: string,
  tipo: 'ACTIVO' | 'PATRIMONIO',
  indice: number,
  marca: string,
): Promise<Cuenta> {
  const codigo = String(8_000_000 + (Date.now() % 1_000_000) + indice);
  const nombre = `Cuenta E2E ${marca} ${indice}`;
  const respuesta = await request.post('/api/v1/contabilidad/cuentas', {
    headers: cabeceras(token),
    data: { codigo, nombre, tipo, aceptaMovimientos: true },
  });
  expect(respuesta.status(), await respuesta.text()).toBe(201);
  const creada = (await respuesta.json()) as { id: string };
  return { id: creada.id, codigo, nombre };
}

async function asegurarPeriodoActual(request: APIRequestContext, token: string): Promise<void> {
  const hoy = new Date();
  const respuesta = await request.post('/api/v1/contabilidad/periodos', {
    headers: cabeceras(token),
    data: { anio: hoy.getFullYear(), mes: hoy.getMonth() + 1 },
  });
  expect([201, 409]).toContain(respuesta.status());
}

async function periodoLibre(
  request: APIRequestContext,
  token: string,
): Promise<{ anio: number; mes: number }> {
  const respuesta = await request.get('/api/v1/contabilidad/periodos', {
    headers: cabeceras(token),
  });
  const usados = new Set(
    ((await respuesta.json()) as { anio: number; mes: number }[]).map((p) => p.anio * 100 + p.mes),
  );
  let anio = 2000;
  let mes = 1;
  while (usados.has(anio * 100 + mes)) {
    mes++;
    if (mes > 12) {
      mes = 1;
      anio++;
    }
  }
  return { anio, mes };
}

async function elegirOpcion(
  page: Page,
  combo: ReturnType<Page['locator']>,
  texto: string | RegExp,
) {
  await combo.click();
  await page.getByRole('option', { name: texto }).first().click();
}

test.describe('contabilidad y tesorería', () => {
  test('finanzas ve el resumen contable y recorre el menú del módulo', async ({ page }) => {
    await iniciarSesion(page, 'finanzas');
    const token = await tokenDe(page, 'finanzas');
    await asegurarPeriodoActual(page.request, token);
    await ir(page, '/contabilidad');
    await expect(page.getByRole('heading', { level: 1, name: 'Contabilidad' })).toBeVisible();
    await expect(page.getByText('Periodo actual', { exact: false }).first()).toBeVisible();
    await expect(page.getByText('Saldo de tesorería')).toBeVisible();
    await expect(
      page.getByRole('heading', { name: 'Hechos que no pudieron contabilizarse' }),
    ).toBeVisible();

    const menu = page.getByRole('navigation').first();
    for (const nombre of ['Asientos', 'Libros y estados', 'Periodos', 'Plan contable']) {
      await expect(menu.getByRole('link', { name: nombre })).toBeVisible();
    }
    await ir(page, '/contabilidad/plan-cuentas');
    await expect(page.getByRole('heading', { level: 1, name: 'Plan contable' })).toBeVisible();
    await expect(page.getByRole('row', { name: /^1011 Caja/ })).toBeVisible();
    await expect(page.getByRole('row', { name: /^1041 / })).toBeVisible();
  });

  test('finanzas registra un asiento, lo contabiliza y lo reversa con motivo', async ({ page }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    await iniciarSesion(page, 'finanzas');
    const token = await tokenDe(page, 'finanzas');
    await asegurarPeriodoActual(page.request, token);
    const debe = await crearCuenta(page.request, token, 'ACTIVO', 1, marca);
    const haber = await crearCuenta(page.request, token, 'PATRIMONIO', 2, marca);

    await ir(page, '/contabilidad/asientos');
    await expect(page.getByRole('heading', { level: 1, name: 'Asientos contables' })).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo asiento' }).click();
    const formulario = page.getByRole('dialog', { name: 'Nuevo asiento' });
    await formulario.locator('#as-glosa').fill(`Aporte E2E ${marca}`);
    await elegirOpcion(
      page,
      formulario.getByRole('combobox', { name: 'Cuenta de la línea 1' }),
      debe.nombre,
    );
    await formulario.getByRole('spinbutton', { name: 'Debe de la línea 1' }).fill('350');
    await formulario.getByRole('spinbutton', { name: 'Debe de la línea 1' }).press('Tab');
    await elegirOpcion(
      page,
      formulario.getByRole('combobox', { name: 'Cuenta de la línea 2' }),
      haber.nombre,
    );
    await formulario.getByRole('spinbutton', { name: 'Haber de la línea 2' }).fill('350');
    await formulario.getByRole('spinbutton', { name: 'Haber de la línea 2' }).press('Tab');
    await expect(formulario.getByText('Cuadra')).toBeVisible();
    await formulario.getByRole('button', { name: 'Guardar borrador' }).click();

    const detalle = page.getByRole('dialog', { name: /^Asiento en borrador/ });
    await expect(detalle).toBeVisible();
    await expect(detalle.getByText(`Aporte E2E ${marca}`)).toBeVisible();
    await detalle.getByRole('button', { name: 'Contabilizar' }).click();
    await expect(page.getByRole('dialog', { name: /^Asiento ASI-\d{4}-\d{6}$/ })).toBeVisible();
    await expect(page.getByRole('dialog').getByText('Contabilizado').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Editar' })).toHaveCount(0);

    await page.getByRole('button', { name: 'Reversar' }).click();
    await confirmarMotivo(
      page,
      'Reversar asiento',
      'Aporte registrado por error en la prueba',
      /Reversar asiento/,
    );
    await expect(page.getByText(/Se generó la reversa ASI-/)).toBeVisible();
    await expect(page.getByRole('dialog').getByText('Revierte al asiento')).toBeVisible();
  });

  test('quien registró un asiento no cierra el periodo y gerencia sí', async ({
    page,
    browser,
  }) => {
    test.setTimeout(120_000);
    const marca = sufijo();
    await iniciarSesion(page, 'finanzas');
    const token = await tokenDe(page, 'finanzas');
    const libre = await periodoLibre(page.request, token);
    const etiqueta = `${libre.anio}-${String(libre.mes).padStart(2, '0')}`;
    const abierto = await page.request.post('/api/v1/contabilidad/periodos', {
      headers: cabeceras(token),
      data: libre,
    });
    expect(abierto.status(), await abierto.text()).toBe(201);
    const debe = await crearCuenta(page.request, token, 'ACTIVO', 3, marca);
    const haber = await crearCuenta(page.request, token, 'PATRIMONIO', 4, marca);
    const fecha = `${etiqueta}-15`;
    const borrador = await page.request.post('/api/v1/contabilidad/asientos', {
      headers: cabeceras(token),
      data: {
        fecha,
        glosa: `Periodo E2E ${marca}`,
        lineas: [
          { cuentaId: debe.id, debe: 80 },
          { cuentaId: haber.id, haber: 80 },
        ],
      },
    });
    expect(borrador.status(), await borrador.text()).toBe(201);
    const { id } = (await borrador.json()) as { id: string };
    const contabilizado = await page.request.post(
      `/api/v1/contabilidad/asientos/${id}/contabilizar`,
      {
        headers: cabeceras(token),
      },
    );
    expect(contabilizado.status()).toBe(200);

    await ir(page, '/contabilidad/periodos');
    await expect(page.getByRole('heading', { level: 1, name: 'Periodos contables' })).toBeVisible();
    const fila = page.getByRole('row', { name: new RegExp(`^${etiqueta}`) });
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: `Cerrar el periodo ${etiqueta}` }).click();
    await page.getByRole('button', { name: 'Cerrar periodo', exact: true }).click();
    await expect(
      page.getByText('Quien registró asientos del periodo no puede cerrarlo'),
    ).toBeVisible();
    await expect(fila.getByText('Abierto')).toBeVisible();

    const contextoGerencia = await browser.newContext();
    const gerencia = await contextoGerencia.newPage();
    await iniciarSesion(gerencia, 'gerencia');
    await ir(gerencia, '/contabilidad/periodos');
    const filaGerencia = gerencia.getByRole('row', { name: new RegExp(`^${etiqueta}`) });
    await filaGerencia.getByRole('button', { name: `Cerrar el periodo ${etiqueta}` }).click();
    await gerencia.getByRole('button', { name: 'Cerrar periodo', exact: true }).click();
    await expect(gerencia.getByText(`El periodo ${etiqueta} fue cerrado.`)).toBeVisible();
    await expect(filaGerencia.getByText('Cerrado')).toBeVisible();
    await expect(
      filaGerencia.getByRole('button', { name: `Reabrir el periodo ${etiqueta}` }),
    ).toBeVisible();
    await contextoGerencia.close();

    await ir(page, '/contabilidad/periodos');
    await expect(
      page.getByRole('row', { name: new RegExp(`^${etiqueta}`) }).getByRole('button', {
        name: `Reabrir el periodo ${etiqueta}`,
      }),
    ).toHaveCount(0);
  });

  test('gerencia consulta los libros pero no registra ni configura', async ({ page }) => {
    await iniciarSesion(page, 'gerencia');
    await ir(page, '/contabilidad/asientos');
    await expect(page.getByRole('heading', { level: 1, name: 'Asientos contables' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Nuevo asiento' })).toHaveCount(0);
    await ir(page, '/contabilidad/plantillas');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Plantillas de asiento' }),
    ).toBeVisible();
    await expect(page.getByRole('button', { name: /^Editar la plantilla/ })).toHaveCount(0);
    await ir(page, '/contabilidad/libros');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Libros y estados financieros' }),
    ).toBeVisible();
    await page.getByRole('button', { name: 'Consultar' }).click();
    await expect(page.getByText('Cuadra', { exact: true }).first()).toBeVisible();
  });

  test('finanzas importa un extracto, revisa la sugerencia y concilia', async ({ page }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    await iniciarSesion(page, 'finanzas');
    const token = await tokenDe(page, 'finanzas');
    await asegurarPeriodoActual(page.request, token);
    const cuentas = (await (
      await page.request.get('/api/v1/contabilidad/cuentas', { headers: cabeceras(token) })
    ).json()) as { id: string; codigo: string }[];
    const banco = cuentas.find((c) => c.codigo === '1041')!;
    const capital = cuentas.find((c) => c.codigo === '5011')!;
    const nombre = `Banco E2E ${marca}`;
    const tesoreria = await page.request.post('/api/v1/contabilidad/tesoreria/cuentas', {
      headers: cabeceras(token),
      data: {
        nombre,
        tipo: 'BANCO',
        banco: 'BBVA',
        ultimos4: '4321',
        cuentaContableId: banco.id,
        saldoInicial: 0,
        predeterminada: false,
      },
    });
    expect(tesoreria.status(), await tesoreria.text()).toBe(201);
    const cuenta = (await tesoreria.json()) as { id: string };
    const centimos = (Math.floor(Math.random() * 90) + 10).toString();
    const monto = `1${marca.slice(-3)}.${centimos}`;
    const movimiento = await page.request.post('/api/v1/contabilidad/tesoreria/movimientos', {
      headers: cabeceras(token),
      data: {
        cuentaId: cuenta.id,
        fecha: isoHoy(),
        tipo: 'INGRESO',
        monto: Number(monto),
        descripcion: `Depósito E2E ${marca}`,
        referencia: `OP-${marca}`,
        contrapartidaId: capital.id,
      },
    });
    expect(movimiento.status(), await movimiento.text()).toBe(201);

    await ir(page, '/contabilidad/conciliacion');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Conciliación bancaria' }),
    ).toBeVisible();
    await elegirOpcion(
      page,
      page.getByRole('combobox', { name: 'Cuenta bancaria a conciliar' }),
      nombre,
    );
    const csv = `fecha,descripcion,referencia,monto\n${isoHoy()},Abono transferencia OP-${marca},OP-${marca},${monto}\n`;
    await page.locator('#cn-archivo').setInputFiles({
      name: `extracto-${marca}.csv`,
      mimeType: 'text/csv',
      buffer: Buffer.from(csv),
    });
    await expect(page.getByText('Se importaron 1 líneas.')).toBeVisible();
    const sugerida = page
      .getByRole('row', { name: new RegExp(`Abono transferencia OP-${marca}`) })
      .first();
    await expect(sugerida).toBeVisible();
    await sugerida.getByRole('button', { name: /^Conciliar / }).click();
    await expect(page.getByText('La línea fue conciliada.')).toBeVisible();
    await expect(page.getByText('Todo conciliado')).toBeVisible();
  });

  test('la tesorería registra un egreso y actualiza el saldo de la cuenta', async ({ page }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    await iniciarSesion(page, 'finanzas');
    const token = await tokenDe(page, 'finanzas');
    await asegurarPeriodoActual(page.request, token);
    const debe = await crearCuenta(page.request, token, 'ACTIVO', 5, marca);
    const nombre = `Caja E2E ${marca}`;
    const cajaChica = await page.request.post('/api/v1/contabilidad/tesoreria/cuentas', {
      headers: cabeceras(token),
      data: {
        nombre,
        tipo: 'CAJA',
        cuentaContableId: debe.id,
        saldoInicial: 500,
        predeterminada: false,
      },
    });
    expect(cajaChica.status(), await cajaChica.text()).toBe(201);

    await ir(page, '/contabilidad/tesoreria');
    await expect(page.getByRole('heading', { level: 1, name: 'Tesorería' })).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(`^${nombre}`) })).toContainText(
      /S\/\s?500\.00/,
    );
    await page.getByRole('button', { name: 'Nuevo movimiento' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Nuevo movimiento' });
    await elegirOpcion(page, dialogo.locator('#mv-cuenta'), nombre);
    await elegirOpcion(page, dialogo.locator('#mv-tipo'), 'Egreso');
    await dialogo.locator('#mv-monto input, input#mv-monto').first().fill('120');
    await dialogo.locator('#mv-desc').fill(`Compra menor ${marca}`);
    await elegirOpcion(page, dialogo.locator('#mv-contra'), /6591 Otros gastos de gestión/);
    await dialogo.getByRole('button', { name: 'Registrar' }).click();
    await expect(page.getByText('El movimiento fue registrado y contabilizado.')).toBeVisible();
    await expect(page.getByRole('row', { name: new RegExp(`^${nombre}`) })).toContainText(
      /S\/\s?380\.00/,
    );
    await expect(
      page.getByRole('row', { name: new RegExp(`Compra menor ${marca}`) }),
    ).toBeVisible();
  });
});
