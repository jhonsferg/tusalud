import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DescargaService, NotificationService } from '@tusalud/core';

import { BotonExportar, parametrosLimpios } from './boton-exportar';

describe('parametrosLimpios', () => {
  it('descarta vacíos y convierte a texto', () => {
    expect(
      parametrosLimpios({ q: '', estado: null, desde: undefined, n: 3, activo: true, a: 'x' }),
    ).toEqual({ n: '3', activo: 'true', a: 'x' });
  });
});

describe('BotonExportar', () => {
  const guardar = vi.fn();
  let controller: HttpTestingController;

  beforeEach(() => {
    guardar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DescargaService, useValue: { guardar } },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function atenderListados(): void {
    controller
      .match((r) => r.method === 'GET' && r.url.endsWith('/exportaciones'))
      .forEach((r) => r.flush([]));
  }

  it('solicita la exportación con los filtros y descarga cuando está lista', () => {
    const fixture = TestBed.createComponent(BotonExportar);
    fixture.componentRef.setInput('tipo', 'kardex');
    fixture.componentRef.setInput('formato', 'XLSX');
    fixture.componentRef.setInput('parametros', { tipo: 'ENTRADA', desde: null });
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();

    const post = controller.expectOne((r) => r.method === 'POST');
    expect(post.request.body).toEqual({
      tipo: 'kardex',
      formato: 'XLSX',
      parametros: { tipo: 'ENTRADA' },
    });
    post.flush({
      id: 'e1',
      tipo: 'kardex',
      formato: 'XLSX',
      estado: 'LISTA',
      creadaEn: '2026-11-17T10:00:00',
    });
    atenderListados();
    controller
      .expectOne((r) => r.url.endsWith('/exportaciones/e1/descargar'))
      .flush(new Blob(['x']));

    expect(guardar).toHaveBeenCalledTimes(1);
    expect(TestBed.inject(NotificationService).notifications()[0]?.type).toBe('success');
  });

  it('muestra el error en español cuando la exportación falla', () => {
    const fixture = TestBed.createComponent(BotonExportar);
    fixture.componentRef.setInput('tipo', 'kardex');
    fixture.componentRef.setInput('formato', 'XLSX');
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    controller
      .expectOne((r) => r.method === 'POST')
      .flush({
        id: 'e2',
        tipo: 'kardex',
        formato: 'XLSX',
        estado: 'ERROR',
        error: 'El reporte supera el máximo de filas',
        creadaEn: '2026-11-17T10:00:00',
      });
    atenderListados();

    const aviso = TestBed.inject(NotificationService).notifications()[0];
    expect(aviso?.type).toBe('error');
    expect(aviso?.message).toBe('El reporte supera el máximo de filas');
    expect(guardar).not.toHaveBeenCalled();
  });
});
