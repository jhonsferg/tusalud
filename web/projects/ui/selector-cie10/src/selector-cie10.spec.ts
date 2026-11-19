import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Cie10Response } from '@tusalud/core';

import { SelectorCie10 } from './selector-cie10';

interface Interno {
  texto: { (): string };
  lista: { (): Cie10Response[] };
  activo: { (): number };
  abierto: { (): boolean };
  escribir(valor: string): void;
  teclado(evento: KeyboardEvent): void;
  elegir(item: Cie10Response): void;
  abrir(): void;
  anuncio: { (): string };
  restriccion(item: Cie10Response): string | null;
}

function montar() {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(SelectorCie10);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  const elegidos: Cie10Response[] = [];
  fixture.componentInstance.elegido.subscribe((item) => elegidos.push(item));
  return { fixture, http, elegidos, interno: fixture.componentInstance as unknown as Interno };
}

const HIPERTENSION: Cie10Response = {
  codigo: 'I10',
  descripcion: 'Hipertensión esencial (primaria)',
  muestra: true,
};
const PROSTATA: Cie10Response = {
  codigo: 'N40',
  descripcion: 'Hiperplasia de la próstata',
  sexo: 'M',
  edadMinima: 40,
  muestra: true,
};

describe('SelectorCie10', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('busca con la consulta limpia después de una pausa y muestra los resultados', () => {
    const { http, interno } = montar();

    interno.escribir('hiper');
    interno.escribir('hipertension');
    vi.advanceTimersByTime(300);

    const peticion = http.expectOne(
      (r) => r.url.endsWith('/cie10') && r.params.get('q') === 'hipertension',
    );
    expect(peticion.request.params.get('limite')).toBe('15');
    peticion.flush([HIPERTENSION, PROSTATA]);

    expect(interno.lista().map((c) => c.codigo)).toEqual(['I10', 'N40']);
    expect(interno.activo()).toBe(0);
    expect(interno.anuncio()).toBe('2 resultados disponibles');
    http.verify();
  });

  it('no consulta con menos de dos caracteres', () => {
    const { http, interno } = montar();

    interno.escribir('h');
    vi.advanceTimersByTime(500);

    http.expectNone((r) => r.url.endsWith('/cie10') && r.params.has('q'));
    expect(interno.lista()).toEqual([]);
  });

  it('oculta los códigos ya elegidos', () => {
    const { fixture, http, interno } = montar();
    fixture.componentRef.setInput('excluir', ['i10']);

    interno.escribir('hiper');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url.endsWith('/cie10')).flush([HIPERTENSION, PROSTATA]);

    expect(interno.lista().map((c) => c.codigo)).toEqual(['N40']);
  });

  it('se maneja con el teclado y emite el diagnóstico elegido', () => {
    const { http, elegidos, interno } = montar();

    interno.escribir('hiper');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url.endsWith('/cie10')).flush([HIPERTENSION, PROSTATA]);

    interno.teclado(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(interno.activo()).toBe(1);
    interno.teclado(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(interno.activo()).toBe(0);
    interno.teclado(new KeyboardEvent('keydown', { key: 'End' }));
    expect(interno.activo()).toBe(1);
    interno.teclado(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(elegidos.map((c) => c.codigo)).toEqual(['N40']);
    expect(interno.texto()).toBe('');
    expect(interno.abierto()).toBe(false);
  });

  it('cierra la lista con Escape', () => {
    const { http, interno } = montar();
    interno.escribir('hiper');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url.endsWith('/cie10')).flush([HIPERTENSION]);
    expect(interno.abierto()).toBe(true);

    interno.teclado(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(interno.abierto()).toBe(false);
  });

  it('anuncia cuando no hay coincidencias y cuando falla la consulta', () => {
    const { http, interno } = montar();

    interno.escribir('zzzz');
    vi.advanceTimersByTime(300);
    http.expectOne((r) => r.url.endsWith('/cie10')).flush([]);
    expect(interno.anuncio()).toBe('Sin resultados');

    interno.escribir('zzzzz');
    vi.advanceTimersByTime(300);
    http
      .expectOne((r) => r.url.endsWith('/cie10'))
      .flush('error', { status: 500, statusText: 'Error' });
    expect(interno.anuncio()).toBe('No se pudo consultar el catálogo');
  });

  it('sugiere los diagnósticos frecuentes del médico al abrir con el campo vacío', () => {
    const { http, interno } = montar();

    interno.abrir();
    http
      .expectOne((r) => r.url.endsWith('/cie10/mis-frecuentes'))
      .flush([{ codigo: 'I10', descripcion: 'Hipertensión esencial (primaria)', casos: 12 }]);

    expect(interno.lista().map((c) => c.codigo)).toEqual(['I10']);
    expect(interno.anuncio()).toBe('1 diagnósticos frecuentes disponibles');
  });

  it('describe las restricciones de sexo y edad del código', () => {
    const { interno } = montar();

    expect(interno.restriccion(PROSTATA)).toBe('Solo hombres · Desde 40 años');
    expect(
      interno.restriccion({
        codigo: 'O80',
        descripcion: 'Parto',
        sexo: 'F',
        edadMinima: 10,
        edadMaxima: 55,
        muestra: true,
      }),
    ).toBe('Solo mujeres · 10 a 55 años');
    expect(
      interno.restriccion({ codigo: 'Z00.1', descripcion: 'Niño', edadMaxima: 17, muestra: true }),
    ).toBe('Hasta 17 años');
    expect(interno.restriccion(HIPERTENSION)).toBeNull();
  });
});
