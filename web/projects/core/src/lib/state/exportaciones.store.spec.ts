import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ExportacionResponse } from '../models';
import { DescargaService } from '../services/descarga.service';
import { ExportacionesStore, ResultadoExportacion, esTerminal } from './exportaciones.store';

const ID = '00000000-0000-4000-8000-000000000009';

function exportacion(estado: ExportacionResponse['estado']): ExportacionResponse {
  return {
    id: ID,
    tipo: 'reporte-ejecutivo',
    formato: 'PDF',
    estado,
    creadaEn: '2026-11-17T10:00:00',
  } as ExportacionResponse;
}

describe('ExportacionesStore', () => {
  let store: ExportacionesStore;
  let controller: HttpTestingController;
  const guardar = vi.fn();

  beforeEach(() => {
    vi.useFakeTimers();
    guardar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DescargaService, useValue: { guardar } },
      ],
    });
    store = TestBed.inject(ExportacionesStore);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function atenderListado(): void {
    controller
      .match((r) => r.method === 'GET' && r.url.endsWith('/exportaciones'))
      .forEach((r) => r.flush([]));
  }

  it('reconoce los estados terminales', () => {
    expect(esTerminal('LISTA')).toBe(true);
    expect(esTerminal('ERROR')).toBe(true);
    expect(esTerminal('EXPIRADA')).toBe(true);
    expect(esTerminal('SOLICITADA')).toBe(false);
    expect(esTerminal('PROCESANDO')).toBe(false);
  });

  it('descarga de inmediato cuando la exportación ya está lista', () => {
    let resultado: ResultadoExportacion | undefined;
    store.solicitarYEsperar({ tipo: 'reporte-ejecutivo', formato: 'PDF' }).subscribe((r) => {
      resultado = r;
    });

    controller.expectOne((r) => r.method === 'POST').flush(exportacion('LISTA'));
    atenderListado();
    controller
      .expectOne((r) => r.url.endsWith(`/exportaciones/${ID}/descargar`))
      .flush(new Blob(['%PDF']));

    expect(resultado?.estado).toBe('descargada');
    expect(guardar).toHaveBeenCalledTimes(1);
  });

  it('consulta hasta que termina y entonces descarga', () => {
    let resultado: ResultadoExportacion | undefined;
    store.solicitarYEsperar({ tipo: 'kardex', formato: 'XLSX' }).subscribe((r) => {
      resultado = r;
    });

    controller.expectOne((r) => r.method === 'POST').flush(exportacion('SOLICITADA'));
    atenderListado();

    vi.advanceTimersByTime(1200);
    controller
      .expectOne((r) => r.url.endsWith(`/exportaciones/${ID}`))
      .flush(exportacion('PROCESANDO'));
    expect(resultado).toBeUndefined();

    vi.advanceTimersByTime(1200);
    controller.expectOne((r) => r.url.endsWith(`/exportaciones/${ID}`)).flush(exportacion('LISTA'));
    controller
      .expectOne((r) => r.url.endsWith(`/exportaciones/${ID}/descargar`))
      .flush(new Blob(['x']));

    expect(resultado?.estado).toBe('descargada');
  });

  it('informa el error del servidor sin descargar', () => {
    let resultado: ResultadoExportacion | undefined;
    store.solicitarYEsperar({ tipo: 'kardex', formato: 'XLSX' }).subscribe((r) => {
      resultado = r;
    });

    controller.expectOne((r) => r.method === 'POST').flush(exportacion('SOLICITADA'));
    atenderListado();
    vi.advanceTimersByTime(1200);
    controller.expectOne((r) => r.url.endsWith(`/exportaciones/${ID}`)).flush(exportacion('ERROR'));

    expect(resultado?.estado).toBe('error');
    expect(guardar).not.toHaveBeenCalled();
  });

  it('deja la exportación en proceso si tarda demasiado', () => {
    let resultado: ResultadoExportacion | undefined;
    store.solicitarYEsperar({ tipo: 'kardex', formato: 'XLSX' }).subscribe((r) => {
      resultado = r;
    });

    controller.expectOne((r) => r.method === 'POST').flush(exportacion('SOLICITADA'));
    atenderListado();

    vi.advanceTimersByTime(61_000);

    expect(resultado?.estado).toBe('en-proceso');
  });

  it('quita una exportación eliminada de la lista', () => {
    store.cargar();
    controller
      .expectOne((r) => r.method === 'GET')
      .flush([exportacion('LISTA'), { ...exportacion('LISTA'), id: 'otra' }]);

    store.eliminar(ID).subscribe();
    controller.expectOne((r) => r.method === 'DELETE').flush(null);

    expect(store.lista.value()?.map((e) => e.id)).toEqual(['otra']);
  });
});
