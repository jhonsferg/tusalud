import { createHmac } from 'node:crypto';

import { Page, expect } from '@playwright/test';

export const CUENTAS = {
  paciente: { email: 'juan.perez@correo.com', password: 'Paciente123!', home: /\/citas$/ },
  medico: { email: 'dr.garcia@tusalud.com', password: 'Medico123!', home: /\/agenda$/ },
  admin: { email: 'admin@tusalud.com', password: 'Admin123!', home: /\/admin\/panel$/ },
} as const;

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

export function proximoDiaHabil(): Date {
  const fecha = new Date();
  do {
    fecha.setDate(fecha.getDate() + 1);
  } while (fecha.getDay() === 0 || fecha.getDay() === 6);
  return fecha;
}

export function formatoFecha(fecha: Date): string {
  const dia = String(fecha.getDate()).padStart(2, '0');
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  return [dia, mes, fecha.getFullYear()].join('/');
}

export async function reservarPrimeraHoraLibre(page: Page, motivo: string): Promise<void> {
  const horas = page.getByRole('radio');
  await expect(horas.first()).toBeVisible();
  const total = await horas.count();
  for (let intento = 0; intento < total; intento++) {
    await horas.nth(intento).click();
    await page.locator('#cita-motivo').fill(motivo);
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();
    try {
      await page.waitForURL(/\/citas\/\d+$/, { timeout: 4000 });
      return;
    } catch {
      await expect(horas.first()).toBeVisible();
    }
  }
  throw new Error('No quedaron horas libres para reservar');
}

export function diaHabilAleatorio(): Date {
  const fecha = new Date();
  do {
    fecha.setTime(Date.now() + (1 + Math.floor(Math.random() * 90)) * 86_400_000);
  } while (fecha.getDay() === 0 || fecha.getDay() === 6);
  return fecha;
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
