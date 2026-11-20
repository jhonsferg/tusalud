import { expect, test } from '@playwright/test';

import { iniciarSesion, ir } from './support';

function ahoraLocal(): string {
  const d = new Date(Date.now() - 3_600_000);
  const dos = (n: number): string => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}T${dos(d.getHours())}:${dos(d.getMinutes())}`;
}

test.describe('seguridad del paciente', () => {
  test('enfermería notifica un evento y calidad lo analiza, asigna una acción y lo cierra', async ({
    page,
  }) => {
    const area = `Hospitalización ${Date.now().toString().slice(-6)}`;
    await iniciarSesion(page, 'enfermeria');
    await ir(page, '/eventos-seguridad');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Notificar un evento de seguridad' }),
    ).toBeVisible();
    await page.getByLabel('Área donde ocurrió').fill(area);
    await page.getByLabel('Fecha y hora').fill(ahoraLocal());
    await page
      .getByLabel('Qué ocurrió')
      .fill('Caída de un paciente al levantarse de la cama sin lesión');
    await page.getByRole('button', { name: /Enviar reporte/ }).click();
    await expect(page.getByText(/EA-\d{4}-\d{4}/).first()).toBeVisible();

    await page.context().clearCookies();
    await page.evaluate(() => localStorage.clear());
    await iniciarSesion(page, 'calidad');
    await ir(page, '/seguridad-paciente');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Seguridad del paciente' }),
    ).toBeVisible();
    const fila = page.locator('p.fila', { hasText: area }).first();
    await expect(fila).toBeVisible();
    await fila.getByRole('button', { name: /Gestionar/ }).click();

    await page.getByLabel('Causa 1').fill('No había barandas');
    await page.getByLabel('Conclusión del análisis').fill('Falta de evaluación del riesgo');
    await page.getByRole('button', { name: /Guardar análisis/ }).click();
    await expect(page.getByText('En análisis').first()).toBeVisible();

    const limite = new Date(Date.now() + 10 * 86_400_000).toISOString().slice(0, 10);
    await page.getByLabel('Descripción de la acción').fill('Evaluar el riesgo de caída al ingreso');
    await page.getByLabel('Fecha límite').fill(limite);
    await page.getByRole('button', { name: /Agregar acción/ }).click();
    await page.getByRole('button', { name: /Marcar hecha/ }).click();
    await page.getByLabel('Verificación de eficacia').fill('Sin nuevas caídas en tres meses');
    await page.getByRole('button', { name: /Cerrar reporte/ }).click();
    await expect(page.getByText(/Cerrado el/)).toBeVisible();
  });

  test('el médico busca una guía de práctica clínica', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/guias');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Guías de práctica clínica' }),
    ).toBeVisible();
    await page.getByLabel(/Buscar guías/).fill('hipertensión');
    await page.getByRole('button', { name: /Buscar$/ }).click();
    await expect(page.getByText('Hipertensión arterial en el adulto').first()).toBeVisible();
    await expect(page.getByText(/Por validar/).first()).toBeVisible();
  });
});
