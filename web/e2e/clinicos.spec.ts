import { Page, expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, omitirGuia } from './support';

async function entrarCon(page: Page, email: string, password: string, home: RegExp) {
  await omitirGuia(page);
  await page.goto('/auth/login');
  await page.getByLabel('Email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(home);
}

async function escribirNumero(page: Page, selector: string, valor: string) {
  const campo = page.locator(selector);
  await campo.click();
  await page.keyboard.press('Control+a');
  await page.keyboard.type(valor);
}

function fechaEnUnAnio(): string {
  const f = new Date(Date.now() + 366 * 86_400_000);
  return `${String(f.getDate()).padStart(2, '0')}/${String(f.getMonth() + 1).padStart(2, '0')}/${f.getFullYear()}`;
}

test.describe('personal de la institución', () => {
  test('el administrador registra personal y este entra a su módulo', async ({ page }) => {
    const sufijo = Date.now().toString();
    const correo = `almacen.${sufijo}@tusalud.com`;
    await iniciarSesion(page, 'admin');
    await page.goto('/admin/usuarios');
    await page.getByRole('button', { name: 'Nuevo personal' }).click();
    await page.locator('#per-nuevo-nombre').fill('Marta');
    await page.locator('#per-nuevo-apellido').fill('Soto');
    await page.locator('#per-nuevo-email').fill(correo);
    await page.locator('#per-nuevo-password').fill('Personal123!');
    await page.locator('p-multiselect').click();
    await page.getByRole('option', { name: 'Almacén' }).click();
    await page.keyboard.press('Escape');
    await page.getByRole('button', { name: /Registrar$/ }).click();
    await expect(page.getByText(/fue registrado como personal/)).toBeVisible();

    await cerrarSesion(page);
    await entrarCon(page, correo, 'Personal123!', /\/almacen$/);
    await expect(page.getByRole('heading', { name: 'Almacén' })).toBeVisible();
  });

  test('cada rol ve en el menú solo lo que le corresponde', async ({ browser }) => {
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await expect(paciente.getByRole('link', { name: 'Mis resultados' })).toBeVisible();
    await expect(paciente.getByRole('link', { name: 'Mis medicamentos' })).toBeVisible();
    await expect(paciente.getByRole('link', { name: 'Proveedores' })).toHaveCount(0);

    const farmacia = await (await browser.newContext()).newPage();
    await iniciarSesion(farmacia, 'farmacia');
    await expect(farmacia.getByRole('link', { name: 'Dispensar' })).toBeVisible();
    await expect(farmacia.getByRole('link', { name: 'Proveedores' })).toHaveCount(0);
    await farmacia.goto('/almacen/ingresos');
    await expect(farmacia).toHaveURL(/\/farmacia$/);
  });
});

test.describe('almacén', () => {
  test('registra un producto, un ingreso con lote y una salida', async ({ page }) => {
    test.setTimeout(90_000);
    const codigo = `E2E${Date.now().toString().slice(-7)}`;
    const nombre = `Producto ${codigo}`;
    await iniciarSesion(page, 'almacen');
    await expect(page.getByText('Productos activos')).toBeVisible();
    await expect(page.getByText('Valor del inventario')).toBeVisible();

    await page.goto('/almacen/productos');
    await page.getByPlaceholder('Buscar por código o nombre').fill('Paracetamol');
    await expect(page.getByRole('cell', { name: /Paracetamol/ }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo producto' }).click();
    await page.locator('#prod-codigo').fill(codigo);
    await page.locator('#prod-nombre').fill(nombre);
    await escribirNumero(page, '#prod-minimo', '5');
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText(`${nombre} fue registrado.`)).toBeVisible();

    await page.goto('/almacen/ingresos');
    await page.getByRole('button', { name: 'Nuevo ingreso' }).click();
    await page.locator('#ing-producto-0').click();
    await page.getByRole('option', { name: new RegExp(codigo) }).click();
    await page.locator('#ing-lote-0').fill(`L-${codigo}`);
    await page.locator('#ing-vence-0').pressSequentially(fechaEnUnAnio());
    await page.keyboard.press('Tab');
    await escribirNumero(page, '#ing-cantidad-0', '50');
    await escribirNumero(page, '#ing-costo-0', '2');
    await page.getByRole('button', { name: /Registrar ingreso$/ }).click();
    await expect(page.getByText('El ingreso fue registrado y el stock actualizado.')).toBeVisible();

    await page.goto('/almacen/productos');
    await page.getByPlaceholder('Buscar por código o nombre').fill(codigo);
    await expect(page.getByRole('cell', { name: new RegExp(codigo) }).first()).toBeVisible();
    await expect(page.getByText('50 unidad')).toBeVisible();

    await page.getByRole('button', { name: `Registrar salida de ${nombre}` }).click();
    await escribirNumero(page, '#sal-cantidad', '60');
    await page.getByRole('button', { name: /Registrar salida$/ }).click();
    await expect(page.getByText(/Stock insuficiente/i).first()).toBeVisible();
    await escribirNumero(page, '#sal-cantidad', '10');
    await page.getByRole('button', { name: /Registrar salida$/ }).click();
    await expect(page.getByText('La salida fue registrada.')).toBeVisible();
    await expect(page.getByText('40 unidad')).toBeVisible();
  });

  test('muestra el kardex y los proveedores', async ({ page }) => {
    await iniciarSesion(page, 'almacen');
    await page.goto('/almacen/movimientos');
    await expect(page.getByRole('heading', { name: 'Kardex' })).toBeVisible();
    await expect(page.getByRole('row').nth(1)).toBeVisible();
    await page.goto('/almacen/proveedores');
    await expect(page.getByRole('heading', { name: 'Proveedores' })).toBeVisible();
    await expect(page.getByRole('row').nth(1)).toBeVisible();
  });
});

test.describe('farmacia', () => {
  test('dispensa una receta pendiente y anula la dispensación', async ({ page }) => {
    test.setTimeout(90_000);
    await iniciarSesion(page, 'farmacia');
    await expect(page.getByText('Recetas pendientes', { exact: true }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Dispensar' }).first().click();
    await expect(page).toHaveURL(/\/farmacia\/dispensar\?atencionId=[0-9a-f-]{36}/);

    const filas = page.locator('[id^="disp-producto-"]');
    await expect(filas.first()).toBeVisible();
    const total = await filas.count();
    for (let i = 0; i < total; i++) {
      const selector = page.locator(`#disp-producto-${i}`);
      const vacio = await selector.getAttribute('aria-label');
      if (!vacio || /Producto/.test(vacio)) {
        await selector.click();
        await page.getByRole('option').first().click();
      }
    }
    await page.getByRole('button', { name: /Confirmar dispensación$/ }).click();
    await expect(page.getByText(/Dispensación registrada/).first()).toBeVisible();
    const comprobante = page.getByRole('dialog', { name: 'Comprobante' });
    await expect(comprobante).toBeVisible();
    const numero = (await comprobante.locator('h4').innerText()).trim();
    await comprobante.getByRole('button', { name: 'Listo' }).click();

    await page.goto('/farmacia/historial');
    await expect(page.getByRole('cell', { name: numero, exact: true })).toBeVisible();
    await page.getByRole('button', { name: `Anular ${numero}` }).click();
    await page.locator('.p-confirmdialog').getByRole('button', { name: 'Anular' }).click();
    await expect(page.getByText('La dispensación fue anulada y el stock devuelto.')).toBeVisible();
  });

  test('registra una venta libre sin receta', async ({ page }) => {
    await iniciarSesion(page, 'farmacia');
    await page.goto('/farmacia/dispensar');
    await page.getByRole('button', { name: 'Agregar producto' }).click();
    await page.locator('#disp-producto-0').click();
    await page
      .getByRole('option', { name: /Paracetamol/ })
      .first()
      .click();
    await page.getByRole('button', { name: /Confirmar dispensación$/ }).click();
    await expect(page.getByRole('dialog', { name: 'Comprobante' })).toBeVisible();
    await expect(page.getByText('Cliente general')).toBeVisible();
  });
});

test.describe('laboratorio', () => {
  test('el médico solicita un examen y el laboratorio lo procesa hasta publicarlo', async ({
    browser,
  }) => {
    test.setTimeout(120_000);
    const medico = await (await browser.newContext()).newPage();
    await iniciarSesion(medico, 'medico');
    await medico.goto('/laboratorio/solicitar');
    await medico.locator('#sol-paciente').click();
    await medico.getByRole('option', { name: /Carlos Mendoza/ }).click();
    await medico.locator('p-multiselect').click();
    await medico
      .getByRole('option', { name: /Glucosa/ })
      .first()
      .click();
    await medico.keyboard.press('Escape');
    await medico.getByRole('button', { name: /Enviar al laboratorio$/ }).click();
    await expect(medico).toHaveURL(/\/laboratorio\/ordenes\/[0-9a-f-]{36}$/);
    const ruta = new URL(medico.url()).pathname;
    const titulo = medico.getByRole('heading', { level: 1 });
    await expect(titulo).toHaveText(/LAB-\d+/);
    const codigo = (await titulo.innerText()).trim();

    const lab = await (await browser.newContext()).newPage();
    await iniciarSesion(lab, 'laboratorio');
    await expect(lab.getByText('Por tomar muestra')).toBeVisible();
    await lab.goto(ruta);
    await lab.getByRole('button', { name: /Registrar toma de muestra$/ }).click();
    await expect(lab.getByText('La toma de muestra fue registrada.')).toBeVisible();
    await lab.getByRole('button', { name: /Capturar resultados$/ }).click();
    await lab.locator('input[id^="res-"]').first().fill('150');
    await lab.getByRole('button', { name: /Guardar resultados$/ }).click();
    await expect(lab.getByText('Los resultados fueron guardados.')).toBeVisible();
    await expect(lab.locator('tr.fuera')).toBeVisible();
    await lab.getByRole('button', { name: /Validar y publicar$/ }).click();
    await lab.locator('.p-confirmdialog').getByRole('button', { name: 'Validar' }).click();
    await expect(lab.getByText('Los resultados fueron validados y publicados.')).toBeVisible();

    const paciente = await (await browser.newContext()).newPage();
    await entrarCon(paciente, 'carlos.mendoza@correo.com', 'Paciente123!', /\/citas$/);
    await paciente.goto('/mis-resultados');
    const tarjeta = paciente.locator('article', { hasText: codigo });
    await expect(tarjeta).toBeVisible();
    await tarjeta.getByRole('button', { name: /Ver resultados$/ }).click();
    await expect(paciente.getByRole('dialog').getByText('150')).toBeVisible();
    await expect(paciente.getByRole('dialog').locator('tr.fuera')).toBeVisible();
  });

  test('el catálogo de exámenes y la cola de órdenes cargan para el laboratorio', async ({
    page,
  }) => {
    await iniciarSesion(page, 'laboratorio');
    await expect(page.getByRole('row').nth(1)).toBeVisible();
    await page.goto('/laboratorio/examenes');
    await page.getByPlaceholder('Buscar examen').fill('Hemograma');
    await expect(page.getByRole('cell', { name: /Hemograma/ }).first()).toBeVisible();
  });

  test('registra un examen nuevo con sus parámetros y lo edita', async ({ page }) => {
    const codigo = `EX${Date.now().toString().slice(-7)}`;
    await iniciarSesion(page, 'laboratorio');
    await page.goto('/laboratorio/examenes');
    await page.getByRole('button', { name: 'Nuevo examen' }).click();
    await page.locator('#exa-codigo').fill(codigo);
    await page.locator('#exa-nombre').fill(`Examen ${codigo}`);
    await page.locator('#exa-par-nombre-0').fill('Valor principal');
    await page.getByRole('button', { name: 'Agregar parámetro' }).click();
    await page.locator('#exa-par-nombre-1').fill('Valor secundario');
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText(`Examen ${codigo} fue registrado.`)).toBeVisible();

    await page.getByPlaceholder('Buscar examen').fill(codigo);
    await expect(page.getByText(`${codigo} · 2 parámetros`)).toBeVisible();
    await page.getByRole('button', { name: `Editar Examen ${codigo}` }).click();
    await page.locator('#exa-par-nombre-1').fill('Valor secundario editado');
    await page.getByRole('button', { name: /Guardar$/ }).click();
    await expect(page.getByText('El examen fue actualizado.')).toBeVisible();
  });

  test('el paciente ve sus órdenes con el avance de cada una', async ({ page }) => {
    await entrarCon(page, 'carlos.mendoza@correo.com', 'Paciente123!', /\/citas$/);
    await page.goto('/mis-resultados');
    await expect(page.locator('article').first()).toBeVisible();
    await expect(page.getByText('Completada').first()).toBeVisible();
  });
});
