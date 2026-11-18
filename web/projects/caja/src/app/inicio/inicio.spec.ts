import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { InicioCaja } from './inicio';

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_CAJA'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(InicioCaja);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  return { fixture, http, html: () => fixture.nativeElement as HTMLElement };
}

describe('Inicio de caja', () => {
  it('con la caja cerrada ofrece abrirla y no pide el resumen del turno', () => {
    const { fixture, http, html } = montar();
    http
      .expectOne((r) => r.url.endsWith('/caja/actual'))
      .flush({ message: 'No tienes una caja abierta' }, { status: 404, statusText: 'No' });
    http.expectOne((r) => r.url.endsWith('/caja/pendientes')).flush([]);
    http.expectOne((r) => r.url.endsWith('/caja/historial')).flush([]);
    fixture.detectChanges();

    http.expectNone((r) => r.url.endsWith('/caja/resumen'));
    const texto = html().textContent ?? '';
    expect(texto).toContain('Caja cerrada');
    expect(texto).toContain('Abrir caja');
    expect(texto).toContain('No hay cuentas por cobrar.');
  });

  it('con la caja abierta muestra el cobrado, lo pendiente y la última diferencia', () => {
    const { fixture, http, html } = montar();
    http
      .expectOne((r) => r.url.endsWith('/caja/actual'))
      .flush({
        id: 'c',
        estado: 'ABIERTA',
        apertura: new Date().toISOString(),
        montoApertura: 100,
        cajeroNombre: 'Jorge',
      });
    http
      .expectOne((r) => r.url.endsWith('/caja/resumen'))
      .flush({ total: 250, comprobantes: 3, porMetodo: [{ metodo: 'EFECTIVO', total: 250 }] });
    http
      .expectOne((r) => r.url.endsWith('/caja/pendientes'))
      .flush([
        {
          origen: 'CITA',
          referenciaId: 'r1',
          paciente: 'Ana Ruiz',
          dni: '1',
          descripcion: 'Consulta',
          monto: 150,
          fecha: '2026-11-17',
        },
      ]);
    http
      .expectOne((r) => r.url.endsWith('/caja/historial'))
      .flush([{ id: 'x', estado: 'CERRADA', diferencia: -5 }]);
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('Caja abierta');
    expect(texto).toContain('Cerrar caja');
    expect(texto).toContain('Ana Ruiz');
    expect(texto).toContain('Efectivo');
    expect(texto).toContain('Última diferencia');
    expect(texto).toContain('5.00');
  });
});
