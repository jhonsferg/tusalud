import { expect, test } from '@playwright/test';

import { iniciarSesion, reservarPrimeraHoraLibre, idDeGarcia, ir } from './support';

test.describe('calendario', () => {
  test('el paciente ve sus citas en las vistas de mes, semana y día', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, '/citas');
    await page.getByRole('button', { name: 'Calendario' }).click();

    const vistas = page.getByRole('group', { name: 'Vista del calendario' });
    await vistas.getByRole('button', { name: 'Mes' }).click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();

    await vistas.getByRole('button', { name: 'Semana' }).click();
    await expect(page.getByRole('button', { name: /^Abrir / }).first()).toBeVisible();

    await vistas.getByRole('button', { name: 'Día' }).click();
    await page.getByRole('button', { name: 'Hoy' }).click();
    await expect(page.locator('.ahora')).toBeVisible();

    await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
    await expect(page.locator('.ahora')).toHaveCount(0);
  });

  test('semana y día muestran las 24 horas con separadores y un marcador al pasar el cursor', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/calendario');
    const vistas = page.getByRole('group', { name: 'Vista del calendario' });
    for (const vista of ['Semana', 'Día']) {
      await vistas.getByRole('button', { name: vista }).click();
      await expect(page.locator('.horas span')).toHaveCount(24);
      await expect(page.locator('.linea-hora')).toHaveCount(24);
      await expect(page.locator('.horas span').first()).toHaveText('00:00');
      await expect(page.locator('.horas span').last()).toHaveText('23:00');
      const cuerpo = page.locator('.tiempo-cuerpo');
      await cuerpo.scrollIntoViewIfNeeded();
      const columna = (await page.locator('.columna').first().boundingBox())!;
      const visible = (await page.locator('.tiempo').boundingBox())!;
      await page.mouse.move(columna.x + columna.width / 2, visible.y + visible.height / 2);
      await expect(page.locator('.marcador')).toHaveCount(1);
      await page.mouse.move(visible.x - 20, visible.y - 20);
      await expect(page.locator('.marcador')).toHaveCount(0);
    }
  });

  test('la fila de todo el día no tapa las primeras horas visibles', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/calendario');
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Semana' })
      .click();
    await page.evaluate(() => {
      const el = document.querySelector('.tiempo') as HTMLElement;
      el.scrollTop = 7 * 56;
    });
    const fijo = (await page.locator('.tiempo-fijo').boundingBox())!;
    const hora7 = (await page.locator('.horas span').nth(7).boundingBox())!;
    expect(hora7.y).toBeGreaterThanOrEqual(fijo.y + fijo.height - 2);
  });

  test('en la vista mensual todo el cuadro del día abre el día y muestra un marcador', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/calendario');
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    const celda = page.locator('.mes-celda').nth(10);
    await celda.hover();
    await expect(celda).toHaveCSS('cursor', 'pointer');
    const caja = (await celda.boundingBox())!;
    await page.mouse.click(caja.x + caja.width - 8, caja.y + caja.height - 8);
    await expect(page.locator('.tiempo.dia')).toBeVisible();
  });

  test('la agenda agrupa por día con el número en un círculo y sin cajas superpuestas', async ({
    page,
  }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/calendario');
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Agenda' })
      .click();
    const grupo = page.locator('.agenda-grupo').first();
    await expect(grupo).toBeVisible();
    const numero = grupo.locator('.dia-num');
    const caja = (await numero.boundingBox())!;
    expect(Math.round(caja.width)).toBe(Math.round(caja.height));
    await expect(numero).toHaveCSS('border-radius', '50%');
    const fecha = (await grupo.locator('.agenda-fecha').boundingBox())!;
    const lista = (await grupo.locator('ul').boundingBox())!;
    expect(lista.x).toBeGreaterThanOrEqual(fecha.x + fecha.width);
    await expect(grupo.locator('.agenda-fecha')).toHaveCSS('border-top-width', '0px');
  });

  test('el paciente elige un hueco libre en el calendario para reservar', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
    await expect(page.getByText('Por elegir')).toBeVisible();

    await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
    const huecos = page.getByRole('button', { name: /^Elegir / });
    await expect(huecos.first()).toBeVisible();
    await huecos.first().click();
    await expect(huecos.first()).toHaveAttribute('aria-pressed', 'true');
    await expect(page.getByText('Por elegir')).toHaveCount(0);

    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    await expect(page.getByText(/\d+ libres/).first()).toBeVisible();
  });

  test('exige elegir una hora antes de confirmar la reserva', async ({ page }) => {
    await iniciarSesion(page, 'paciente');
    await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
    await page.locator('#cita-motivo').fill('Sin hora elegida');
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();
    await expect(page.getByText('Elige una hora libre en el calendario.')).toBeVisible();
    await expect(page).toHaveURL(/\/citas\/nueva/);
  });

  test('el médico alterna entre el día y el calendario de su agenda', async ({ page }) => {
    await iniciarSesion(page, 'medico');
    await ir(page, '/agenda');
    await page.getByRole('button', { name: 'Calendario' }).click();
    await page
      .getByRole('group', { name: 'Vista del calendario' })
      .getByRole('button', { name: 'Mes' })
      .click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toBeVisible();
    await page.getByText('Día', { exact: true }).first().click();
    await expect(page.getByRole('grid', { name: 'Calendario mensual' })).toHaveCount(0);
  });
});

test.describe('reprogramación', () => {
  test('el paciente reprograma una cita en su propia página con el calendario', async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await iniciarSesion(page, 'paciente');
    await ir(page, '/citas');
    await page.getByRole('button', { name: 'Ver detalle' }).first().click();
    await expect(page).toHaveURL(/\/citas\/[0-9a-f-]{36}$/);
    const detalle = new URL(page.url()).pathname;
    await page.getByRole('button', { name: 'Reprogramar' }).click();
    await expect(page).toHaveURL(/\/citas\/[0-9a-f-]{36}\/reprogramar$/);
    await expect(page.locator('p-datepicker')).toHaveCount(0);
    await expect(page.getByText('Cita actual').first()).toBeVisible();

    const huecos = page.getByRole('button', { name: /^Elegir / });
    for (let i = 0; i < 12 && !(await huecos.count()); i++) {
      await page.getByRole('button', { name: 'Siguiente', exact: true }).click();
      await page.waitForTimeout(500);
    }
    await huecos.nth(Math.floor((await huecos.count()) / 2)).click();
    await page.getByRole('button', { name: /Confirmar reprogramación$/ }).click();
    await expect(page.getByText(/Tu cita fue reprogramada/).first()).toBeVisible();
    await expect(page).toHaveURL(new RegExp(detalle + '$'));
  });

  test('una cita cancelada no se puede reprogramar', async ({ page }) => {
    test.setTimeout(90_000);
    await iniciarSesion(page, 'paciente');
    await ir(page, `/citas/nueva?medicoId=${await idDeGarcia(page)}`);
    await reservarPrimeraHoraLibre(page, 'Cita para cancelar y no reprogramar');
    const url = new URL(page.url()).pathname;
    await page.getByRole('button', { name: 'Cancelar cita' }).click();
    await page.getByRole('button', { name: 'Sí, cancelar cita' }).click();
    await expect(page.getByText('Cancelada', { exact: true })).toBeVisible();
    await page.goto(`${url}/reprogramar`);
    await expect(page.getByText('Esta cita no se puede reprogramar')).toBeVisible();
  });
});
