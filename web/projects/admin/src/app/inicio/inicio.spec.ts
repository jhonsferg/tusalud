import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { InicioAdmin } from './inicio';

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ADMIN'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(InicioAdmin);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  return { fixture, http, html: () => fixture.nativeElement as HTMLElement };
}

function responderBase(http: HttpTestingController): void {
  http
    .expectOne((r) => r.url.endsWith('/admin/estadisticas'))
    .flush({
      citasHoy: 4,
      citasPendientes: 1,
      totalPacientes: 20,
      totalMedicos: 7,
      ingresosMes: 1500,
    });
  http.expectOne((r) => r.url.endsWith('/admin/usuarios')).flush([]);
  http.expectOne((r) => r.url.endsWith('/admin/roles')).flush([]);
  http.expectOne((r) => r.url.endsWith('/admin/auditoria/citas-por-estado')).flush([]);
}

describe('Inicio de administración', () => {
  it('muestra los eventos de seguridad, el estado del sistema y el almacenamiento', () => {
    const { fixture, http, html } = montar();
    responderBase(http);
    http
      .expectOne((r) => r.url.endsWith('/admin/seguridad/resumen'))
      .flush({
        ultimas24Horas: { total: 5, altas: 2, avisos: 3, porTipo: [] },
        ultimos7Dias: { total: 12, altas: 2, avisos: 6, porTipo: [] },
        iniciosInusuales: [
          {
            id: 'e1',
            tipo: 'BLOQUEO_INTENTOS',
            etiqueta: 'Bloqueo por intentos de verificación',
            severidad: 'ALTA',
            usuarioNombre: 'Carlos Mendoza',
            ip: '45.231.88.x',
            creadoEn: new Date().toISOString(),
          },
        ],
      });
    http
      .expectOne((r) => r.url.endsWith('/admin/sistema/estado'))
      .flush({
        estadoGeneral: 'OK',
        version: '1.2.3',
        iniciadoEn: '2026-11-17T08:00:00',
        segundosEnMarcha: 7500,
        componentes: [
          { nombre: 'Base de datos', estado: 'OK', latenciaMs: 4 },
          { nombre: 'Almacén de objetos', estado: 'CAIDO', latenciaMs: 0 },
        ],
        migraciones: { disponible: true, aplicadas: 19, ultima: '19' },
      });
    http
      .expectOne((r) => r.url.endsWith('/admin/almacenamiento/uso'))
      .flush({
        totalObjetos: 3,
        totalBytes: 3 * 1024 * 1024,
        buckets: [{ bucket: 'imagenes', objetos: 3, bytes: 3 * 1024 * 1024, propositos: [] }],
      });
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('Alta severidad');
    expect(texto).toContain('Bloqueo por intentos de verificación');
    expect(texto).toContain('Versión 1.2.3');
    expect(texto).toContain('Operativo');
    expect(texto).toContain('Sin respuesta');
    expect(texto).toContain('19 aplicadas');
    expect(texto).toContain('3.0 MB');
    expect(texto).not.toContain('requieren un endpoint del backend');
  });

  it('sin eventos inusuales lo indica y sin archivos muestra el estado vacío', () => {
    const { fixture, http, html } = montar();
    responderBase(http);
    http
      .expectOne((r) => r.url.endsWith('/admin/seguridad/resumen'))
      .flush({
        ultimas24Horas: { total: 0, altas: 0, avisos: 0, porTipo: [] },
        ultimos7Dias: { total: 0, altas: 0, avisos: 0, porTipo: [] },
        iniciosInusuales: [],
      });
    http
      .expectOne((r) => r.url.endsWith('/admin/sistema/estado'))
      .flush({
        estadoGeneral: 'OK',
        version: 'x',
        iniciadoEn: '2026-11-17T08:00:00',
        segundosEnMarcha: 10,
        componentes: [],
        migraciones: { disponible: false, aplicadas: 0 },
      });
    http
      .expectOne((r) => r.url.endsWith('/admin/almacenamiento/uso'))
      .flush({
        totalObjetos: 0,
        totalBytes: 0,
        buckets: [],
      });
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('Sin inicios de sesión inusuales esta semana.');
    expect(texto).toContain('Aún no hay archivos guardados.');
  });
});
