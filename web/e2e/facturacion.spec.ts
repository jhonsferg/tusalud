import { Page, expect, test } from '@playwright/test';

import { iniciarSesion, ir, registrarPaciente, tokenDe } from './support';

const RUC_VALIDO = '20100070970';
const RUC_INVALIDO = '20123456789';

interface Cobro {
  id: string;
  codigo: string;
}

async function asegurarCaja(page: Page): Promise<string> {
  const token = await tokenDe(page, 'caja');
  const cabeceras = { Authorization: `Bearer ${token}` };
  const actual = await page.request.get('/api/v1/caja/actual', { headers: cabeceras });
  if (actual.status() === 404) {
    const apertura = await page.request.post('/api/v1/caja/aperturas', {
      headers: cabeceras,
      data: { montoApertura: 50 },
    });
    expect(apertura.status()).toBe(201);
  }
  return token;
}

async function cobrarFactura(
  page: Page,
  ruc: string,
  nombre: string,
  precio = 236,
): Promise<Cobro> {
  const token = await asegurarCaja(page);
  const respuesta = await page.request.post('/api/v1/caja/cobros', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      origen: 'SERVICIO',
      tipoComprobante: 'FACTURA',
      clienteTipoDoc: 'RUC',
      clienteDoc: ruc,
      clienteNombre: nombre,
      metodoPago: 'TRANSFERENCIA',
      items: [{ descripcion: 'Chequeo ocupacional', cantidad: 1, precioUnitario: precio }],
    },
  });
  expect(respuesta.status()).toBe(201);
  return (await respuesta.json()) as Cobro;
}

async function procesarEnvios(page: Page): Promise<void> {
  const token = await tokenDe(page, 'finanzas');
  const respuesta = await page.request.post('/api/v1/facturacion/cola/procesar', {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(respuesta.status()).toBe(200);
}

async function abrirComprobante(page: Page, nombre: string): Promise<void> {
  await ir(page, '/comprobantes-electronicos');
  await page.getByRole('searchbox', { name: 'Buscar comprobante' }).fill(nombre);
  const fila = page.getByRole('row', { name: new RegExp(nombre) });
  await expect(fila).toBeVisible();
  await fila.getByRole('button', { name: /Ver el detalle de/ }).click();
  await expect(
    page.getByRole('heading', { name: 'Firma y constancia de recepción' }),
  ).toBeVisible();
}

test.describe('facturación electrónica', () => {
  test('finanzas ve el comprobante firmado y su constancia de recepción', async ({ page }) => {
    const nombre = `Empresa Fiscal ${Date.now()}`;
    await cobrarFactura(page, RUC_VALIDO, nombre);
    await procesarEnvios(page);

    await iniciarSesion(page, 'finanzas');
    await abrirComprobante(page, nombre);

    await expect(page.getByText(/Aceptado|Con observaciones/).first()).toBeVisible();
    await expect(page.getByText('Envío del comprobante')).toBeVisible();
    await expect(page.getByText('Correcto')).toBeVisible();
    await expect(page.getByRole('button', { name: 'XML firmado' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Emitir nota' })).toBeVisible();
  });

  test('un RUC incorrecto se rechaza y se corrige desde el detalle sin cambiar el número', async ({
    page,
  }) => {
    const nombre = `Empresa Rechazo ${Date.now()}`;
    await cobrarFactura(page, RUC_INVALIDO, nombre, 118);
    await procesarEnvios(page);

    await iniciarSesion(page, 'finanzas');
    await abrirComprobante(page, nombre);
    await expect(page.getByText(/SUNAT rechazó el comprobante/)).toBeVisible();
    const numero = await page.getByRole('heading', { level: 1 }).textContent();

    await page.getByRole('button', { name: 'Corregir datos del cliente' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Corregir datos del cliente' });
    await dialogo.locator('#cor-doc').fill(RUC_VALIDO);
    await dialogo.locator('#cor-dir').fill('Jr. Comercio 45, Lima');
    await dialogo.getByRole('button', { name: 'Corregir y reenviar' }).click();

    await expect(dialogo).toBeHidden();
    await expect(page.getByText(/SUNAT rechazó el comprobante/)).toBeHidden();
    await expect(page.getByText('Aceptado', { exact: true }).first()).toBeVisible();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(numero ?? '');
  });

  test('finanzas emite una nota de crédito total y el comprobante queda acreditado', async ({
    page,
  }) => {
    const nombre = `Empresa Nota ${Date.now()}`;
    await cobrarFactura(page, RUC_VALIDO, nombre);
    await procesarEnvios(page);

    await iniciarSesion(page, 'finanzas');
    await abrirComprobante(page, nombre);
    await page.getByRole('button', { name: 'Emitir nota' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Emitir una nota' });
    await expect(dialogo.getByText(/repite todos los ítems/)).toBeVisible();
    await dialogo.locator('#nota-desc').fill('Anulación de la operación solicitada');
    await dialogo.getByRole('button', { name: 'Emitir nota' }).click();

    await expect(dialogo).toBeHidden();
    await expect(page.getByText('Acreditado con notas')).toBeVisible();
  });

  test('la caja ve la bandeja pero no las acciones fiscales', async ({ page }) => {
    const nombre = `Empresa Caja ${Date.now()}`;
    await cobrarFactura(page, RUC_VALIDO, nombre);
    await procesarEnvios(page);

    await iniciarSesion(page, 'caja');
    await abrirComprobante(page, nombre);

    await expect(page.getByRole('button', { name: 'Ver PDF' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'XML firmado' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Emitir nota' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Dar de baja' })).toHaveCount(0);
  });

  test('el paciente ve y descarga el PDF de su comprobante', async ({ page }) => {
    const cuenta = await registrarPaciente(page, 'cpe');
    const tokenRecepcion = await tokenDe(page, 'recepcion');
    const dni = /\.(\d+)@/.exec(cuenta.email)![1].slice(-8);
    const busqueda = await page.request.get(`/api/v1/recepcion/pacientes?q=${dni}`, {
      headers: { Authorization: `Bearer ${tokenRecepcion}` },
    });
    const [paciente] = (await busqueda.json()) as { pacienteId: string }[];
    const tokenCaja = await asegurarCaja(page);
    const cobro = await page.request.post('/api/v1/caja/cobros', {
      headers: { Authorization: `Bearer ${tokenCaja}` },
      data: {
        origen: 'SERVICIO',
        pacienteId: paciente.pacienteId,
        tipoComprobante: 'BOLETA',
        clienteTipoDoc: 'DNI',
        clienteDoc: dni,
        clienteNombre: 'Prueba cpe',
        metodoPago: 'EFECTIVO',
        items: [{ descripcion: 'Curación simple', cantidad: 1, precioUnitario: 59 }],
      },
    });
    expect(cobro.status()).toBe(201);
    await procesarEnvios(page);

    await ir(page, '/mis-comprobantes');
    await expect(page.getByRole('heading', { name: 'Mis comprobantes' })).toBeVisible();
    const fila = page.getByRole('row', { name: /Boleta de venta|Boleta/ }).first();
    await expect(fila).toBeVisible();
    const descarga = page.waitForEvent('download');
    await fila.getByRole('button', { name: /Descargar el PDF de/ }).click();
    expect((await descarga).suggestedFilename()).toMatch(/\.pdf$/);
  });

  test('finanzas consulta la configuración fiscal sin ver credenciales y las series', async ({
    page,
  }) => {
    await iniciarSesion(page, 'finanzas');
    await ir(page, '/configuracion-fiscal');

    await expect(page.getByRole('heading', { name: 'Configuración fiscal' })).toBeVisible();
    await expect(page.getByText(/secretos del servidor/)).toBeVisible();
    await expect(page.getByRole('cell', { name: 'FC01', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'F001', exact: true })).toBeVisible();
    await expect(page.getByLabel('RUC')).toHaveValue(/^\d{11}$/);
  });

  test('la caja no abre la configuración fiscal', async ({ page }) => {
    await iniciarSesion(page, 'caja');
    await ir(page, '/configuracion-fiscal');
    await expect(page.getByRole('heading', { name: 'Configuración fiscal' })).toHaveCount(0);
    await expect(page.getByText(/no encontrada/i).first()).toBeVisible();
  });
});
