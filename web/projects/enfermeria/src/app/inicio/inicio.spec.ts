import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { InicioEnfermeria } from './inicio';

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ENFERMERIA'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(InicioEnfermeria);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  return { fixture, http, html: () => fixture.nativeElement as HTMLElement };
}

function responderBase(http: HttpTestingController): void {
  http
    .expectOne((r) => r.url.endsWith('/hospitalizacion/resumen'))
    .flush({
      camasLibres: 3,
      camasOcupadas: 2,
      camasLimpieza: 0,
      ingresosActivos: 2,
      emergenciasEnEspera: 0,
      ocupacionPorcentaje: 40,
    });
  http.expectOne((r) => r.url.endsWith('/hospitalizacion/mapa')).flush({ areas: [] });
  http.expectOne((r) => r.url.endsWith('/hospitalizacion/emergencias')).flush([]);
  http.expectOne((r) => r.url.includes('/hospitalizacion/ingresos')).flush([]);
}

describe('Inicio de enfermería', () => {
  it('lista los ingresos con horas sin evolución de enfermería', () => {
    const { fixture, http, html } = montar();
    responderBase(http);
    http
      .expectOne((r) => r.url.endsWith('/hospitalizacion/evoluciones-pendientes'))
      .flush([
        {
          ingresoId: 'i1',
          codigo: 'HOS-000001',
          cama: 'H-101',
          area: 'HOSPITALIZACION',
          paciente: 'Rosa Quispe',
          ingresadoEn: '2026-11-16T08:00:00',
          ultimaEvolucion: '2026-11-17T01:00:00',
          horasSinRegistro: 13,
        },
        {
          ingresoId: 'i2',
          codigo: 'HOS-000002',
          cama: 'U-201',
          area: 'UCI',
          paciente: 'Pedro Soto',
          ingresadoEn: '2026-11-17T02:00:00',
          ultimaEvolucion: null,
          horasSinRegistro: 9,
        },
      ]);
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('Evoluciones pendientes');
    expect(texto).toContain('Rosa Quispe');
    expect(texto).toContain('Cama H-101');
    expect(texto).toContain('sin registro hace 13 h');
    expect(texto).toContain('sin ninguna evolución hace 9 h');
  });

  it('cuando no hay pendientes lo indica', () => {
    const { fixture, http, html } = montar();
    responderBase(http);
    http.expectOne((r) => r.url.endsWith('/hospitalizacion/evoluciones-pendientes')).flush([]);
    fixture.detectChanges();

    expect(html().textContent).toContain('Las evoluciones de enfermería están al día.');
  });
});
