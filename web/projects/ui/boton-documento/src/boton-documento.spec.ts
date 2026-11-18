import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DescargaService } from '@tusalud/core';

import { BotonDocumento } from './boton-documento';

const ID = '00000000-0000-4000-8000-000000000001';

describe('BotonDocumento', () => {
  const guardar = vi.fn();
  const abrir = vi.fn();
  let controller: HttpTestingController;

  beforeEach(() => {
    guardar.mockClear();
    abrir.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DescargaService, useValue: { guardar, abrir } },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function montar(accion: 'descargar' | 'imprimir') {
    const fixture = TestBed.createComponent(BotonDocumento);
    fixture.componentRef.setInput('tipo', 'receta');
    fixture.componentRef.setInput('registro', ID);
    fixture.componentRef.setInput('accion', accion);
    fixture.detectChanges();
    const boton = (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLElement;
    return { fixture, boton };
  }

  it('descarga el PDF con una petición autenticada y sin token en la URL', () => {
    const { boton } = montar('descargar');
    boton.click();

    const peticion = controller.expectOne((r) => r.url.endsWith(`/documentos/receta/${ID}`));
    expect(peticion.request.urlWithParams).not.toMatch(/token|access/i);
    peticion.flush(new Blob(['%PDF']));

    expect(guardar).toHaveBeenCalledTimes(1);
    expect(abrir).not.toHaveBeenCalled();
  });

  it('abre el PDF para imprimir cuando se pide imprimir', () => {
    const { boton } = montar('imprimir');
    boton.click();
    controller.expectOne((r) => r.url.endsWith(`/documentos/receta/${ID}`)).flush(new Blob(['x']));

    expect(abrir).toHaveBeenCalledTimes(1);
    expect(guardar).not.toHaveBeenCalled();
  });

  it('ignora clics mientras genera y se libera si falla', () => {
    const { boton, fixture } = montar('descargar');
    boton.click();
    boton.click();

    controller
      .expectOne((r) => r.url.endsWith(`/documentos/receta/${ID}`))
      .flush(new Blob(['{}'], { type: 'application/json' }), {
        status: 403,
        statusText: 'Forbidden',
      });
    fixture.detectChanges();

    expect(guardar).not.toHaveBeenCalled();
    boton.click();
    controller.expectOne((r) => r.url.endsWith(`/documentos/receta/${ID}`)).flush(new Blob(['x']));
    expect(guardar).toHaveBeenCalledTimes(1);
  });

  it('no genera si está deshabilitado', () => {
    const fixture = TestBed.createComponent(BotonDocumento);
    fixture.componentRef.setInput('tipo', 'receta');
    fixture.componentRef.setInput('registro', ID);
    fixture.componentRef.setInput('deshabilitado', true);
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector('button')?.click();
    controller.expectNone(() => true);
  });
});
