import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { PERIODOS_DIAS } from '@tusalud/ui/tablero';
import { InicioGerencia } from './inicio';

function indicador(actual: number, anterior = 0, variacionPct: number | null = null) {
  return { actual, anterior, variacionPct };
}

function reporte() {
  return {
    periodo: { desde: '2026-10-19', hasta: '2026-11-17', agrupacion: 'DIA', dias: 30 },
    periodoAnterior: { desde: '2026-09-19', hasta: '2026-10-18', agrupacion: 'DIA', dias: 30 },
    citas: indicador(15, 10, 50),
    atendidas: indicador(8),
    canceladas: indicador(2),
    tasaAusenciaPct: indicador(38.46),
    ingresos: indicador(3840.8, 3000, 28.0),
    ticketPromedio: indicador(137.17),
    ocupacionHospitalariaPct: indicador(2.38),
    esperaPromedioMin: indicador(13.75),
    valoracionPromedio: indicador(4.6),
    serie: [
      { fecha: '2026-11-16', citas: 2, atendidas: 1, ingresos: 100 },
      { fecha: '2026-11-17', citas: 5, atendidas: 3, ingresos: 900 },
    ],
  };
}

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_GERENCIA'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(InicioGerencia);
  fixture.detectChanges();
  return {
    fixture,
    http: TestBed.inject(HttpTestingController),
    html: () => fixture.nativeElement as HTMLElement,
  };
}

describe('Inicio de gerencia', () => {
  it('pide el reporte ejecutivo del periodo guardado y muestra los indicadores redondeados', () => {
    const { fixture, http, html } = montar();
    const peticion = http.expectOne((r) => r.url.endsWith('/reportes/ejecutivo'));
    expect(PERIODOS_DIAS).toContain(Number(localStorage.getItem('tusalud.periodo.dias') ?? 30));
    expect(peticion.request.params.get('agrupacion')).toBe('DIA');
    peticion.flush(reporte());
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('2.4%');
    expect(texto).toContain('13.8 min');
    expect(texto).toContain('38.5%');
    expect(texto).toContain('4.6 / 5');
    expect(html().querySelectorAll('app-bar-chart').length).toBe(2);
  });

  it('cambiar el periodo vuelve a consultar con las fechas nuevas y lo recuerda', () => {
    const { fixture, http, html } = montar();
    http.expectOne((r) => r.url.endsWith('/reportes/ejecutivo')).flush(reporte());
    fixture.detectChanges();

    const botones = Array.from(
      html().querySelectorAll<HTMLButtonElement>('app-selector-periodo button'),
    );
    botones.find((boton) => boton.textContent?.includes('90'))!.click();
    const segunda = http.expectOne((r) => r.url.endsWith('/reportes/ejecutivo'));
    expect(segunda.request.params.get('agrupacion')).toBe('SEMANA');
    segunda.flush(reporte());
    expect(localStorage.getItem('tusalud.periodo.dias')).toBe('90');
  });

  it('muestra el mensaje del servidor si el reporte falla', () => {
    const { fixture, http, html } = montar();
    http
      .expectOne((r) => r.url.endsWith('/reportes/ejecutivo'))
      .flush({ message: 'Sin permiso' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    expect(html().querySelector('[role="alert"]')).not.toBeNull();
  });
});
