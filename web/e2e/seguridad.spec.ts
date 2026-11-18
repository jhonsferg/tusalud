import { expect, test } from '@playwright/test';

import {
  cerrarSesion,
  codigoTotp,
  iniciarSesion,
  omitirGuia,
  registrarPaciente,
  ir,
} from './support';

test.describe('acceso por rol', () => {
  test('el médico no ve especialidades ni el directorio de médicos y vuelve a su agenda', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await expect(page.getByRole('link', { name: 'Especialidades' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Médicos', exact: true })).toHaveCount(0);

    for (const ruta of [
      '/admin/especialidades',
      '/admin/medicos',
      '/paciente/citas/nueva',
      '/paciente/pagos',
      '/admin/panel',
    ]) {
      await page.goto(ruta);
      await expect(page).toHaveURL(/\/medico\/inicio$/);
    }
  });

  test('el paciente y el administrador vuelven a su página de inicio', async ({ browser }) => {
    const paciente = await (await browser.newContext()).newPage();
    await iniciarSesion(paciente, 'paciente');
    for (const ruta of ['/admin/usuarios', '/medico/agenda', '/medico/pacientes']) {
      await paciente.goto(ruta);
      await expect(paciente).toHaveURL(/\/paciente\/inicio$/);
    }

    const admin = await (await browser.newContext()).newPage();
    await iniciarSesion(admin, 'admin');
    for (const ruta of [
      '/paciente/citas/nueva',
      '/paciente/pagos/metodos',
      '/paciente/historia-clinica',
      '/medico/agenda',
    ]) {
      await admin.goto(ruta);
      await expect(admin).toHaveURL(/\/admin\/$/);
    }
  });
});

test.describe('guía de bienvenida', () => {
  test('se muestra en el primer ingreso y se puede abrir de nuevo', async ({ page }) => {
    const cuenta = await registrarPaciente(page, 'guia');
    const guia = page.getByRole('dialog', { name: 'Te damos la bienvenida a TuSalud' });
    await expect(guia).toBeVisible({ timeout: 10_000 });
    await guia.getByRole('button', { name: 'Comenzar' }).click();
    await expect(page.getByRole('dialog', { name: 'Tu inicio' })).toBeVisible();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect(page.getByRole('dialog', { name: 'Reserva tu cita' })).toBeVisible();
    await expect(page).toHaveURL(/\/paciente\/citas\/nueva/);
    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect(page.getByRole('dialog', { name: 'Mis citas' })).toBeVisible();
    await expect(page).toHaveURL(/\/paciente\/citas$/);
    await page.getByRole('button', { name: 'Siguiente' }).click();
    await expect(page.getByRole('dialog', { name: 'Tu historia clínica' })).toBeVisible();
    await expect(page).toHaveURL(/\/paciente\/historia-clinica/);
    const guardado = page.waitForResponse(
      (r) => r.url().includes('/perfil/preferencias') && r.request().method() === 'PUT',
    );
    await page.keyboard.press('Escape');
    await guardado;
    await expect(page.getByRole('dialog', { name: 'Tu historia clínica' })).toHaveCount(0);
    await expect(page).toHaveURL(/\/paciente\/inicio$/);

    await page.reload();
    await expect(page.getByRole('button', { name: 'Menú de usuario' })).toBeVisible();
    await page.waitForTimeout(1500);
    await expect(
      page.getByRole('dialog', { name: 'Te damos la bienvenida a TuSalud' }),
    ).toHaveCount(0);

    await cerrarSesion(page);
    await expect(page).toHaveURL(/\/auth\/login/);
    await page.getByLabel('Email').fill(cuenta.email);
    await page.locator('#login-password').fill(cuenta.password);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);
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

    await ir(page, '/perfil?tab=seguridad');
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
    await expect(page).toHaveURL(/\/paciente\/inicio$/);

    await cerrarSesion(page);
    await page.getByLabel('Email').fill(cuenta.email);
    await page.locator('#login-password').fill(cuenta.password);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await page.getByRole('button', { name: 'Usar un código de recuperación' }).click();
    await page.locator('#login-codigo').fill(recuperacion);
    await page.getByRole('button', { name: 'Verificar y entrar' }).click();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);

    await ir(page, '/perfil?tab=seguridad');
    await page.getByRole('button', { name: 'Desactivar', exact: true }).click();
    await page.locator('#mfa-password').fill(cuenta.password);
    await page.locator('#mfa-codigo').fill(codigoTotp(secreto));
    await page.getByRole('dialog').getByRole('button', { name: 'Desactivar', exact: true }).click();
    await expect(page.getByText('La verificación en dos pasos fue desactivada.')).toBeVisible();
  });
});

test.describe('renovación de sesión', () => {
  test('renueva el token de acceso sin pedir las credenciales otra vez', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    const antes = await page.evaluate(() => JSON.parse(localStorage.getItem('tusalud.session')!));
    expect(antes.refreshToken).toBeTruthy();

    await page.evaluate(() => {
      const sesion = JSON.parse(localStorage.getItem('tusalud.session')!);
      sesion.token = 'invalido.invalido.invalido';
      localStorage.setItem('tusalud.session', JSON.stringify(sesion));
    });
    await page.reload();
    await expect(page.getByRole('button', { name: 'Menú de usuario' })).toBeVisible();
    await expect(page).toHaveURL(/\/paciente\/inicio$/);

    await expect
      .poll(async () =>
        page.evaluate(() => JSON.parse(localStorage.getItem('tusalud.session')!).refreshToken),
      )
      .not.toBe(antes.refreshToken);
  });

  test('cierra la sesión si el refresh token ya no es válido', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await page.evaluate(() => {
      const sesion = JSON.parse(localStorage.getItem('tusalud.session')!);
      sesion.token = 'invalido.invalido.invalido';
      sesion.refreshToken = 'revocado';
      localStorage.setItem('tusalud.session', JSON.stringify(sesion));
    });
    await page.reload();
    await expect(page).toHaveURL(/\/auth\/login/);
  });
});
