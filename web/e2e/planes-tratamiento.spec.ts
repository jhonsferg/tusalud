import { expect, test } from '@playwright/test';

import { cerrarSesion, iniciarSesion, ir, registrarPaciente, tokenDe } from './support';

function cabecera(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

test.describe('planes de tratamiento de crónicos', () => {
  test('el médico crea un plan, el paciente registra una medición fuera de la meta y el tablero lo refleja', async ({
    page,
  }) => {
    const cuenta = await registrarPaciente(page, 'Cronico');
    const sesion = await page.request.post('/api/v1/auth/login', {
      data: { email: cuenta.email, password: cuenta.password },
    });
    const pacienteToken = ((await sesion.json()) as { token: string }).token;
    const medicoToken = await tokenDe(page, 'medico');
    const medicos = (await (
      await page.request.get('/api/v1/medicos', { headers: cabecera(pacienteToken) })
    ).json()) as { id: string; apellido: string; especialidades: { id: string }[] }[];
    const garcia = medicos.find((m) => m.apellido === 'García')!;

    let pacienteId = '';
    for (let intento = 0; intento < 10 && pacienteId === ''; intento++) {
      const fecha = new Date();
      fecha.setDate(fecha.getDate() + 300 + Math.floor(Math.random() * 400));
      if (fecha.getDay() === 0 || fecha.getDay() === 6) {
        continue;
      }
      const dos = (n: number): string => String(n).padStart(2, '0');
      const hora = 8 + Math.floor(Math.random() * 4);
      const minutos = Math.random() < 0.5 ? '00' : '30';
      const cita = await page.request.post('/api/v1/citas', {
        headers: cabecera(pacienteToken),
        data: {
          medicoId: garcia.id,
          especialidadId: garcia.especialidades[0].id,
          fechaHora: `${fecha.getFullYear()}-${dos(fecha.getMonth() + 1)}-${dos(fecha.getDate())}T${dos(hora)}:${minutos}:00`,
          motivo: 'Control de hipertensión e2e',
          modalidad: 'PRESENCIAL',
        },
      });
      if (cita.status() === 201) {
        pacienteId = ((await cita.json()) as { pacienteId: string }).pacienteId;
      }
    }
    expect(pacienteId).not.toBe('');

    const plan = await page.request.post('/api/v1/planes-tratamiento', {
      headers: cabecera(medicoToken),
      data: {
        pacienteId,
        programa: 'HIPERTENSION',
        diagnosticoCodigo: 'I10',
        objetivos: 'Mantener la presión arterial bajo control',
        frecuenciaControlDias: 30,
        cantidadControles: 3,
        metas: [{ parametro: 'PRESION_SISTOLICA', valorMin: 90, valorMax: 130, unidad: 'mmHg' }],
      },
    });
    expect(plan.status()).toBe(201);

    await ir(page, '/mi-plan');
    await expect(
      page.getByRole('heading', { level: 1, name: 'Mi plan de tratamiento' }),
    ).toBeVisible();
    await page.getByRole('button', { name: /Ver mi plan de HIPERTENSION/ }).click();
    await expect(page.getByText('Controles programados')).toBeVisible();
    await page.getByLabel('Valor medido').fill('172');
    await page.getByRole('button', { name: /Registrar medición/ }).click();
    await expect(page.getByText('Fuera de la meta').first()).toBeVisible();

    await cerrarSesion(page);
    await iniciarSesion(page, 'medico');
    await ir(page, '/pacientes-cronicos');
    await expect(page.getByRole('heading', { level: 1, name: 'Pacientes crónicos' })).toBeVisible();
    const fila = page.locator('p.fila', { hasText: 'Cronico' }).first();
    await expect(fila).toBeVisible();
    await expect(fila.getByText(/1 mediciones fuera de la meta/)).toBeVisible();
  });
});
