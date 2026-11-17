import { Page, expect } from '@playwright/test';

export const CUENTAS = {
  paciente: { email: 'juan.perez@correo.com', password: 'Paciente123!', home: /\/citas$/ },
  medico: { email: 'dr.garcia@tusalud.com', password: 'Medico123!', home: /\/agenda$/ },
  admin: { email: 'admin@tusalud.com', password: 'Admin123!', home: /\/admin\/auditoria$/ },
} as const;

export async function iniciarSesion(page: Page, rol: keyof typeof CUENTAS): Promise<void> {
  const cuenta = CUENTAS[rol];
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
