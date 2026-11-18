import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService } from 'primeng/api';

import { NotificationService, PlantillaResponse, errorInterceptor } from '@tusalud/core';

import { Plantillas } from './plantillas';

function plantilla(codigo: string, version = 1): PlantillaResponse {
  return {
    codigo,
    nombre: `Plantilla ${codigo}`,
    version,
    personalizada: version > 1,
    actualizadaPor: null,
    actualizadaEn: null,
    motivo: null,
  };
}

interface Interno {
  abrirReemplazo(p: PlantillaResponse): void;
  elegir(e: Event): void;
  reemplazar(): void;
  archivo(): File | null;
  error(): string | null;
}

describe('Plantillas', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        ConfirmationService,
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function montar() {
    const fixture = TestBed.createComponent(Plantillas);
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/plantillas'))
      .flush([plantilla('receta'), plantilla('comprobante', 2)]);
    fixture.detectChanges();
    return { fixture, c: fixture.componentInstance as unknown as Interno };
  }

  function evento(archivo: File): Event {
    return { target: { files: { item: () => archivo } } } as unknown as Event;
  }

  it('lista las plantillas del sistema', () => {
    const { fixture } = montar();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Plantilla receta');
    expect(texto).toContain('Plantilla comprobante');
  });

  it('rechaza archivos que no son JRXML o superan 512 KB', () => {
    const { c } = montar();

    c.elegir(evento(new File(['x'], 'plantilla.exe')));
    expect(c.archivo()).toBeNull();
    expect(c.error()).toBe('Elige un archivo JRXML.');

    c.elegir(evento(new File([new Uint8Array(600 * 1024)], 'grande.jrxml')));
    expect(c.archivo()).toBeNull();
    expect(c.error()).toContain('512 KB');
  });

  it('exige elegir un archivo antes de reemplazar', () => {
    const { c } = montar();
    c.abrirReemplazo(plantilla('receta'));
    c.reemplazar();

    expect(c.error()).toBe('Elige el archivo JRXML de la nueva plantilla.');
    controller.expectNone((r) => r.method === 'PUT');
  });

  it('sube la plantilla válida y avisa la nueva versión', () => {
    const { c } = montar();
    c.abrirReemplazo(plantilla('receta'));
    c.elegir(evento(new File(['<jasperReport/>'], 'receta.jrxml')));
    c.reemplazar();

    controller.expectOne((r) => r.method === 'PUT').flush(plantilla('receta', 2));

    expect(TestBed.inject(NotificationService).notifications()[0]?.message).toContain('versión 2');
  });

  it('muestra el motivo cuando el servidor rechaza la plantilla', () => {
    const { c } = montar();
    c.abrirReemplazo(plantilla('receta'));
    c.elegir(evento(new File(['<jasperReport/>'], 'receta.jrxml')));
    c.reemplazar();

    controller
      .expectOne((r) => r.method === 'PUT')
      .flush(
        { status: 400, message: 'La plantilla contiene elementos no permitidos' },
        { status: 400, statusText: 'Bad Request' },
      );

    expect(c.error()).toBe('La plantilla contiene elementos no permitidos');
  });
});
