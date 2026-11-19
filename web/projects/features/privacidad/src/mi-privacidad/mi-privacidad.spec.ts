import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { errorInterceptor } from '@tusalud/core';

import { MiPrivacidad } from './mi-privacidad';

interface Interno {
  abrirNueva(): void;
  cambiarTipo(tipo: string): void;
  enviar(): void;
  error(): string | null;
  descripcion: { set(valor: string): void };
  campo: { set(valor: string | null): void };
  valor: { set(valor: string): void };
}

class ResizeObserverFalso {
  observe(): void {}
  unobserve(): void {}
  disconnect(): void {}
}

describe('MiPrivacidad', () => {
  let controller: HttpTestingController;

  beforeAll(() => {
    globalThis.ResizeObserver ??= ResizeObserverFalso;
  });

  function montar(aceptada = true) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    controller = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MiPrivacidad);
    fixture.detectChanges();
    const responder = (sufijo: string, cuerpo: object) =>
      controller.expectOne((r) => r.method === 'GET' && r.url.endsWith(sufijo)).flush(cuerpo);
    responder('/privacidad/politica/estado', {
      versionVigente: 2,
      politicaId: 'p2',
      aceptada,
      aceptadaEn: aceptada ? '2026-11-01T10:00:00' : null,
      versionAceptada: aceptada ? 2 : 1,
    });
    responder('/privacidad/consentimientos', [
      {
        finalidadId: 'f1',
        codigo: 'ATENCION',
        nombre: 'Atención de salud',
        descripcion: 'Registrar su historia clínica',
        baseLegal: 'Relación asistencial',
        obligatoria: true,
        estado: 'NO_REQUIERE',
        desde: null,
      },
      {
        finalidadId: 'f2',
        codigo: 'MKT',
        nombre: 'Comunicaciones comerciales',
        descripcion: 'Campañas',
        baseLegal: 'Consentimiento',
        obligatoria: false,
        estado: 'SIN_REGISTRO',
        desde: null,
      },
    ]);
    responder('/privacidad/solicitudes/mias', []);
    responder('/privacidad/accesos', []);
    fixture.detectChanges();
    return { fixture, interno: fixture.componentInstance as unknown as Interno };
  }

  afterEach(() => controller.verify());

  it('distingue lo necesario de lo que el titular decide', () => {
    const { fixture } = montar();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Necesario para su atención');
    expect(texto).toContain('Sin autorizar');
    expect(texto).toContain('Autorizar');
  });

  it('avisa cuando hay una política nueva sin aceptar', () => {
    const { fixture } = montar(false);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Hay una política de privacidad nueva, versión 2',
    );
  });

  it('no envía una solicitud con una descripción corta', () => {
    const { interno } = montar();

    interno.abrirNueva();
    interno.descripcion.set('corta');
    interno.enviar();

    expect(interno.error()).toContain('al menos 10 caracteres');
  });

  it('exige el valor correcto cuando se rectifica un dato', () => {
    const { interno } = montar();

    interno.abrirNueva();
    interno.cambiarTipo('RECTIFICACION');
    interno.campo.set('TELEFONO');
    interno.valor.set('');
    interno.descripcion.set('Mi teléfono está mal registrado');
    interno.enviar();

    expect(interno.error()).toContain('valor correcto');
  });

  it('al elegir cancelación consulta qué se conserva antes de enviar', () => {
    const { interno } = montar();

    interno.abrirNueva();
    interno.cambiarTipo('CANCELACION');

    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/privacidad/cancelacion/previa'))
      .flush({
        hayBloqueos: true,
        categorias: [
          {
            categoria: 'Historia clínica y citas',
            estado: 'BLOQUEADO',
            motivo: 'Se conserva por norma',
            baseLegal: 'Por confirmar',
          },
        ],
      });
  });
});
