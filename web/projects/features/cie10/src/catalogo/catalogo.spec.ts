import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Cie10ImportacionResponse, Cie10ResumenResponse } from '@tusalud/core';

import { CatalogoCie10 } from './catalogo';

interface Interno {
  archivo: { set(valor: File | null): void };
  vista: { set(valor: Cie10ImportacionResponse | null): void };
  etiqueta: { set(valor: string): void };
  omitirErrores: { set(valor: boolean): void };
  puedeImportar: { (): boolean };
  restriccion(item: {
    codigo: string;
    descripcion: string;
    muestra: boolean;
    sexo?: 'F' | 'M';
    edadMinima?: number;
  }): string;
}

const RESUMEN: Cie10ResumenResponse = {
  totalActivos: 263,
  totalMuestra: 263,
  soloMuestra: true,
  versionVigente: null,
  versiones: [],
};

function vista(parcial: Partial<Cie10ImportacionResponse>): Cie10ImportacionResponse {
  return {
    aplicada: false,
    filas: 2,
    validas: 2,
    altas: 2,
    cambios: 0,
    sinCambios: 0,
    desactivados: 0,
    totalErrores: 0,
    errores: [],
    muestra: [],
    ...parcial,
  };
}

function montar() {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(CatalogoCie10);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne((r) => r.url.endsWith('/admin/cie10/resumen')).flush(RESUMEN);
  fixture.detectChanges();
  return { fixture, http, interno: fixture.componentInstance as unknown as Interno };
}

describe('CatalogoCie10', () => {
  it('avisa que el catálogo cargado es solo una muestra', () => {
    const { fixture } = montar();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('muestra curada');
  });

  it('solo permite importar con vista previa válida, etiqueta y archivo', () => {
    const { interno } = montar();
    const archivo = new File(['codigo'], 'catalogo.csv', { type: 'text/csv' });

    expect(interno.puedeImportar()).toBe(false);
    interno.archivo.set(archivo);
    interno.vista.set(vista({}));
    expect(interno.puedeImportar()).toBe(false);
    interno.etiqueta.set('CIE-10 2026');
    expect(interno.puedeImportar()).toBe(true);
  });

  it('con errores exige omitirlos y bloquea una vista sin filas válidas', () => {
    const { interno } = montar();
    interno.archivo.set(new File(['codigo'], 'catalogo.csv'));
    interno.etiqueta.set('CIE-10 2026');

    interno.vista.set(vista({ totalErrores: 1 }));
    expect(interno.puedeImportar()).toBe(false);
    interno.omitirErrores.set(true);
    expect(interno.puedeImportar()).toBe(true);

    interno.vista.set(vista({ validas: 0, totalErrores: 2 }));
    expect(interno.puedeImportar()).toBe(false);
  });

  it('describe a quién aplica un código', () => {
    const { interno } = montar();

    expect(
      interno.restriccion({
        codigo: 'N40',
        descripcion: 'Próstata',
        muestra: true,
        sexo: 'M',
        edadMinima: 40,
      }),
    ).toBe('Solo hombres · Desde 40 años');
    expect(interno.restriccion({ codigo: 'I10', descripcion: 'Hipertensión', muestra: true })).toBe(
      '',
    );
  });
});
