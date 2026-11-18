import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ArchivoImg } from './archivo-img';

@Component({
  imports: [ArchivoImg],
  template: `<app-archivo-img [archivoId]="id()" alt="Foto" />`,
})
class Anfitrion {
  id = signal<string | null>(null);
}

const A = '11111111-1111-4111-8111-111111111111';
const B = '22222222-2222-4222-8222-222222222222';

describe('ArchivoImg', () => {
  const crear = vi.fn((blob: Blob) => `blob:${blob.size}`);
  const revocar = vi.fn();
  let controller: HttpTestingController;

  beforeEach(() => {
    crear.mockClear();
    revocar.mockClear();
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar }));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    vi.unstubAllGlobals();
  });

  it('descarga el archivo autenticado y lo muestra como imagen', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.componentInstance.id.set(A);
    fixture.detectChanges();
    await fixture.whenStable();

    controller.expectOne(`/api/v1/archivos/${A}`).flush(new Blob(['abc'], { type: 'image/png' }));
    fixture.detectChanges();

    const imagen = (fixture.nativeElement as HTMLElement).querySelector('img');
    expect(imagen?.getAttribute('src')).toBe('blob:3');
    expect(imagen?.getAttribute('alt')).toBe('Foto');
  });

  it('libera la URL anterior al cambiar de archivo y al destruirse', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.componentInstance.id.set(A);
    fixture.detectChanges();
    await fixture.whenStable();
    controller.expectOne(`/api/v1/archivos/${A}`).flush(new Blob(['abc']));
    fixture.detectChanges();

    fixture.componentInstance.id.set(B);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(revocar).toHaveBeenCalledWith('blob:3');
    controller.expectOne(`/api/v1/archivos/${B}`).flush(new Blob(['abcd']));

    fixture.destroy();
    expect(revocar).toHaveBeenCalledWith('blob:4');
  });

  it('muestra un marcador cuando la descarga falla y nada cuando no hay id', async () => {
    const fixture = TestBed.createComponent(Anfitrion);
    fixture.componentInstance.id.set(A);
    fixture.detectChanges();
    await fixture.whenStable();
    controller
      .expectOne(`/api/v1/archivos/${A}`)
      .flush(new Blob(['no']), { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('img')).toBeNull();
    expect(raiz.querySelector('.estado .pi-image')).not.toBeNull();

    fixture.componentInstance.id.set(null);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(raiz.querySelector('.estado.cargando')).not.toBeNull();
  });
});
