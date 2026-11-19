import { Page, expect, test } from '@playwright/test';

import { CUENTAS, iniciarSesion, ir, omitirGuia, registrarPaciente } from './support';

const MAILPIT = process.env['E2E_MAILPIT_URL'] ?? 'http://localhost:8025';

interface MensajeMailpit {
  ID: string;
  Subject: string;
  Snippet: string;
}

async function esperarCorreo(page: Page, email: string, asunto: string): Promise<MensajeMailpit> {
  const consulta = encodeURIComponent(`to:${email} subject:"${asunto}"`);
  for (let intento = 0; intento < 40; intento++) {
    const respuesta = await page.request.get(`${MAILPIT}/api/v1/search?query=${consulta}`);
    if (respuesta.ok()) {
      const { messages } = (await respuesta.json()) as { messages: MensajeMailpit[] };
      if (messages.length > 0) {
        return messages[0];
      }
    }
    await page.waitForTimeout(1000);
  }
  throw new Error(`No llegó el correo "${asunto}" a ${email}`);
}

async function codigoDeCorreo(page: Page, email: string): Promise<string> {
  const mensaje = await esperarCorreo(page, email, 'Su código de verificación');
  const codigo = /(\d{6})/.exec(mensaje.Snippet);
  expect(codigo, 'el correo trae un código de 6 dígitos').not.toBeNull();
  return codigo![1];
}

async function tokenDelPaciente(
  page: Page,
  cuenta: { email: string; password: string },
): Promise<string> {
  const respuesta = await page.request.post('/api/v1/auth/login', { data: cuenta });
  return ((await respuesta.json()) as { token: string }).token;
}

async function activarCorreoPorApi(
  page: Page,
  cuenta: { email: string; password: string },
): Promise<string> {
  const token = await tokenDelPaciente(page, cuenta);
  const cabeceras = { Authorization: `Bearer ${token}` };
  const solicitud = await page.request.post('/api/v1/canales/contactos/CORREO/solicitar', {
    headers: cabeceras,
    data: { consentimiento: true },
  });
  expect(solicitud.status()).toBe(200);
  const codigo = await codigoDeCorreo(page, cuenta.email);
  const verificacion = await page.request.post('/api/v1/canales/contactos/CORREO/verificar', {
    headers: cabeceras,
    data: { codigo },
  });
  expect(verificacion.status()).toBe(200);
  return token;
}

async function abrirAvisos(page: Page): Promise<void> {
  await ir(page, '/perfil');
  await page.getByRole('tab', { name: /Avisos/ }).click();
  await expect(
    page.getByRole('heading', { name: 'Avisos por correo, SMS y WhatsApp' }),
  ).toBeVisible();
}

test.describe('canales de notificación', () => {
  test('el paciente activa el correo con consentimiento y un código recibido en Mailpit', async ({
    page,
  }) => {
    const cuenta = await registrarPaciente(page, 'canal');
    await abrirAvisos(page);

    const tarjeta = page.locator('article[data-canal="CORREO"]');
    await expect(tarjeta.getByTestId('estado-canal')).toHaveText('Sin activar');
    await expect(page.getByText(/nunca incluyen diagnósticos/)).toBeVisible();

    await page.getByRole('button', { name: 'Activar Correo electrónico' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Activar Correo electrónico' });
    await dialogo.getByRole('button', { name: 'Enviar código' }).click();
    await expect(dialogo.getByText(/debes aceptar recibir avisos/)).toBeVisible();

    await dialogo.getByText(/Acepto recibir avisos de TuSalud/).click();
    await dialogo.getByRole('button', { name: 'Enviar código' }).click();
    await expect(dialogo.getByText(/Te enviamos un código de 6 dígitos/)).toBeVisible();

    await dialogo.locator('#canal-codigo').fill('12ab');
    await dialogo.getByRole('button', { name: 'Verificar' }).click();
    await expect(dialogo.getByText('El código tiene 6 dígitos.')).toBeVisible();

    const codigo = await codigoDeCorreo(page, cuenta.email);
    await dialogo.locator('#canal-codigo').fill(codigo === '000000' ? '111111' : '000000');
    await dialogo.getByRole('button', { name: 'Verificar' }).click();
    await expect(dialogo.getByText('El código no es correcto')).toBeVisible();

    await dialogo.locator('#canal-codigo').fill(codigo);
    await dialogo.getByRole('button', { name: 'Verificar' }).click();
    await expect(dialogo).toBeHidden();
    await expect(tarjeta.getByTestId('estado-canal')).toHaveText('Activo');
    await expect(
      tarjeta.getByRole('button', { name: 'Dar de baja Correo electrónico' }),
    ).toBeVisible();
  });

  test('las preferencias por tipo se guardan y las alertas de seguridad no se pueden apagar', async ({
    page,
  }) => {
    const cuenta = await registrarPaciente(page, 'prefs');
    await activarCorreoPorApi(page, cuenta);
    await abrirAvisos(page);

    const cita = page.getByRole('switch', { name: 'Cita registrada por Correo electrónico' });
    await expect(cita).toBeChecked();
    const seguridad = page.getByRole('switch', {
      name: 'Actividad inusual por Correo electrónico',
    });
    await expect(seguridad).toBeChecked();
    await expect(seguridad).toBeDisabled();
    await expect(page.getByRole('switch', { name: 'Cita registrada por WhatsApp' })).toBeDisabled();

    const guardar = page.getByRole('button', { name: 'Guardar avisos por canal' });
    await expect(guardar).toBeDisabled();
    await cita.click();
    await expect(guardar).toBeEnabled();
    await page.locator('#silencio-desde').fill('21:30');
    await guardar.click();
    await expect(page.getByText('Guardamos tus preferencias de avisos.')).toBeVisible();

    await page.reload();
    await page.getByRole('tab', { name: /Avisos/ }).click();
    await expect(
      page.getByRole('switch', { name: 'Cita registrada por Correo electrónico' }),
    ).not.toBeChecked();
    await expect(page.locator('#silencio-desde')).toHaveValue('21:30');
  });

  test('el paciente da de baja un canal desde su perfil', async ({ page }) => {
    const cuenta = await registrarPaciente(page, 'baja');
    await activarCorreoPorApi(page, cuenta);
    await abrirAvisos(page);

    const tarjeta = page.locator('article[data-canal="CORREO"]');
    await expect(tarjeta.getByTestId('estado-canal')).toHaveText('Activo');
    await tarjeta.getByRole('button', { name: 'Dar de baja Correo electrónico' }).click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'Dar de baja' }).click();
    await expect(tarjeta.getByTestId('estado-canal')).toHaveText('Dado de baja');
    await expect(tarjeta.getByRole('button', { name: 'Activar Correo electrónico' })).toBeVisible();
  });

  test('el enlace de baja del correo funciona sin iniciar sesión', async ({ page, browser }) => {
    const cuenta = await registrarPaciente(page, 'enlace');
    const token = await activarCorreoPorApi(page, cuenta);
    const cambio = await page.request.put('/api/v1/perfil/password', {
      headers: { Authorization: `Bearer ${token}` },
      data: { passwordActual: cuenta.password, passwordNueva: 'Paciente456!' },
    });
    expect(cambio.status()).toBe(204);

    const aviso = await esperarCorreo(page, cuenta.email, 'Se cambió su contraseña');
    const texto = await page.request.get(`${MAILPIT}/api/v1/message/${aviso.ID}`);
    const cuerpo = (await texto.json()) as { Text: string; HTML: string };
    expect(cuerpo.Text).toContain('Hola Prueba');
    expect(cuerpo.Text).not.toContain('Paciente456');
    expect(cuerpo.HTML).toContain('Dejar de recibir estos correos');
    const enlace = /baja\?t=([A-Za-z0-9_.-]+)/.exec(cuerpo.Text);
    expect(enlace, 'el correo trae el enlace de baja').not.toBeNull();

    const anonimo = await browser.newContext({
      baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
      locale: 'es-PE',
    });
    const publica = await anonimo.newPage();
    await publica.goto(`/baja?t=${enlace![1]}`);
    await expect(publica.getByRole('heading', { name: 'Dejar de recibir avisos' })).toBeVisible();
    await publica.getByRole('button', { name: 'Confirmar baja' }).click();
    await expect(publica.getByRole('status')).toContainText(
      'Ya no recibirás avisos por Correo electrónico',
    );
    await anonimo.close();

    const mis = await page.request.get('/api/v1/canales/mis-canales', {
      headers: { Authorization: `Bearer ${token}` },
    });
    const canales = ((await mis.json()) as { canales: { canal: string; estado: string }[] })
      .canales;
    expect(canales.find((c) => c.canal === 'CORREO')?.estado).toBe('BAJA');
  });

  test('un enlace de baja alterado muestra un error claro', async ({ page }) => {
    await page.goto('/baja?t=abc.def');
    await page.getByRole('button', { name: 'Confirmar baja' }).click();
    await expect(page.locator('p.error[role="alert"]')).toContainText('no es válido');
  });

  test('el administrador edita una plantilla, valida las variables y la restaura', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await page.getByRole('link', { name: 'Plantillas de mensajes' }).click();
    await expect(
      page.getByRole('heading', { level: 1, name: 'Plantillas de mensajes' }),
    ).toBeVisible();

    await page
      .getByRole('button', { name: 'Editar la plantilla Cita registrada de Correo electrónico' })
      .click();
    const dialogo = page.getByRole('dialog', { name: /Editar: Cita registrada/ });
    await dialogo.locator('#pl-asunto').fill('Su cita quedó registrada en {{centro}}');
    await dialogo.locator('#pl-cuerpo').fill('Hola {{nombre}}, su diagnóstico es {{diagnostico}}.');
    await dialogo.locator('#pl-motivo').fill('Prueba de validación');
    await dialogo.getByRole('button', { name: 'Publicar versión' }).click();
    await expect(dialogo.getByText(/Variables no permitidas: diagnostico/)).toBeVisible();

    await dialogo.locator('#pl-cuerpo').fill('Hola {{nombre}}, ingrese a {{enlace}}.');
    await expect(dialogo.getByText('Hola María, ingrese a https://tusalud.example.')).toBeVisible();
    await dialogo.getByRole('button', { name: 'Publicar versión' }).click();
    await expect(page.getByText(/Publicamos la versión \d+ de la plantilla/)).toBeVisible();

    const fila = page.getByRole('row', { name: /Cita registrada.*Correo electrónico/ });
    await expect(fila.getByText('Personalizada')).toBeVisible();
    await page
      .getByRole('button', { name: 'Ver el historial de Cita registrada de Correo electrónico' })
      .click();
    await expect(page.getByRole('dialog', { name: 'Historial de versiones' })).toContainText(
      'Prueba de validación',
    );
    await page.keyboard.press('Escape');

    await page
      .getByRole('button', { name: 'Restaurar Cita registrada de Correo electrónico' })
      .click();
    await page.getByRole('alertdialog').getByRole('button', { name: 'Restaurar' }).click();
    await expect(page.getByText('Se restauró el texto predeterminado.')).toBeVisible();
    await expect(fila.getByText('Predeterminada').first()).toBeVisible();
  });

  test('administración y operaciones consultan las entregas pero solo administración reintenta', async ({
    page,
  }) => {
    await iniciarSesion(page, 'operaciones');
    await page.getByRole('link', { name: 'Entregas de avisos' }).click();
    await expect(
      page.getByRole('heading', { level: 1, name: 'Entregas de notificaciones' }),
    ).toBeVisible();
    await expect(page.locator('article.kpi')).toHaveCount(3);
    await expect(page.getByText('Proveedores:')).toHaveCount(0);
    await expect(page.getByRole('button', { name: /^Reintentar/ })).toHaveCount(0);

    await ir(page, '/canales/plantillas');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Plantillas de mensajes' }),
    ).toHaveCount(0);
  });

  test('el administrador ve la bitácora con los proveedores y filtra por canal', async ({
    page,
  }) => {
    await iniciarSesion(page, 'admin');
    await page.getByRole('link', { name: 'Entregas de avisos' }).click();
    await expect(
      page.getByRole('heading', { level: 1, name: 'Entregas de notificaciones' }),
    ).toBeVisible();
    await expect(page.getByText('Proveedores:')).toBeVisible();
    await expect(page.getByText('Correo electrónico: SMTP')).toBeVisible();
    await page.locator('p-select[inputid="en-canal"]').click();
    await page.getByRole('option', { name: 'Mensaje de texto' }).click();
    await expect(page.locator('p-select[inputid="en-canal"]')).toContainText('Mensaje de texto');
  });

  test('operaciones y administración ven el estado de los respaldos y el paciente no', async ({
    page,
    browser,
  }) => {
    await iniciarSesion(page, 'operaciones');
    await page.getByRole('link', { name: 'Respaldos' }).first().click();
    await expect(
      page.getByRole('heading', { level: 1, name: 'Respaldos y recuperación' }),
    ).toBeVisible();
    await expect(page.getByTestId('semaforo')).toBeVisible();
    await expect(page.getByText(/RPO 24 h · RTO 60 min/)).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Plan de recuperación' })).toBeVisible();

    const paciente = await browser.newContext({
      baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
      locale: 'es-PE',
    });
    const pagina = await paciente.newPage();
    await omitirGuia(pagina);
    await pagina.goto('/auth/login');
    await pagina.getByLabel('Email').fill(CUENTAS.paciente.email);
    await pagina.locator('#login-password').fill(CUENTAS.paciente.password);
    await pagina.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(pagina).toHaveURL(CUENTAS.paciente.home);
    await expect(pagina.getByRole('link', { name: 'Respaldos' })).toHaveCount(0);
    await paciente.close();
  });
});
