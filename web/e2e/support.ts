import { createHmac } from 'node:crypto';

import { Page, expect } from '@playwright/test';

export const CUENTAS = {
  paciente: {
    email: 'juan.perez@correo.com',
    password: 'Paciente123!',
    home: /\/paciente\/citas$/,
  },
  medico: { email: 'dr.garcia@tusalud.com', password: 'Medico123!', home: /\/medico\/agenda$/ },
  admin: { email: 'admin@tusalud.com', password: 'Admin123!', home: /\/admin\/panel$/ },
  laboratorio: { email: 'lab@tusalud.com', password: 'Personal123!', home: /\/laboratorio\/$/ },
  farmacia: { email: 'farmacia@tusalud.com', password: 'Personal123!', home: /\/farmacia\/$/ },
  almacen: { email: 'almacen@tusalud.com', password: 'Personal123!', home: /\/almacen\/$/ },
  recepcion: { email: 'recepcion@tusalud.com', password: 'Personal123!', home: /\/recepcion\/$/ },
  caja: { email: 'caja@tusalud.com', password: 'Personal123!', home: /\/caja\/$/ },
  gerencia: {
    email: 'gerencia@tusalud.com',
    password: 'Personal123!',
    home: /\/gerencia\/reportes\/ejecutivo$/,
  },
  finanzas: {
    email: 'finanzas@tusalud.com',
    password: 'Personal123!',
    home: /\/finanzas\/reportes\/financiero$/,
  },
  operaciones: {
    email: 'operaciones@tusalud.com',
    password: 'Personal123!',
    home: /\/operaciones\/reportes\/operativo$/,
  },
  calidad: {
    email: 'calidad@tusalud.com',
    password: 'Personal123!',
    home: /\/calidad\/reportes\/calidad$/,
  },
  rrhh: { email: 'rrhh@tusalud.com', password: 'Personal123!', home: /\/rrhh\/$/ },
  enfermeria: {
    email: 'enfermeria@tusalud.com',
    password: 'Personal123!',
    home: /\/enfermeria\/$/,
  },
} as const;

const PREFIJO_OCULTO: Readonly<Record<string, string>> = {
  admin: 'admin',
  recepcion: 'recepcion',
  caja: 'caja',
  rrhh: 'rrhh',
  enfermeria: 'hospitalizacion',
  laboratorio: 'laboratorio',
  farmacia: 'farmacia',
  almacen: 'almacen',
};

const APPS = [
  'paciente',
  'medico',
  'admin',
  'gerencia',
  'finanzas',
  'operaciones',
  'calidad',
  'rrhh',
  'recepcion',
  'caja',
  'enfermeria',
  'laboratorio',
  'farmacia',
  'almacen',
];

export function appActual(page: Page): string {
  const primero = new URL(page.url()).pathname.split('/').filter(Boolean)[0] ?? '';
  if (!APPS.includes(primero)) {
    throw new Error(`La página no está dentro de una aplicación: ${page.url()}`);
  }
  return primero;
}

export function urlDeModulo(app: string, interna: string): string {
  const oculto = PREFIJO_OCULTO[app];
  const corte = interna.search(/[?#]/);
  const ruta = corte < 0 ? interna : interna.slice(0, corte);
  const resto = corte < 0 ? '' : interna.slice(corte);
  let visible = ruta;
  if (oculto && (ruta === `/${oculto}` || ruta.startsWith(`/${oculto}/`))) {
    visible = ruta.slice(oculto.length + 1);
  }
  return `/${app}${visible === '' ? '/' : visible}${resto}`;
}

export async function ir(page: Page, interna: string): Promise<void> {
  await page.goto(urlDeModulo(appActual(page), interna));
}

export async function omitirGuia(page: Page): Promise<void> {
  await page.route('**/api/v1/perfil/preferencias', async (route) => {
    if (route.request().method() !== 'GET') {
      await route.continue();
      return;
    }
    try {
      const response = await route.fetch();
      const json = await response.json();
      await route.fulfill({ response, json: { ...json, tourCompletado: true } });
    } catch {
      await route.abort();
    }
  });
}

export async function iniciarSesion(page: Page, rol: keyof typeof CUENTAS): Promise<void> {
  const cuenta = CUENTAS[rol];
  await omitirGuia(page);
  await page.goto('/auth/login');
  await page.getByLabel('Email').fill(cuenta.email);
  await page.locator('#login-password').fill(cuenta.password);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await expect(page).toHaveURL(cuenta.home);
}

export async function reservarPrimeraHoraLibre(page: Page, motivo: string): Promise<void> {
  const huecos = page.getByRole('button', { name: /^Elegir / });
  const semanas = 1 + Math.floor(Math.random() * 12);
  await expect(page.getByRole('button', { name: 'Siguiente', exact: true })).toBeVisible();
  for (let i = 0; i < semanas; i++) {
    await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
  }
  for (let intento = 0; intento < 20; intento++) {
    if (!(await huecos.count())) {
      await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
      await page.waitForTimeout(400);
      continue;
    }
    await huecos.nth(Math.floor(Math.random() * (await huecos.count()))).click();
    await page.locator('#cita-motivo').fill(motivo);
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();
    try {
      await page.waitForURL(/\/citas\/[0-9a-f-]{36}$/, { timeout: 4000 });
      return;
    } catch {
      await page.waitForTimeout(400);
    }
  }
  throw new Error('No quedaron horas libres para reservar');
}

export async function cerrarSesion(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Menú de usuario' }).click();
  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
}

const BASE32 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

function decodificarBase32(texto: string): Buffer {
  let bits = '';
  for (const caracter of texto.replace(/=+$/, '').toUpperCase()) {
    bits += BASE32.indexOf(caracter).toString(2).padStart(5, '0');
  }
  const bytes: number[] = [];
  for (let i = 0; i + 8 <= bits.length; i += 8) {
    bytes.push(parseInt(bits.slice(i, i + 8), 2));
  }
  return Buffer.from(bytes);
}

export function codigoTotp(secreto: string, desplazamiento = 0): string {
  const contador = Math.floor(Date.now() / 30_000) + desplazamiento;
  const mensaje = Buffer.alloc(8);
  mensaje.writeBigUInt64BE(BigInt(contador));
  const hash = createHmac('sha1', decodificarBase32(secreto)).update(mensaje).digest();
  const inicio = hash[hash.length - 1] & 0x0f;
  const numero = hash.readUInt32BE(inicio) & 0x7fffffff;
  return String(numero % 1_000_000).padStart(6, '0');
}

export async function registrarPaciente(page: Page, etiqueta: string) {
  const sufijo = Date.now().toString();
  const cuenta = {
    email: `${etiqueta}.${sufijo}@correo.com`,
    password: 'Paciente123!',
  };
  await page.goto('/auth/registro');
  await page.locator('#reg-nombre').fill('Prueba');
  await page.locator('#reg-apellido').fill(etiqueta);
  await page.locator('#reg-dni').fill(sufijo.slice(-8));
  await page.locator('#reg-nacimiento').pressSequentially('15/05/1990');
  await page.keyboard.press('Escape');
  await page.locator('#reg-email').fill(cuenta.email);
  await page.locator('#reg-password').fill(cuenta.password);
  await page.getByRole('button', { name: 'Crear cuenta' }).click();
  await expect(page).toHaveURL(/\/citas$/);
  return cuenta;
}

export async function idDeMedico(page: Page, indice: number): Promise<string> {
  const token = await page.evaluate(
    () => JSON.parse(localStorage.getItem('tusalud.session') ?? '{}').token as string,
  );
  const respuesta = await page.request.get('/api/v1/medicos', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const medicos = (await respuesta.json()) as { id: string }[];
  return medicos[indice].id;
}

export async function idDeGarcia(page: Page): Promise<string> {
  const token = await page.evaluate(
    () => JSON.parse(localStorage.getItem('tusalud.session') ?? '{}').token as string,
  );
  const respuesta = await page.request.get('/api/v1/medicos', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const medicos = (await respuesta.json()) as { id: string; apellido: string }[];
  return medicos.find((m) => m.apellido === 'García')!.id;
}

export async function tokenDe(page: Page, rol: keyof typeof CUENTAS): Promise<string> {
  const cuenta = CUENTAS[rol];
  const respuesta = await page.request.post('/api/v1/auth/login', {
    data: { email: cuenta.email, password: cuenta.password },
  });
  const { token } = (await respuesta.json()) as { token: string };
  return token;
}
