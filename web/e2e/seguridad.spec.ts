import { expect, test } from '@playwright/test';

import { cerrarSesion, codigoTotp, iniciarSesion, omitirGuia, registrarPaciente } from './support';

test.describe('acceso por rol', () => {
  test('el médico no ve especialidades ni el directorio de médicos y vuelve a su agenda', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await expect(page.getByRole('link', { name: 'Especialidades' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Médicos', exact: true })).toHaveCount(0);

    for (const ruta of ['/especialidades', '/medicos', '/citas/nueva', '/pagos', '/admin/panel']) {
      await page.goto(ruta);
      await expect(page).toHaveURL(/\/agenda$/);
    }
  });

  test('el paciente y el administrador vuelven a su página de inicio', async ({ browser }) => {
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    for (const ruta of ['/admin/usuarios', '/agenda', '/pacientes']) {
      await paciente.goto(ruta);
      await expect(paciente).toHaveURL(/\/citas$/);
    }

    const admin = await (await browser.newContext()).newPage();
    await iniciarSesion(admin, 'admin');
    for (const ruta of ['/citas/nueva', '/pagos/metodos', '/historia-clinica', '/agenda']) {
      await admin.goto(ruta);
      await expect(admin).toHaveURL(/\/admin\/panel$/);
    }
  });
});

test.describe('guía de bienvenida', () => {
  test('se muestra en el primer ingreso y se puede abrir de nuevo', async ({ page }) => {
    await registrarPaciente(page, 'guia');
    const guia = page.getByRole('dialog', { name: 'Te damos la bienvenida a TuSalud' });
    await expect(guia).toBeVisible({ timeout: 10_000 });
    await guia.getByRole('button', { name: 'Comenzar' }).click();
    await expect(page.getByRole('dialog', { name: 'Reserva tu cita' })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: 'Reserva tu cita' })).toHaveCount(0);

    await page.reload();
    await expect(page.getByRole('button', { name: 'Menú de usuario' })).toBeVisible();
    await page.waitForTimeout(1500);
    await expect(
      page.getByRole('dialog', { name: 'Te damos la bienvenida a TuSalud' }),
    ).toHaveCount(0);

    await page.getByRole('button', { name: 'Menú de usuario' }).click();
    await page.getByRole('button', { name: 'Guía de bienvenida' }).click();
    await expect(
      page.getByRole('dialog', { name: 'Te damos la bienvenida a TuSalud' }),
    ).toBeVisible();
    const avanzar = page.getByRole('button', { name: /Siguiente|Comenzar|Finalizar/ });
    for (let paso = 0; paso < 20; paso++) {
      try {
        await avanzar.first().click({ timeout: 2000 });
      } catch {
        break;
      }
    }
    await expect(page.getByRole('dialog', { name: 'Todo listo' })).toHaveCount(0);
  });
});

test.describe('verificación en dos pasos', () => {
  test('activa MFA, exige el código al ingresar y permite usar un código de recuperación', async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await omitirGuia(page);
    const cuenta = await registrarPaciente(page, 'mfa');

    await page.goto('/perfil?tab=seguridad');
    await expect(page.locator('p-tag').getByText('Desactivada', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Activar verificación' }).click();
    const secreto = (await page.locator('.clave-manual code').innerText()).trim();
    await page.getByRole('button', { name: 'Ya lo escaneé' }).click();
    await page.locator('#mfa-codigo').fill('000000');
    await page.getByRole('button', { name: 'Verificar y activar' }).click();
    await expect(page.getByText(/no es válido/).first()).toBeVisible();

    await page.locator('#mfa-codigo').fill(codigoTotp(secreto));
    await page.getByRole('button', { name: 'Verificar y activar' }).click();
    await expect(page.getByText(/Guarda estos códigos/)).toBeVisible();
    const recuperacion = (await page.locator('.codigos li').first().innerText()).trim();
    await page.getByRole('button', { name: 'Listo' }).click();
    await expect(page.locator('p-tag').getByText('Activada', { exact: true })).toBeVisible();

    await cerrarSesion(page);
    await page.getByLabel('Email').fill(cuenta.email);
    await page.locator('#login-password').fill(cuenta.password);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page.getByRole('heading', { name: 'Verificación en dos pasos' })).toBeVisible();

    await page.locator('#login-codigo').fill('000000');
    await page.getByRole('button', { name: 'Verificar y entrar' }).click();
    await expect(page.getByText('El código no es válido o ya venció.')).toBeVisible();

    await page.locator('#login-codigo').fill(codigoTotp(secreto));
    await page.getByRole('button', { name: 'Verificar y entrar' }).click();
    await expect(page).toHaveURL(/\/citas$/);

    await cerrarSesion(page);
    await page.getByLabel('Email').fill(cuenta.email);
    await page.locator('#login-password').fill(cuenta.password);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await page.getByRole('button', { name: 'Usar un código de recuperación' }).click();
    await page.locator('#login-codigo').fill(recuperacion);
    await page.getByRole('button', { name: 'Verificar y entrar' }).click();
    await expect(page).toHaveURL(/\/citas$/);

    await page.goto('/perfil?tab=seguridad');
    await page.getByRole('button', { name: 'Desactivar', exact: true }).click();
    await page.locator('#mfa-password').fill(cuenta.password);
    await page.locator('#mfa-codigo').fill(codigoTotp(secreto));
    await page.getByRole('dialog').getByRole('button', { name: 'Desactivar', exact: true }).click();
    await expect(page.getByText('La verificación en dos pasos fue desactivada.')).toBeVisible();
  });
});
