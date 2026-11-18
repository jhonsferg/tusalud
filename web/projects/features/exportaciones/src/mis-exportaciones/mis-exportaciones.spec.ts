import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ConfirmationService } from 'primeng/api';

import { DescargaService, ExportacionResponse } from '@tusalud/core';

import { MisExportaciones } from './mis-exportaciones';

function fila(
  id: string,
  estado: ExportacionResponse['estado'],
  extra: Partial<ExportacionResponse> = {},
): ExportacionResponse {
  return {
    id,
    tipo: 'kardex',
    formato: 'XLSX',
    estado,
    nombre: `kardex-${id}.xlsx`,
    tamano: 2048,
    error: null,
    creadaEn: '2026-11-17T10:00:00',
    terminadaEn: null,
    expiraEn: '2026-11-24T10:00:00',
    ...extra,
  };
}

describe('MisExportaciones', () => {
  const guardar = vi.fn();
  let controller: HttpTestingController;

  beforeEach(() => {
    guardar.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ConfirmationService,
        { provide: DescargaService, useValue: { guardar } },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function montar(items: ExportacionResponse[]) {
    const fixture = TestBed.createComponent(MisExportaciones);
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/exportaciones'))
      .flush(items);
    fixture.detectChanges();
    return fixture;
  }

  it('muestra los estados en español', () => {
    const fixture = montar([
      fila('1', 'LISTA'),
      fila('2', 'PROCESANDO'),
      fila('3', 'ERROR', { error: 'El reporte supera el máximo de filas' }),
      fila('4', 'EXPIRADA'),
    ]);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Lista');
    expect(texto).toContain('Procesando');
    expect(texto).toContain('Con error');
    expect(texto).toContain('Expirada');
    expect(texto).toContain('El reporte supera el máximo de filas');
  });

  it('descarga una exportación lista con petición autenticada', () => {
    const fixture = montar([fila('1', 'LISTA')]);
    const botones = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'td.acciones button',
    );

    expect(botones.length).toBeGreaterThan(0);
    botones[0].click();
    controller
      .expectOne((r) => r.url.endsWith('/exportaciones/1/descargar'))
      .flush(new Blob(['x']));

    expect(guardar).toHaveBeenCalledTimes(1);
  });

  it('no ofrece descargar una exportación con error', () => {
    const fixture = montar([fila('3', 'ERROR', { error: 'falló' })]);
    const botones = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>(
      'td.acciones button[aria-label*="escargar"]',
    );
    expect(botones.length).toBe(0);
  });
});
