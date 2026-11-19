import { APIRequestContext, Browser, BrowserContext, Page, expect, test } from '@playwright/test';

import { CUENTAS, ir, iniciarSesion } from './support';

test.use({
  launchOptions: {
    args: ['--use-fake-ui-for-media-stream', '--use-fake-device-for-media-stream'],
  },
  permissions: ['camera', 'microphone'],
});

interface CuentaApi {
  email: string;
  password: string;
}

async function loginApi(request: APIRequestContext, cuenta: CuentaApi): Promise<string> {
  const respuesta = await request.post('/api/v1/auth/login', {
    data: { email: cuenta.email, password: cuenta.password },
  });
  return ((await respuesta.json()) as { token: string }).token;
}

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

function fechaVirtualAleatoria(): string {
  for (;;) {
    const fecha = new Date();
    fecha.setDate(fecha.getDate() + 2 + Math.floor(Math.random() * 24));
    if (fecha.getDay() !== 0 && fecha.getDay() !== 6) {
      const hora = 8 + Math.floor(Math.random() * 5);
      const minutos = Math.random() < 0.5 ? '00' : '30';
      const dia = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
      return `${dia}T${String(hora).padStart(2, '0')}:${minutos}:00`;
    }
  }
}

async function crearTeleconsulta(request: APIRequestContext): Promise<string> {
  const tokenPaciente = await loginApi(request, CUENTAS.paciente);
  const medicos = (await (
    await request.get('/api/v1/medicos', { headers: cabecera(tokenPaciente) })
  ).json()) as { id: string; apellido: string; especialidades: { id: string }[] }[];
  const garcia = medicos.find((m) => m.apellido.includes('García'))!;
  for (let intento = 0; intento < 12; intento++) {
    const respuesta = await request.post('/api/v1/citas', {
      headers: cabecera(tokenPaciente),
      data: {
        medicoId: garcia.id,
        especialidadId: garcia.especialidades[0].id,
        fechaHora: fechaVirtualAleatoria(),
        motivo: 'Teleconsulta de control e2e',
        modalidad: 'VIRTUAL',
      },
    });
    if (respuesta.status() === 201) {
      const cita = (await respuesta.json()) as { id: string; modalidad: string };
      expect(cita.modalidad).toBe('VIRTUAL');
      const mias = (await (
        await request.get('/api/v1/teleconsultas/mias', { headers: cabecera(tokenPaciente) })
      ).json()) as { id: string; citaId: string }[];
      return mias.find((t) => t.citaId === cita.id)!.id;
    }
  }
  throw new Error('No se pudo reservar una teleconsulta libre');
}

async function aceptarConsentimientoApi(request: APIRequestContext): Promise<string> {
  const token = await loginApi(request, CUENTAS.paciente);
  const consentimiento = (await (
    await request.get('/api/v1/teleconsultas/consentimiento', { headers: cabecera(token) })
  ).json()) as { version: string };
  await request.post('/api/v1/teleconsultas/consentimiento', {
    headers: cabecera(token),
    data: { version: consentimiento.version },
  });
  return token;
}

async function nuevaPagina(browser: Browser): Promise<{ contexto: BrowserContext; pagina: Page }> {
  const contexto = await browser.newContext({
    permissions: ['camera', 'microphone'],
    locale: 'es-PE',
  });
  return { contexto, pagina: await contexto.newPage() };
}

function conectar(pagina: Page): Promise<string> {
  return pagina.evaluate(async () => {
    const sesion = JSON.parse(localStorage.getItem('tusalud.session') ?? '{}') as { token: string };
    return sesion.token;
  });
}

test.describe('telemedicina', () => {
  test('el médico y el paciente se conectan por video, chatean y finalizan la teleconsulta', async ({
    browser,
    request,
  }) => {
    test.setTimeout(180_000);
    const id = await crearTeleconsulta(request);
    const paciente = await nuevaPagina(browser);
    const medico = await nuevaPagina(browser);
    try {
      await iniciarSesion(paciente.pagina, 'paciente');
      await expect(paciente.pagina.getByRole('link', { name: 'Teleconsultas' })).toBeVisible();
      await ir(paciente.pagina, '/teleconsultas');
      await expect(
        paciente.pagina.getByRole('heading', { level: 1, name: 'Teleconsultas' }),
      ).toBeVisible();
      await paciente.pagina
        .getByRole('button', { name: /^Ingresar a la teleconsulta con / })
        .first()
        .click();
      await expect(paciente.pagina).toHaveURL(new RegExp(`/teleconsultas/[0-9a-f-]{36}/prueba$`));

      await ir(paciente.pagina, `/teleconsultas/${id}/prueba`);
      const consentimiento = paciente.pagina.locator('#tele-acepto');
      if (await consentimiento.isVisible({ timeout: 4000 }).catch(() => false)) {
        await consentimiento.click();
        await paciente.pagina.getByRole('button', { name: 'Aceptar y continuar' }).click();
      }
      await paciente.pagina.getByRole('button', { name: 'Probar cámara y micrófono' }).click();
      await expect(paciente.pagina.getByRole('button', { name: 'Probar de nuevo' })).toBeVisible();
      await paciente.pagina.getByRole('button', { name: 'Ingresar a la sala de espera' }).click();
      await expect(paciente.pagina.locator('.sala[data-conexion="esperando"]')).toBeVisible({
        timeout: 40_000,
      });
      await expect(paciente.pagina.getByRole('heading', { name: /^Esperando a / })).toBeVisible();

      await iniciarSesion(medico.pagina, 'medico');
      await ir(medico.pagina, `/teleconsultas/${id}/sala`);
      await expect(medico.pagina.getByRole('button', { name: 'Admitir al paciente' })).toBeVisible({
        timeout: 40_000,
      });
      await expect(paciente.pagina.getByText('Estás en la sala de espera')).toBeVisible();

      await medico.pagina.getByRole('button', { name: 'Admitir al paciente' }).click();
      await expect(medico.pagina.locator('.sala[data-conexion="conectado"]')).toBeVisible({
        timeout: 60_000,
      });
      await expect(paciente.pagina.locator('.sala[data-conexion="conectado"]')).toBeVisible({
        timeout: 60_000,
      });

      for (const pagina of [medico.pagina, paciente.pagina]) {
        await expect
          .poll(
            () =>
              pagina.evaluate(() => {
                const video = document.querySelector('#tele-video-remoto') as HTMLVideoElement;
                return video.readyState >= 2 && video.videoWidth > 0;
              }),
            { timeout: 40_000 },
          )
          .toBe(true);
        const antes = await pagina.evaluate(
          () => (document.querySelector('#tele-video-remoto') as HTMLVideoElement).currentTime,
        );
        await expect
          .poll(
            () =>
              pagina.evaluate(
                () =>
                  (document.querySelector('#tele-video-remoto') as HTMLVideoElement).currentTime,
              ),
            { timeout: 10_000 },
          )
          .toBeGreaterThan(antes);
      }

      await paciente.pagina.locator('#tele-chat-texto').fill('Buenos días doctor, ya me escucha');
      await paciente.pagina.locator('#tele-chat-texto').press('Enter');
      await expect(medico.pagina.locator('#tele-chat')).toContainText(
        'Buenos días doctor, ya me escucha',
      );

      await medico.pagina.locator('#tele-chat-texto').fill('Lo escucho bien');
      await medico.pagina.locator('#tele-chat-texto').press('Enter');
      await expect(paciente.pagina.locator('#tele-chat')).toContainText('Lo escucho bien');

      await expect(medico.pagina.getByLabel('Resumen del paciente')).toBeVisible();
      await expect(medico.pagina.getByText('Este acceso queda registrado')).toBeVisible();

      await medico.pagina
        .getByRole('button', { name: 'Finalizar la teleconsulta para ambos' })
        .click();
      await expect(
        medico.pagina.getByRole('heading', { name: 'La teleconsulta terminó' }),
      ).toBeVisible();
      await expect(
        paciente.pagina.getByRole('heading', { name: 'La teleconsulta terminó' }),
      ).toBeVisible();
      await expect(
        medico.pagina.getByRole('button', { name: 'Registrar la atención' }),
      ).toBeVisible();

      const detalle = await request.get(`/api/v1/teleconsultas/${id}`, {
        headers: cabecera(await loginApi(request, CUENTAS.paciente)),
      });
      expect(((await detalle.json()) as { estado: string }).estado).toBe('FINALIZADA');
    } finally {
      await paciente.contexto.close();
      await medico.contexto.close();
    }
  });

  test('un tercero no accede y un ticket solo sirve una vez', async ({ browser, request }) => {
    const id = await crearTeleconsulta(request);
    const tokenPaciente = await aceptarConsentimientoApi(request);
    const tokenOtroPaciente = await loginApi(request, CUENTAS.paciente2);
    const tokenOtroMedico = await loginApi(request, CUENTAS.medico2);
    const tokenAdmin = await loginApi(request, CUENTAS.admin);

    for (const token of [tokenOtroPaciente, tokenOtroMedico]) {
      expect(
        (
          await request.post(`/api/v1/teleconsultas/${id}/ticket`, { headers: cabecera(token) })
        ).status(),
      ).toBe(403);
      expect(
        (await request.get(`/api/v1/teleconsultas/${id}`, { headers: cabecera(token) })).status(),
      ).toBe(403);
      expect(
        (
          await request.get(`/api/v1/teleconsultas/${id}/ice`, { headers: cabecera(token) })
        ).status(),
      ).toBe(403);
    }
    expect(
      (
        await request.get(`/api/v1/teleconsultas/${id}`, { headers: cabecera(tokenAdmin) })
      ).status(),
    ).toBe(403);
    expect((await request.get(`/api/v1/teleconsultas/${id}`)).status()).toBe(401);

    const ticket = (await (
      await request.post(`/api/v1/teleconsultas/${id}/ticket`, { headers: cabecera(tokenPaciente) })
    ).json()) as { ticket: string; expiraEnSegundos: number };
    expect(ticket.expiraEnSegundos).toBe(30);

    const { contexto, pagina } = await nuevaPagina(browser);
    try {
      await iniciarSesion(pagina, 'paciente');
      const intentar = (valor: string) =>
        pagina.evaluate(
          (t) =>
            new Promise<string>((resolver) => {
              const socket = new WebSocket(
                `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/teleconsulta?ticket=${t}`,
              );
              socket.onopen = () => {
                resolver('abierto');
                socket.close();
              };
              socket.onerror = () => resolver('rechazado');
              setTimeout(() => resolver('tiempo'), 8000);
            }),
          valor,
        );
      expect(await intentar(ticket.ticket)).toBe('abierto');
      expect(await intentar(ticket.ticket)).toBe('rechazado');
      expect(await intentar('ticket-inventado')).toBe('rechazado');
      expect(await conectar(pagina)).toBeTruthy();
    } finally {
      await contexto.close();
    }
  });

  test('la teleconsulta aparece en el inicio y el médico la ve en su lista', async ({
    browser,
    request,
  }) => {
    const id = await crearTeleconsulta(request);
    const paciente = await nuevaPagina(browser);
    const medico = await nuevaPagina(browser);
    try {
      await iniciarSesion(paciente.pagina, 'paciente');
      await expect(
        paciente.pagina.getByRole('link', { name: 'Próxima teleconsulta' }),
      ).toBeVisible();

      await iniciarSesion(medico.pagina, 'medico');
      await ir(medico.pagina, '/teleconsultas');
      await expect(
        medico.pagina.getByRole('heading', { level: 1, name: 'Teleconsultas' }),
      ).toBeVisible();
      await expect(
        medico.pagina.getByRole('button', { name: /^Abrir la sala con / }).first(),
      ).toBeVisible();
      expect(id).toBeTruthy();
    } finally {
      await paciente.contexto.close();
      await medico.contexto.close();
    }
  });
});
