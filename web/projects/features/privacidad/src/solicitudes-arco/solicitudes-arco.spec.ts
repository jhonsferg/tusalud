import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SolicitudArco, errorInterceptor } from '@tusalud/core';

import { SolicitudesArco } from './solicitudes-arco';

interface Interno {
  abrir(s: SolicitudArco): void;
  resolver(s: SolicitudArco): void;
  error(): string | null;
  resolucion: { set(valor: string): void };
  ayudaDecision(tipo: string): string;
}

const solicitud: SolicitudArco = {
  id: 's1',
  codigo: 'ARCO-2026-00001',
  tipo: 'ACCESO',
  estado: 'RECIBIDA',
  descripcion: 'Quiero una copia de mis datos',
  campo: null,
  valorPropuesto: null,
  solicitante: 'María Pérez',
  usuarioId: 'u1',
  responsable: null,
  recibidaEn: '2026-11-19T09:00:00',
  venceEn: '2026-11-01',
  vencida: true,
  resueltaEn: null,
  resolucion: null,
  tieneExportacion: false,
};

describe('SolicitudesArco', () => {
  let controller: HttpTestingController;

  function montar() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    controller = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SolicitudesArco);
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/privacidad/gestion/solicitudes'))
      .flush([solicitud]);
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/privacidad/gestion/estado'))
      .flush({
        solicitudesAbiertas: 1,
        solicitudesVencidas: 1,
        incidentesAbiertos: 0,
        incidentesVencidos: 0,
        versionPolitica: 1,
        ultimaRetencion: null,
      });
    fixture.detectChanges();
    return { fixture, interno: fixture.componentInstance as unknown as Interno };
  }

  afterEach(() => controller.verify());

  it('marca como vencida la solicitud que pasó su plazo', () => {
    const { fixture } = montar();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('ARCO-2026-00001');
    expect(texto).toContain('María Pérez');
    expect(texto).toContain('Vencida');
    expect(texto).toContain('Sin asignar');
  });

  it('exige una respuesta al titular antes de resolver', () => {
    const { interno } = montar();

    interno.resolucion.set('corta');
    interno.resolver(solicitud);

    expect(interno.error()).toContain('al menos 10 caracteres');
  });

  it('explica el efecto de cada derecho al resolver', () => {
    const { interno } = montar();

    expect(interno.ayudaDecision('ACCESO')).toContain('exportación');
    expect(interno.ayudaDecision('CANCELACION')).toContain('historia clínica');
    expect(interno.ayudaDecision('OPOSICION')).toContain('revoca');
  });
});
