import { APIRequestContext, Page, expect, test } from '@playwright/test';

import { confirmarMotivo, iniciarSesion, ir, tokenDe } from './support';

const PESOS = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];

function rucValido(): string {
  const base = '20' + String(Math.floor(10_000_000 + Math.random() * 80_000_000));
  let suma = 0;
  for (let i = 0; i < 10; i++) {
    suma += Number(base[i]) * PESOS[i];
  }
  const resto = 11 - (suma % 11);
  const digito = resto === 10 ? 0 : resto === 11 ? 1 : resto;
  return base + digito;
}

function sufijo(): string {
  return `${Date.now().toString().slice(-7)}${Math.floor(Math.random() * 90 + 10)}`;
}

function cabeceras(token: string) {
  return { Authorization: `Bearer ${token}` };
}

interface Creado {
  id: string;
}

async function crear(
  request: APIRequestContext,
  token: string,
  url: string,
  data: unknown,
): Promise<Creado> {
  const respuesta = await request.post(url, { headers: cabeceras(token), data });
  expect(respuesta.status(), `${url} ${await respuesta.text()}`).toBe(201);
  return (await respuesta.json()) as Creado;
}

async function crearOrden(
  request: APIRequestContext,
  token: string,
  data: unknown,
): Promise<Creado> {
  const respuesta = await request.post('/api/v1/compras/ordenes', {
    headers: cabeceras(token),
    data,
  });
  expect(respuesta.status(), await respuesta.text()).toBe(201);
  const detalle = (await respuesta.json()) as { resumen: { id: string } };
  return { id: detalle.resumen.id };
}

async function producto(page: Page, nombre: string, stockMinimo = 5): Promise<Creado> {
  const token = await tokenDe(page, 'almacen');
  const codigo = `E2E-${sufijo()}`;
  return crear(page.request, token, '/api/v1/almacen/productos', {
    codigo,
    nombre,
    tipo: 'MEDICAMENTO',
    principioActivo: 'Principio',
    presentacion: 'Caja',
    unidad: 'unidad',
    precioVenta: 3,
    stockMinimo,
    requiereReceta: false,
    activo: true,
  });
}

async function proveedor(
  page: Page,
  nombre: string,
  condicionPagoDias = 30,
): Promise<Creado & { ruc: string }> {
  const token = await tokenDe(page, 'almacen');
  const ruc = rucValido();
  const creado = await crear(page.request, token, '/api/v1/almacen/proveedores', {
    ruc,
    razonSocial: nombre,
    condicionPagoDias,
    plazoEntregaDias: 3,
    activo: true,
  });
  return { ...creado, ruc };
}

async function ordenAprobadaYRecibida(
  page: Page,
  proveedorId: string,
  productoId: string,
  cantidad: number,
  precio: number,
): Promise<string> {
  const almacen = await tokenDe(page, 'almacen');
  const finanzas = await tokenDe(page, 'finanzas');
  const orden = await crearOrden(page.request, almacen, {
    proveedorId,
    afectaIgv: true,
    condicionPagoDias: 30,
    items: [{ productoId, cantidad, precioUnitario: precio }],
  });
  const enviada = await page.request.post(`/api/v1/compras/ordenes/${orden.id}/enviar-aprobacion`, {
    headers: cabeceras(almacen),
    data: {},
  });
  expect(enviada.status(), await enviada.text()).toBe(200);
  const aprobada = await page.request.post(`/api/v1/compras/ordenes/${orden.id}/aprobar`, {
    headers: cabeceras(finanzas),
    data: {},
  });
  expect(aprobada.status()).toBe(200);
  const detalle = (await aprobada.json()) as { items: { id: string }[] };
  const vence = new Date();
  vence.setMonth(vence.getMonth() + 8);
  const recepcion = await page.request.post(`/api/v1/compras/ordenes/${orden.id}/recepciones`, {
    headers: cabeceras(almacen),
    data: {
      guiaRemision: `T001-${sufijo()}`,
      items: [
        {
          ordenItemId: detalle.items[0].id,
          cantidadRecibida: cantidad,
          numeroLote: `L-${sufijo()}`,
          fechaVencimiento: vence.toISOString().slice(0, 10),
        },
      ],
    },
  });
  expect(recepcion.status()).toBe(201);
  return orden.id;
}

async function elegirOpcion(
  page: Page,
  combo: ReturnType<Page['locator']>,
  texto: string | RegExp,
) {
  await combo.click();
  await page.getByRole('option', { name: texto }).first().click();
}

test.describe('compras y cuentas por pagar', () => {
  test('la farmacia pide, el almacén cotiza y ordena, finanzas aprueba y el almacén recibe', async ({
    page,
    browser,
  }) => {
    test.setTimeout(120_000);
    const marca = sufijo();
    const articulo = await producto(page, `Producto E2E ${marca}`);
    await proveedor(page, `Droguería Barata ${marca}`);
    await proveedor(page, `Droguería Cara ${marca}`, 15);

    await iniciarSesion(page, 'farmacia');
    await ir(page, '/compras/requisiciones');
    await expect(page.getByRole('heading', { level: 1, name: 'Requisiciones' })).toBeVisible();
    await page.getByRole('button', { name: 'Nueva requisición' }).click();
    await page.locator('#req-motivo').fill(`Reposición E2E ${marca}`);
    await elegirOpcion(
      page,
      page.getByRole('combobox', { name: 'Producto 1' }),
      new RegExp(`Producto E2E ${marca}`),
    );
    await page.locator('#req-cantidad-0').fill('100');
    await page.getByRole('button', { name: 'Guardar y enviar' }).click();
    await expect(page.getByRole('heading', { level: 1, name: /^REQ-\d{4}-\d{6}$/ })).toBeVisible();
    await expect(page.getByText('Enviada', { exact: true })).toBeVisible();
    const codigoRequisicion = (await page.getByRole('heading', { level: 1 }).innerText()).trim();
    await expect(page.getByRole('button', { name: 'Anular' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Cotizaciones', exact: true })).toHaveCount(0);

    const almacen = await browser.newContext();
    const pa = await almacen.newPage();
    await iniciarSesion(pa, 'almacen');
    await ir(pa, '/compras/requisiciones');
    await pa.getByRole('button', { name: `Ver la requisición ${codigoRequisicion}` }).click();
    await expect(pa.getByRole('heading', { name: 'Cotizaciones', exact: true })).toBeVisible();

    for (const [proveedorNombre, precio] of [
      [`Droguería Barata ${marca}`, '1.5'],
      [`Droguería Cara ${marca}`, '2'],
    ]) {
      await pa.getByRole('button', { name: 'Registrar cotización' }).click();
      await elegirOpcion(pa, pa.locator('#cot-proveedor'), proveedorNombre);
      await pa.locator('#cot-precio-0').fill(precio);
      await pa.getByRole('button', { name: 'Guardar cotización' }).click();
      await expect(pa.getByRole('row', { name: new RegExp(`^${proveedorNombre}`) })).toBeVisible();
    }
    await expect(pa.getByRole('heading', { name: 'Cuadro comparativo' })).toBeVisible();
    await expect(
      pa.getByRole('columnheader', { name: new RegExp(`Droguería Cara ${marca}`) }),
    ).toBeVisible();

    await pa
      .getByRole('button', { name: `Seleccionar la cotización de Droguería Barata ${marca}` })
      .click();
    await pa.getByRole('button', { name: 'Generar orden' }).click();
    await expect(
      pa.getByRole('heading', { level: 1, name: 'Orden de compra en borrador' }),
    ).toBeVisible();
    await expect(pa.getByText('Borrador', { exact: true }).first()).toBeVisible();
    await pa.getByRole('button', { name: 'Enviar a aprobación' }).click();
    await expect(pa.getByText('Pendiente de finanzas')).toBeVisible();
    const urlOrden = pa.url();
    await expect(pa.getByRole('button', { name: 'Aprobar orden' })).toHaveCount(0);

    const finanzas = await browser.newContext();
    const pf = await finanzas.newPage();
    await iniciarSesion(pf, 'finanzas');
    await ir(pf, '/compras/ordenes');
    await expect(
      pf.getByRole('row', { name: new RegExp(`Droguería Barata ${marca}`) }),
    ).toBeVisible();
    await pf
      .getByRole('row', { name: new RegExp(`Droguería Barata ${marca}`) })
      .getByRole('button', { name: /Ver la orden/ })
      .click();
    await pf.getByRole('button', { name: 'Aprobar orden' }).click();
    await expect(pf.getByRole('heading', { level: 1, name: /^OC-\d{4}-\d{6}$/ })).toBeVisible();
    await expect(pf.getByText('Aprobada', { exact: true }).first()).toBeVisible();
    await expect(pf.getByText(/Huella de la orden aprobada/)).toBeVisible();
    await expect(pf.getByRole('button', { name: 'Registrar recepción' })).toHaveCount(0);
    await expect(pf.getByRole('button', { name: 'Descargar PDF' })).toBeVisible();

    await pa.goto(urlOrden);
    await expect(pa.getByRole('heading', { level: 1, name: /^OC-/ })).toBeVisible();
    await expect(pa.getByRole('button', { name: 'Editar' })).toHaveCount(0);
    await pa.getByRole('button', { name: 'Registrar recepción' }).click();
    const dialogo = pa.getByRole('dialog', { name: 'Registrar recepción' });
    await dialogo.getByLabel(/Lote de Producto E2E/).fill(`LOTE-${marca}`);
    const vence = new Date();
    vence.setMonth(vence.getMonth() + 9);
    const texto = `${String(vence.getDate()).padStart(2, '0')}/${String(vence.getMonth() + 1).padStart(2, '0')}/${vence.getFullYear()}`;
    const campo = dialogo.getByLabel(/Vencimiento de Producto E2E/);
    await campo.click();
    await campo.pressSequentially(texto);
    await campo.press('Tab');
    await dialogo.getByRole('button', { name: 'Confirmar recepción' }).click();
    await expect(pa.getByText('Recibida', { exact: true }).first()).toBeVisible();
    await expect(pa.getByRole('heading', { name: 'Recepciones' })).toBeVisible();

    const token = await tokenDe(page, 'almacen');
    const stock = await page.request.get(
      `/api/v1/almacen/productos?q=${encodeURIComponent(`Producto E2E ${marca}`)}`,
      { headers: cabeceras(token) },
    );
    expect(stock.status()).toBe(200);
    const productos = (await stock.json()) as { id: string; stockActual: number }[];
    expect(productos.find((p) => p.id === articulo.id)?.stockActual).toBe(100);
    await almacen.close();
    await finanzas.close();
  });

  test('finanzas registra la factura de un proveedor, la paga en dos partes y ve su vencimiento', async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    const articulo = await producto(page, `Insumo E2E ${marca}`);
    const proveedorCreado = await proveedor(page, `Proveedor Pagos ${marca}`, 0);
    const ordenId = await ordenAprobadaYRecibida(page, proveedorCreado.id, articulo.id, 10, 10);

    await iniciarSesion(page, 'finanzas');
    await ir(page, `/compras/cuentas-por-pagar?ordenId=${ordenId}`);
    const numeroFactura = sufijo().slice(0, 8);
    const dialogo = page.getByRole('dialog', { name: 'Registrar comprobante del proveedor' });
    await expect(dialogo).toBeVisible();
    await expect(dialogo.locator('#cp-ruc')).toHaveValue(proveedorCreado.ruc);
    await dialogo.locator('#cp-serie').fill('F001');
    await dialogo.locator('#cp-numero').fill(numeroFactura);
    await dialogo.locator('#cp-subtotal').fill('100');
    await dialogo.locator('#cp-subtotal').blur();
    await dialogo.getByRole('button', { name: 'Registrar comprobante' }).click();
    const fila = page.getByRole('row', { name: new RegExp(`F001-${numeroFactura}`) });
    await expect(fila).toBeVisible();
    await expect(fila.getByText('Pendiente', { exact: true })).toBeVisible();

    await fila.getByRole('button', { name: /Registrar un pago de/ }).click();
    const pago = page.getByRole('dialog', { name: /Registrar pago de/ });
    await pago.locator('#pago-monto').fill('50');
    await pago.getByRole('button', { name: 'Confirmar pago' }).click();
    await expect(fila.getByText('Pago parcial')).toBeVisible();

    await fila.getByRole('button', { name: /Registrar un pago de/ }).click();
    await page
      .getByRole('dialog', { name: /Registrar pago de/ })
      .getByRole('button', { name: 'Confirmar pago' })
      .click();
    await expect(fila.getByText('Pagado', { exact: true })).toBeVisible();
    await expect(fila.getByRole('button', { name: /Registrar un pago de/ })).toHaveCount(0);

    await fila.getByRole('button', { name: /Ver los pagos de/ }).click();
    await expect(page.getByRole('dialog', { name: /Pagos de/ })).toContainText('Transferencia');
  });

  test('una orden mayor al límite solo la aprueba la gerencia', async ({ page, browser }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    const articulo = await producto(page, `Equipo E2E ${marca}`);
    const proveedorCreado = await proveedor(page, `Proveedor Grande ${marca}`, 0);
    const almacen = await tokenDe(page, 'almacen');
    const orden = await crearOrden(page.request, almacen, {
      proveedorId: proveedorCreado.id,
      afectaIgv: true,
      items: [{ productoId: articulo.id, cantidad: 10, precioUnitario: 1000 }],
    });
    const enviada = await page.request.post(
      `/api/v1/compras/ordenes/${orden.id}/enviar-aprobacion`,
      { headers: cabeceras(almacen), data: {} },
    );
    expect(enviada.status(), await enviada.text()).toBe(200);

    await iniciarSesion(page, 'finanzas');
    await ir(page, `/compras/ordenes/${orden.id}`);
    await expect(page.getByText(/la aprueba la gerencia/)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Aprobar orden' })).toBeDisabled();

    const gerencia = await browser.newContext();
    const pg = await gerencia.newPage();
    await iniciarSesion(pg, 'gerencia');
    await ir(pg, `/compras/ordenes/${orden.id}`);
    await pg.getByRole('button', { name: 'Aprobar orden' }).click();
    await expect(pg.getByRole('heading', { level: 1, name: /^OC-/ })).toBeVisible();
    await expect(pg.getByText('Aprobada', { exact: true }).first()).toBeVisible();
    await gerencia.close();
  });

  test('una orden rechazada exige motivo y queda sin número', async ({ page }) => {
    test.setTimeout(90_000);
    const marca = sufijo();
    const articulo = await producto(page, `Material E2E ${marca}`);
    const proveedorCreado = await proveedor(page, `Proveedor Rechazo ${marca}`, 0);
    const almacen = await tokenDe(page, 'almacen');
    const orden = await crearOrden(page.request, almacen, {
      proveedorId: proveedorCreado.id,
      afectaIgv: true,
      items: [{ productoId: articulo.id, cantidad: 4, precioUnitario: 5 }],
    });
    await page.request.post(`/api/v1/compras/ordenes/${orden.id}/enviar-aprobacion`, {
      headers: cabeceras(almacen),
      data: {},
    });

    await iniciarSesion(page, 'finanzas');
    await ir(page, `/compras/ordenes/${orden.id}`);
    await page.getByRole('button', { name: 'Rechazar' }).click();
    await confirmarMotivo(
      page,
      'Rechazar orden de compra',
      'Precio fuera de rango',
      'Rechazar orden',
    );
    await expect(page.getByText('Rechazada', { exact: true }).first()).toBeVisible();
    await expect(
      page.getByRole('heading', { level: 1, name: 'Orden de compra en borrador' }),
    ).toBeVisible();
    await expect(page.getByText('Precio fuera de rango').first()).toBeVisible();
  });

  test('cada rol ve solo sus herramientas de compras', async ({ page, browser }) => {
    await iniciarSesion(page, 'farmacia');
    await expect(page.getByRole('link', { name: 'Requisiciones' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Órdenes de compra' })).toHaveCount(0);
    await ir(page, '/compras/ordenes');
    await expect(page.getByRole('heading', { name: 'Página no encontrada' })).toBeVisible();

    const almacen = await browser.newContext();
    const pa = await almacen.newPage();
    await iniciarSesion(pa, 'almacen');
    await expect(pa.getByRole('link', { name: 'Órdenes de compra' })).toBeVisible();
    await expect(pa.getByRole('link', { name: 'Recepciones y devoluciones' })).toBeVisible();
    await expect(pa.getByRole('link', { name: 'Cuentas por pagar' })).toHaveCount(0);
    await ir(pa, '/compras/cuentas-por-pagar');
    await expect(pa.getByRole('heading', { name: 'Página no encontrada' })).toBeVisible();
    await almacen.close();

    const finanzas = await browser.newContext();
    const pf = await finanzas.newPage();
    await iniciarSesion(pf, 'finanzas');
    await expect(pf.getByRole('link', { name: 'Cuentas por pagar' })).toBeVisible();
    await ir(pf, '/compras/cuentas-por-pagar');
    await expect(pf.getByRole('heading', { level: 1, name: 'Cuentas por pagar' })).toBeVisible();
    await ir(pf, '/compras/reporte');
    await expect(
      pf.getByRole('heading', { level: 1, name: 'Compras por proveedor' }),
    ).toBeVisible();
    await expect(pf.getByRole('button', { name: 'Exportar a Excel' })).toBeVisible();
    await finanzas.close();
  });

  test('el almacén registra un proveedor con condiciones y rechaza un RUC inválido', async ({
    page,
  }) => {
    const marca = sufijo();
    await iniciarSesion(page, 'almacen');
    await ir(page, '/almacen/proveedores');
    await page.getByRole('button', { name: 'Nuevo proveedor' }).click();
    await page.locator('#prv-ruc').fill('20123456780');
    await page.locator('#prv-razon').fill(`Proveedor inválido ${marca}`);
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByRole('dialog').getByText(/no es válido/)).toBeVisible();

    await page.locator('#prv-ruc').fill(rucValido());
    await page.locator('#prv-razon').fill(`Proveedor válido ${marca}`);
    await page.locator('#prv-pago').fill('45');
    await page.getByRole('button', { name: 'Guardar' }).click();
    const fila = page.getByRole('row', { name: new RegExp(`Proveedor válido ${marca}`) });
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('45 días de crédito');
  });
});
