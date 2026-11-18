import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { Seguridad } from './seguridad';

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ADMIN'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(Seguridad);
  fixture.detectChanges();
  return { fixture, http: TestBed.inject(HttpTestingController) };
}

const EVENTO = {
  id: 'e1',
  tipo: 'LOGIN_FALLIDO',
  etiqueta: 'Inicio de sesión fallido',
  severidad: 'AVISO',
  usuarioNombre: 'Juan Pérez',
  ip: '190.236.14.x',
  dispositivo: 'Chrome en Windows',
  detalle: 'Credenciales inválidas',
  creadoEn: '2026-11-17T10:00:00',
};

describe('Página de seguridad', () => {
  it('lista los eventos con su cuenta, origen y detalle', () => {
    const { fixture, http } = montar();
    http
      .expectOne((r) => r.url.endsWith('/admin/seguridad/resumen'))
      .flush({
        ultimas24Horas: { total: 1, altas: 0, avisos: 1, porTipo: [] },
        ultimos7Dias: { total: 4, altas: 0, avisos: 2, porTipo: [] },
        iniciosInusuales: [],
      });
    http
      .expectOne(
        (r) => r.url.endsWith('/admin/seguridad/eventos') && r.params.get('pagina') === '0',
      )
      .flush({ contenido: [EVENTO], pagina: 0, tamano: 20, total: 1, totalPaginas: 1 });
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Inicio de sesión fallido');
    expect(texto).toContain('Juan Pérez');
    expect(texto).toContain('190.236.14.x');
    expect(texto).toContain('Credenciales inválidas');
    expect(texto).toContain('Eventos en 7 días');
  });

  it('al elegir un tipo vuelve a la primera página y pide el filtro al servidor', () => {
    const { fixture, http } = montar();
    http
      .expectOne((r) => r.url.endsWith('/admin/seguridad/resumen'))
      .flush({
        ultimas24Horas: { total: 0, altas: 0, avisos: 0, porTipo: [] },
        ultimos7Dias: { total: 0, altas: 0, avisos: 0, porTipo: [] },
        iniciosInusuales: [],
      });
    http
      .expectOne((r) => r.url.endsWith('/admin/seguridad/eventos'))
      .flush({ contenido: [], pagina: 0, tamano: 20, total: 0, totalPaginas: 0 });

    const componente = fixture.componentInstance as unknown as {
      tipo: { set(valor: string | null): void };
      cambiarFiltro(): void;
    };
    componente.tipo.set('BLOQUEO_INTENTOS');
    componente.cambiarFiltro();

    const filtrada = http.expectOne(
      (r) =>
        r.url.endsWith('/admin/seguridad/eventos') && r.params.get('tipo') === 'BLOQUEO_INTENTOS',
    );
    expect(filtrada.request.params.get('pagina')).toBe('0');
    filtrada.flush({ contenido: [], pagina: 0, tamano: 20, total: 0, totalPaginas: 0 });
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Sin eventos');
  });
});
