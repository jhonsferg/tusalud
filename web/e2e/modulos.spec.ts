import { expect, test } from '@playwright/test';

import { iniciarSesion, omitirGuia, reservarPrimeraHoraLibre, idDeGarcia, ir } from './support';

test.describe('pagos del paciente', () => {
  test('agrega y elimina una tarjeta', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/pagos/metodos');
    await expect(page.getByText('•••• •••• •••• 4242')).toBeVisible();

    await page.getByRole('button', { name: 'Agregar tarjeta' }).first().click();
    await page.locator('#mp-titular').fill('Juan Perez');
    await page.locator('#mp-numero').fill('5555555555554444');
    await page.locator('#mp-mes').click();
    await page.getByRole('option', { name: '12', exact: true }).click();
    await page.locator('#mp-anio').click();
    await page.getByRole('option').last().click();
    await page.locator('#mp-cvv').fill('123');
    await page.getByRole('button', { name: 'Guardar tarjeta' }).click();
    await expect(page.getByText('Tu método de pago fue agregado.')).toBeVisible();
    await expect(page.getByText('•••• •••• •••• 4444')).toBeVisible();

    await page.getByRole('button', { name: 'Eliminar tarjeta terminada en 4444' }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByText('•••• •••• •••• 4444')).toHaveCount(0);
  });

  test('valida el número de tarjeta', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/pagos/metodos');
    await page.getByRole('button', { name: 'Agregar tarjeta' }).first().click();
    await page.locator('#mp-numero').fill('1234567812345678');
    await page.getByRole('button', { name: 'Guardar tarjeta' }).click();
    await expect(page.getByText('El número de tarjeta no es válido.')).toBeVisible();
  });

  test('reserva, paga con cupón y ve el comprobante', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
    await reservarPrimeraHoraLibre(page, 'Pago con cupón e2e');

    await page.getByRole('button', { name: 'Pagar cita' }).click();
    await page.locator('#pago-cupon').fill('SALUD20');
    await page.getByRole('button', { name: 'Aplicar' }).click();
    await expect(page.getByText(/cupón/i).first()).toBeVisible();
    await expect(page.getByText('- S/ 30.00')).toBeVisible();
    await page.getByRole('button', { name: 'Pagar ahora' }).click();
    await expect(page.getByText(/Pago registrado/).first()).toBeVisible();
    await expect(page.getByText('Pagada', { exact: true }).first()).toBeVisible();

    await ir(page, '/pagos');
    await expect(page.getByRole('cell', { name: /B001-/ }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Ver comprobante' }).first().click();
    await expect(page.getByText('Comprobante de pago')).toBeVisible();
  });
});

test.describe('billeteras digitales', () => {
  for (const [billetera, nombre] of [
    ['Yape', 'YAPE'],
    ['Plin', 'PLIN'],
  ] as const) {
    test(`paga con ${billetera} escaneando un QR y confirmando desde la app`, async ({ page }) => {
      await iniciarSesion(page, 'paciente');
      await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
      await reservarPrimeraHoraLibre(page, `Pago con ${nombre} e2e`);

      await page.getByRole('button', { name: 'Pagar cita' }).click();
      await page.getByText(billetera, { exact: true }).click();
      await page.getByRole('button', { name: 'Generar código QR' }).click();
      await expect(page.getByAltText('Código QR de pago')).toBeVisible();
      await expect(page.getByText(/Vence en/)).toBeVisible();
      await page.getByRole('button', { name: 'Simular pago desde la app' }).click();
      await expect(page.getByText(/Pago registrado/).first()).toBeVisible();
      await expect(page.getByText('Pagada', { exact: true }).first()).toBeVisible();
    });
  }

  test('cancela un QR pendiente al cambiar de billetera', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
    await reservarPrimeraHoraLibre(page, 'Cambio de billetera e2e');

    await page.getByRole('button', { name: 'Pagar cita' }).click();
    await page.getByText('Yape', { exact: true }).click();
    await page.getByRole('button', { name: 'Generar código QR' }).click();
    await expect(page.getByAltText('Código QR de pago')).toBeVisible();
    await page.getByText('Plin', { exact: true }).click();
    await expect(page.getByRole('button', { name: 'Generar código QR' })).toBeVisible();
  });
});

test.describe('historia clínica', () => {
  test('el paciente ve su historia sin poder editarla y con el aviso para actualizarla', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('link', { name: 'Historia clínica', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Juan Pérez' })).toBeVisible();
    await expect(page.getByText('Penicilina')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Editar datos médicos' })).toHaveCount(0);
    await expect(page.getByText(/acércate al centro de salud/)).toBeVisible();
  });

  test('el médico ve sus pacientes y abre una historia', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await page.getByRole('link', { name: 'Mis pacientes', exact: true }).click();
    await expect(page.getByRole('cell', { name: /Carlos Mendoza/ }).first()).toBeVisible();
    await page
      .getByRole('row', { name: /Carlos Mendoza/ })
      .getByRole('button', { name: 'Historia' })
      .click();
    await expect(page.getByRole('heading', { name: 'Carlos Mendoza' })).toBeVisible();
    await expect(page.getByText('Hipertensión arterial controlada')).toBeVisible();
  });
});

test.describe('notificaciones y perfil', () => {
  test('la bandeja lista avisos y permite marcarlos como leídos', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/notificaciones');
    await expect(page.getByRole('heading', { name: 'Notificaciones' })).toBeVisible();
    await expect(page.getByText('Nuevo inicio de sesión').first()).toBeVisible();
    await page.getByRole('button', { name: 'Marcar todas como leídas' }).click();
    await expect(page.getByText('0 sin leer')).toBeVisible();
  });

  test('el panel de la campana abre y muestra el estado en vivo', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Abrir notificaciones' }).click();
    await expect(page.getByText('En vivo')).toBeVisible({ timeout: 10_000 });
  });

  test('lista las sesiones activas y marca la actual', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/perfil?tab=seguridad');
    await expect(page.getByText('Sesiones activas')).toBeVisible();
    await expect(page.getByText('Este dispositivo')).toBeVisible();
  });

  test('cambia el modo oscuro y el color principal', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Cambiar a modo oscuro' }).first().click();
    await expect(page.locator('html')).toHaveClass(/app-dark/);
    await ir(page, '/perfil?tab=apariencia');
    await page.getByRole('radio', { name: 'Violeta' }).click();
    await expect(page.getByRole('radio', { name: 'Violeta' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });
});

async function restaurarTarifa(page: import('@playwright/test').Page): Promise<void> {
  const token = await page.evaluate(
    () => JSON.parse(localStorage.getItem('tusalud.session') ?? '{}').token as string,
  );
  const headers = { Authorization: `Bearer ${token}` };
  const lista = (await (await page.request.get('/api/v1/especialidades', { headers })).json()) as {
    id: number;
    nombre: string;
    descripcion: string;
  }[];
  const oftalmologia = lista.find((item) => item.nombre === 'Oftalmología')!;
  const respuesta = await page.request.put(`/api/v1/especialidades/${oftalmologia.id}`, {
    headers,
    data: {
      nombre: oftalmologia.nombre,
      descripcion: oftalmologia.descripcion,
      precioConsulta: 110,
    },
  });
  expect(respuesta.ok()).toBe(true);
}

test.describe('especialidades y roles', () => {
  test('el administrador cambia la tarifa de una especialidad y el paciente la ve al reservar', async ({
    browser,
  }) => {
    const admin = await (await browser.newContext()).newPage();
    await iniciarSesion(admin, 'admin');
    await ir(admin, '/especialidades');
    await expect(admin.getByRole('columnheader', { name: 'Consulta' })).toBeVisible();
    await admin.getByRole('button', { name: 'Editar Oftalmología' }).click();
    await expect(admin.locator('#esp-precio')).not.toHaveValue('');
    await admin.locator('#esp-precio').click();
    await admin.keyboard.press('Control+a');
    await admin.locator('#esp-precio').pressSequentially('115', { delay: 60 });
    await admin.getByRole('button', { name: 'Guardar cambios' }).click();
    await expect(
      admin.getByRole('row', { name: /Oftalmología/ }).getByText(/115\.00/),
    ).toBeVisible();

    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    await ir(paciente, '/citas/nueva');
    const opcion = paciente.getByRole('option', { name: 'Oftalmología' });
    await expect(async () => {
      await paciente.locator('#cita-especialidad').click();
      await expect(opcion).toBeVisible({ timeout: 1500 });
    }).toPass();
    await opcion.click();
    await paciente.locator('#cita-medico').click();
    await paciente.getByRole('option', { name: /Marcos Delgado/ }).click();
    await expect(paciente.locator('.summary').getByText(/115.00/)).toBeVisible();

    await restaurarTarifa(admin);
  });

  test('el administrador edita las especialidades de un médico', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/medicos');
    await page
      .locator('article', { hasText: 'Luis García' })
      .getByRole('button', { name: 'Ver perfil' })
      .click();
    await expect(page.getByText('Especialidades y tarifas')).toBeVisible();
    await page.getByRole('button', { name: 'Editar especialidades' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar' }).click();
    await expect(
      page.getByText('Las especialidades del médico fueron actualizadas.'),
    ).toBeVisible();
  });

  test('el administrador gestiona los roles de un usuario', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/usuarios');
    await page.getByPlaceholder('Buscar por nombre o correo').fill('valeria.castillo');
    await page.getByRole('button', { name: /Gestionar roles de Valeria/ }).click();
    const dialogo = page.getByRole('dialog');
    await dialogo.getByLabel(/Administrador/).check();
    await dialogo.getByRole('button', { name: 'Guardar roles' }).click();
    await expect(page.getByText('Los roles del usuario fueron actualizados.')).toBeVisible();
    await expect(
      page.getByRole('row', { name: /Valeria/ }).getByText('Administrador'),
    ).toBeVisible();

    await page.getByRole('button', { name: /Gestionar roles de Valeria/ }).click();
    await page
      .getByRole('dialog')
      .getByLabel(/Administrador/)
      .uncheck();
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar roles' }).click();
    await expect(page.getByText('Los roles del usuario fueron actualizados.')).toBeVisible();
  });
});

test.describe('pacientes compartidos', () => {
  test('un médico comparte un paciente con otro colega y deja de compartirlo', async ({
    browser,
  }) => {
    test.setTimeout(90_000);
    const garcia = await (await browser.newContext()).newPage();
    await iniciarSesion(garcia, 'medico');
    await ir(garcia, '/pacientes');
    await garcia.getByRole('button', { name: 'Compartir a Diego Herrera' }).click();
    const previo = garcia.getByRole('button', { name: 'Dejar de compartir con Ricardo Salazar' });
    if (await previo.count()) {
      await previo.click();
      await expect(previo).toHaveCount(0);
    }
    await garcia.locator('#compartir-colega').click();
    await garcia.getByRole('option', { name: /Ricardo Salazar/ }).click();
    await garcia.locator('#compartir-nota').fill('Revisión conjunta por cuadro respiratorio');
    await garcia
      .getByRole('dialog')
      .getByRole('button', { name: /Compartir$/ })
      .click();
    await expect(garcia.getByText(/fue compartido con Ricardo Salazar/)).toBeVisible();
    await expect(
      garcia.getByRole('button', { name: 'Dejar de compartir con Ricardo Salazar' }),
    ).toBeVisible();

    const salazar = await (await browser.newContext()).newPage();
    await omitirGuia(salazar);
    await salazar.goto('/auth/login');
    await salazar.getByLabel('Email').fill('dr.salazar@tusalud.com');
    await salazar.locator('#login-password').fill('Medico123!');
    await salazar.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(salazar).toHaveURL(/\/medico\/inicio$/);
    await ir(salazar, '/pacientes');
    await expect(
      salazar.getByRole('row', { name: /Diego Herrera/ }).getByText(/Compartido por/),
    ).toBeVisible();

    await garcia.getByRole('button', { name: 'Dejar de compartir con Ricardo Salazar' }).click();
    await expect(garcia.getByText('Se dejó de compartir el paciente.')).toBeVisible();
  });
});

test.describe('administración', () => {
  test('muestra el panel de control con indicadores', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/panel');
    await expect(page.getByRole('heading', { name: 'Panel de control' })).toBeVisible();
    await expect(page.getByText('Ingresos del mes')).toBeVisible();
    await expect(page.getByText('Médicos con más citas atendidas')).toBeVisible();
  });

  test('crea y elimina un cupón', async ({ page }) => {
    const codigo = `E2E${Date.now().toString().slice(-6)}`;
    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/cupones');
    await expect(page.getByRole('cell', { name: /BIENVENIDA10/ }).first()).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo cupón' }).first().click();
    await page.locator('#cu-codigo').fill(codigo);
    await page.locator('#cu-valor').pressSequentially('15');
    await page.getByRole('button', { name: 'Guardar cupón' }).click();
    await expect(page.getByText('El cupón fue guardado.')).toBeVisible();
    await expect(page.getByRole('cell', { name: new RegExp(codigo) }).first()).toBeVisible();

    await page.getByRole('button', { name: `Eliminar ${codigo}` }).click();
    await page.getByRole('button', { name: 'Eliminar', exact: true }).click();
    await expect(page.getByRole('button', { name: `Eliminar ${codigo}` })).toHaveCount(0);
  });

  test('lista pagos y gestiona usuarios', async ({ page }) => {
    await iniciarSesion(page, 'admin');
    await ir(page, '/admin/pagos');
    await expect(page.getByRole('cell', { name: /B001-/ }).first()).toBeVisible();

    await ir(page, '/admin/usuarios');
    await page.getByPlaceholder('Buscar por nombre o correo').fill('ana.paredes');
    const fila = page.getByRole('row', { name: /Ana Lucía Paredes/ });
    await expect(fila).toBeVisible();
    const reactivar = fila.getByRole('button', { name: 'Activar', exact: true });
    if (await reactivar.count()) {
      await reactivar.click();
      await page
        .locator('.p-confirmdialog')
        .getByRole('button', { name: 'Activar', exact: true })
        .click();
      await expect(page.getByText('El usuario fue activado.')).toBeVisible();
    }
    await fila.getByRole('button', { name: 'Desactivar' }).click();
    await page
      .locator('.p-confirmdialog')
      .getByRole('button', { name: 'Desactivar', exact: true })
      .click();
    await expect(page.getByText('El usuario fue desactivado.')).toBeVisible();
    await fila.getByRole('button', { name: 'Activar' }).click();
    await page
      .locator('.p-confirmdialog')
      .getByRole('button', { name: 'Activar', exact: true })
      .click();
    await expect(page.getByText('El usuario fue activado.')).toBeVisible();
  });
});
