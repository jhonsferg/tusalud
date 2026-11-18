import { expect, test } from '@playwright/test';

import { CUENTAS, iniciarSesion, ir } from './support';

const TABLEROS: readonly { cuenta: keyof typeof CUENTAS; seccion: string }[] = [
  { cuenta: 'paciente', seccion: 'Tu próxima cita' },
  { cuenta: 'medico', seccion: 'Agenda de hoy' },
  { cuenta: 'recepcion', seccion: 'Cola de espera' },
  { cuenta: 'enfermeria', seccion: 'Emergencias por prioridad' },
  { cuenta: 'caja', seccion: 'Cuentas por cobrar' },
  { cuenta: 'laboratorio', seccion: 'Cola de trabajo' },
  { cuenta: 'imagenes', seccion: 'Cola de trabajo' },
  { cuenta: 'farmacia', seccion: 'Recetas por dispensar' },
  { cuenta: 'almacen', seccion: 'Alertas de inventario' },
  { cuenta: 'rrhh', seccion: 'Ausencias por resolver' },
  { cuenta: 'gerencia', seccion: 'Ingresos por día' },
  { cuenta: 'finanzas', seccion: 'Cobros por día' },
  { cuenta: 'operaciones', seccion: 'Ocupación hospitalaria' },
  { cuenta: 'calidad', seccion: 'Satisfacción por especialidad' },
  { cuenta: 'admin', seccion: 'Accesos recientes' },
];

test.describe('inicio por rol', () => {
  for (const { cuenta, seccion } of TABLEROS) {
    test(`${cuenta} abre su tablero de inicio con saludo y su sección principal`, async ({
      page,
    }) => {
      await iniciarSesion(page, cuenta);
      await expect(
        page.getByRole('heading', { level: 1, name: /^Buen(os|as) (días|tardes|noches)/ }),
      ).toBeVisible();
      await expect(page.getByRole('heading', { level: 2, name: seccion })).toBeVisible();
      await expect(page.getByRole('status', { name: /^Módulo actual: / })).toBeVisible();
    });
  }
});

test.describe('seguridad en la experiencia', () => {
  test('el modo privacidad difumina los datos del paciente y los revela al pasar el cursor', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/pacientes');
    const dato = page.locator('.sensible').first();
    await expect(dato).toBeVisible();
    await expect(dato).toHaveCSS('filter', 'none');

    await page.getByRole('button', { name: /Activar modo privacidad/ }).click();
    await expect(dato).toHaveCSS('filter', /blur/);
    await dato.hover();
    await expect(dato).toHaveCSS('filter', 'none');

    await page.getByRole('button', { name: 'Desactivar modo privacidad' }).click();
    await expect(dato).toHaveCSS('filter', 'none');
  });

  test('bloquear la pantalla exige la contraseña y se desbloquea al ingresarla', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await page.getByRole('button', { name: 'Bloquear pantalla' }).first().click();
    const bloqueo = page.getByRole('dialog', { name: 'Sesión bloqueada' });
    await expect(bloqueo).toBeVisible();

    await bloqueo.getByLabel('Contraseña').fill('incorrecta');
    await bloqueo.getByRole('button', { name: 'Desbloquear' }).click();
    await expect(bloqueo.getByText('La contraseña no es correcta.')).toBeVisible();

    await bloqueo.getByLabel('Contraseña').fill(CUENTAS.paciente.password);
    await bloqueo.getByRole('button', { name: 'Desbloquear' }).click();
    await expect(bloqueo).toHaveCount(0);
    await expect(page.getByRole('heading', { level: 1, name: /^Buen(os|as) / })).toBeVisible();
  });

  test('el bloqueo también se puede cerrar saliendo de la sesión', async ({ page }) => {
    await iniciarSesion(page, 'recepcion');
    await page.getByRole('button', { name: 'Bloquear pantalla' }).first().click();
    const bloqueo = page.getByRole('dialog', { name: 'Sesión bloqueada' });
    await expect(bloqueo).toBeVisible();
    await bloqueo.getByRole('button', { name: 'Cerrar sesión' }).click();
    await expect(page).toHaveURL(/\/auth\/login/);
  });

  test('la historia clínica de un paciente avisa que el acceso queda auditado', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/pacientes');
    await page
      .getByRole('row', { name: /Carlos Mendoza/ })
      .getByRole('button', { name: 'Historia' })
      .click();
    await expect(page.getByRole('note')).toContainText('queda registrado en la auditoría clínica');
  });

  test('el paciente ve su historia sin ese aviso porque es su propia información', async ({
    page,
  }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/historia-clinica');
    await expect(page.getByRole('heading', { name: 'Juan Pérez' })).toBeVisible();
    await expect(page.getByText('queda registrado en la auditoría clínica')).toHaveCount(0);
  });
});

test.describe('adaptación al móvil', () => {
  test('el paciente navega con la barra inferior @responsive', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    const barra = page.getByRole('navigation', { name: 'Navegación rápida' });
    await expect(barra).toBeVisible();
    for (const nombre of ['Inicio', 'Citas', 'Reservar', 'Resultados', 'Más opciones del menú']) {
      await expect(
        barra.getByRole(nombre.startsWith('Más') ? 'button' : 'link', { name: nombre }),
      ).toBeVisible();
    }
    await barra.getByRole('link', { name: 'Citas' }).click();
    await expect(page).toHaveURL(/\/paciente\/citas$/);
    await barra.getByRole('link', { name: 'Inicio' }).click();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
  });
});
