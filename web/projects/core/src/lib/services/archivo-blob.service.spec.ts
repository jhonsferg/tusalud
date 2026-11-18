import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ArchivoBlobService } from './archivo-blob.service';

describe('ArchivoBlobService', () => {
  let servicio: ArchivoBlobService;
  let controller: HttpTestingController;
  const crear = vi.fn(() => 'blob:http://localhost/abc');
  const revocar = vi.fn();

  beforeEach(() => {
    crear.mockClear();
    revocar.mockClear();
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: crear, revokeObjectURL: revocar }));
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ArchivoBlobService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
    vi.unstubAllGlobals();
  });

  it('descarga el archivo autenticado y entrega una URL de blob', () => {
    let resultado: { url: string; tipo: string } | null = null;
    servicio.cargar('11111111-1111-4111-8111-111111111111').subscribe((blob) => (resultado = blob));

    const peticion = controller.expectOne('/api/v1/archivos/11111111-1111-4111-8111-111111111111');
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['x'], { type: 'image/png' }));

    expect(resultado).toEqual(
      expect.objectContaining({ url: 'blob:http://localhost/abc', tipo: 'image/png' }),
    );
    expect(crear).toHaveBeenCalledTimes(1);
  });

  it('libera la URL una sola vez', () => {
    const blob = servicio.crear(new Blob(['x'], { type: 'image/png' }));

    blob.liberar();
    blob.liberar();

    expect(revocar).toHaveBeenCalledTimes(1);
    expect(revocar).toHaveBeenCalledWith('blob:http://localhost/abc');
  });
});
