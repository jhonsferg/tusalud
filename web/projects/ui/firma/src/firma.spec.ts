import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { Firma } from './firma';

describe('Firma', () => {
  beforeEach(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  });

  afterEach(() => vi.restoreAllMocks());

  function montar() {
    TestBed.configureTestingModule({});
    const fixture = TestBed.createComponent(Firma);
    fixture.detectChanges();
    return fixture;
  }

  it('empieza vacía y no exporta imagen', () => {
    const fixture = montar();
    expect(fixture.componentInstance.vacia()).toBe(true);
    expect(fixture.componentInstance.exportarPng()).toBeNull();
  });

  it('muestra la ayuda y expone un nombre accesible', () => {
    const fixture = montar();
    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('Firme aquí');
    expect(elemento.querySelector('canvas')?.getAttribute('aria-label')).toContain('firma');
  });

  it('limpiar y deshacer no fallan sin trazos', () => {
    const fixture = montar();
    expect(() => {
      fixture.componentInstance.deshacer();
      fixture.componentInstance.limpiar();
    }).not.toThrow();
    expect(fixture.componentInstance.vacia()).toBe(true);
  });
});
