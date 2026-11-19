import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { EstadoRespaldos, RespaldoEjecucion, errorInterceptor } from '@tusalud/core';

import { Respaldos, formatearBytes } from './respaldos';

function ejecucion(tipo: RespaldoEjecucion['tipo'], estado: RespaldoEjecucion['estado']) {
  return {
    id: `id-${tipo}-${estado}`,
    tipo,
    estado,
    alcance: 'COMPLETO',
    categoria: tipo === 'RESPALDO' ? 'DIARIO' : null,
    iniciadoEn: '2026-03-10T02:00:00',
    finalizadoEn: '2026-03-10T02:01:10',
    duracionSeg: 70,
    bytesTotal: 5 * 1024 * 1024,
    archivo: 'diario/20260310-020000',
    checksum: 'a'.repeat(64),
    migracionesValidadas: 24,
    tablasValidadas: 80,
    objetosAlmacen: 3,
    copiaRemota: false,
    detalle: 'Base y almacén respaldados',
  } satisfies RespaldoEjecucion;
}

function estado(semaforo: EstadoRespaldos['semaforo'], extra: Partial<EstadoRespaldos> = {}) {
  return {
    ultimoRespaldoExitoso: ejecucion('RESPALDO', 'EXITOSO'),
    ultimoRespaldoFallido: null,
    ultimaPruebaExitosa: ejecucion('PRUEBA_RESTAURACION', 'EXITOSO'),
    ultimaPrueba: ejecucion('PRUEBA_RESTAURACION', 'EXITOSO'),
    horasDesdeUltimoRespaldo: 3,
    diasDesdeUltimaPrueba: 1,
    respaldoVencido: false,
    pruebaVencida: false,
    maxHorasSinRespaldo: 26,
    maxDiasSinPrueba: 8,
    rpoObjetivoHoras: 24,
    rtoObjetivoMinutos: 60,
    semaforo,
    ...extra,
  } satisfies EstadoRespaldos;
}

describe('formatearBytes', () => {
  it('convierte a la unidad legible', () => {
    expect(formatearBytes(null)).toBe('—');
    expect(formatearBytes(512)).toBe('512 B');
    expect(formatearBytes(2048)).toBe('2.0 KB');
    expect(formatearBytes(5 * 1024 * 1024)).toBe('5.0 MB');
    expect(formatearBytes(3 * 1024 ** 3)).toBe('3.0 GB');
  });
});

describe('Respaldos', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function montar(datos: EstadoRespaldos) {
    const fixture = TestBed.createComponent(Respaldos);
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/respaldos/estado'))
      .flush(datos);
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/respaldos'))
      .flush([ejecucion('RESPALDO', 'EXITOSO'), ejecucion('PRUEBA_RESTAURACION', 'FALLIDO')]);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('muestra el semáforo verde y los objetivos de recuperación', () => {
    const raiz = montar(estado('VERDE'));
    expect(raiz.querySelector('[data-testid="semaforo"]')?.textContent).toContain(
      'Respaldos al día',
    );
    expect(raiz.querySelector('[data-semaforo="VERDE"]')).not.toBeNull();
    expect(raiz.textContent).toContain('RPO 24 h');
    expect(raiz.textContent).toContain('RTO 60 min');
    expect(raiz.textContent).toContain('Exitoso');
    expect(raiz.textContent).toContain('Fallido');
  });

  it('avisa cuando el respaldo está vencido', () => {
    const raiz = montar(estado('ROJO', { respaldoVencido: true, horasDesdeUltimoRespaldo: 40 }));
    expect(raiz.querySelector('[data-semaforo="ROJO"]')).not.toBeNull();
    expect(raiz.textContent).toContain('Hace más de 26 horas');
    expect(raiz.textContent).toContain('Respaldos en riesgo');
  });

  it('pide atención cuando falta probar la restauración', () => {
    const raiz = montar(estado('AMARILLO', { pruebaVencida: true }));
    expect(raiz.querySelector('[data-semaforo="AMARILLO"]')).not.toBeNull();
    expect(raiz.textContent).toContain('Falta una prueba de restauración');
  });
});
