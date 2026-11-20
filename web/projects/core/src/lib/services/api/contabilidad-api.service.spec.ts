import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ContabilidadApiService } from './contabilidad-api.service';

describe('ContabilidadApiService', () => {
  let api: ContabilidadApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ContabilidadApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(method: string, sufijo: string, body: unknown = null) {
    const peticion = controller.expectOne((r) => r.method === method && r.url.endsWith(sufijo));
    expect(peticion.request.body).toEqual(body);
    return peticion;
  }

  it('gestiona el plan contable y los periodos', () => {
    api.cuentas(true).subscribe();
    const lista = controller.expectOne((r) => r.url.endsWith('/contabilidad/cuentas'));
    expect(lista.request.params.get('soloActivas')).toBe('true');
    lista.flush([]);

    const cuenta = {
      codigo: '1099',
      nombre: 'Otra',
      tipo: 'ACTIVO' as const,
      aceptaMovimientos: true,
    };
    api.crearCuenta(cuenta).subscribe();
    esperar('POST', '/contabilidad/cuentas', cuenta).flush({});

    api
      .actualizarCuenta('c1', { nombre: 'Nueva', aceptaMovimientos: true, activa: false })
      .subscribe();
    esperar('PUT', '/contabilidad/cuentas/c1', {
      nombre: 'Nueva',
      aceptaMovimientos: true,
      activa: false,
    }).flush({});

    api.abrirPeriodo(2026, 3).subscribe();
    esperar('POST', '/contabilidad/periodos', { anio: 2026, mes: 3 }).flush({});
    api.cerrarPeriodo('p1').subscribe();
    esperar('POST', '/contabilidad/periodos/p1/cerrar', {}).flush({});
    api.reabrirPeriodo('p1', 'Corrección de cierre').subscribe();
    esperar('POST', '/contabilidad/periodos/p1/reabrir', { motivo: 'Corrección de cierre' }).flush(
      {},
    );
  });

  it('gestiona los asientos y su reversa', () => {
    api.asientos({ estado: 'BORRADOR', q: 'aporte' }).subscribe();
    const lista = controller.expectOne((r) => r.url.endsWith('/contabilidad/asientos'));
    expect(lista.request.params.get('estado')).toBe('BORRADOR');
    expect(lista.request.params.get('q')).toBe('aporte');
    lista.flush([]);

    const request = {
      fecha: '2026-03-10',
      glosa: 'Aporte',
      lineas: [
        { cuentaId: 'a', debe: 10, haber: null, glosa: null },
        { cuentaId: 'b', debe: null, haber: 10, glosa: null },
      ],
    };
    api.crearAsiento(request).subscribe();
    esperar('POST', '/contabilidad/asientos', request).flush({});
    api.contabilizar('a1').subscribe();
    esperar('POST', '/contabilidad/asientos/a1/contabilizar', {}).flush({});
    api.reversar('a1', 'Error de registro').subscribe();
    esperar('POST', '/contabilidad/asientos/a1/reversar', { motivo: 'Error de registro' }).flush(
      {},
    );
    api.eliminarAsiento('a2').subscribe();
    esperar('DELETE', '/contabilidad/asientos/a2').flush(null);
  });

  it('consulta los libros, los estados y los libros electrónicos', () => {
    api.mayor('c1', '2026-03-01', '2026-03-31').subscribe();
    const mayor = controller.expectOne((r) => r.url.endsWith('/contabilidad/libros/mayor/c1'));
    expect(mayor.request.params.get('desde')).toBe('2026-03-01');
    mayor.flush({});

    api.estadoSituacion('2026-03-31').subscribe();
    const situacion = controller.expectOne((r) =>
      r.url.endsWith('/contabilidad/libros/estado-situacion'),
    );
    expect(situacion.request.params.get('al')).toBe('2026-03-31');
    situacion.flush({});

    api.libroElectronico('DIARIO', 2026, 3).subscribe();
    const libro = controller.expectOne((r) =>
      r.url.endsWith('/contabilidad/libros/electronicos/DIARIO'),
    );
    expect(libro.request.params.get('anio')).toBe('2026');
    expect(libro.request.params.get('mes')).toBe('3');
    libro.flush({});
  });

  it('administra la tesorería y la conciliación', () => {
    api.flujoCaja(6).subscribe();
    const flujo = controller.expectOne((r) => r.url.endsWith('/tesoreria/flujo-caja'));
    expect(flujo.request.params.get('semanas')).toBe('6');
    flujo.flush({});

    api.importarExtracto('t1', 'extracto.csv', 'fecha,monto').subscribe();
    esperar('POST', '/tesoreria/conciliacion/extractos', {
      cuentaId: 't1',
      archivo: 'extracto.csv',
      contenido: 'fecha,monto',
    }).flush({});

    api.sugerencias('t1').subscribe();
    esperar('GET', '/tesoreria/conciliacion/t1/sugerencias').flush([]);
    api.conciliar('l1', 'm1').subscribe();
    esperar('POST', '/tesoreria/conciliacion/conciliar', {
      lineaId: 'l1',
      movimientoId: 'm1',
    }).flush({});
    api.deshacerConciliacion('l1').subscribe();
    esperar('POST', '/tesoreria/conciliacion/lineas/l1/deshacer', {}).flush({});
    api.ignorarLinea('l2').subscribe();
    esperar('POST', '/tesoreria/conciliacion/lineas/l2/ignorar', {}).flush({});
  });

  it('actualiza plantillas y reprocesa pendientes', () => {
    const request = { nombre: 'Pago', activa: true, cuentas: [{ lineaId: 'l', cuentaId: 'c' }] };
    api.actualizarPlantilla('p1', request).subscribe();
    esperar('PUT', '/contabilidad/plantillas/p1', request).flush({});
    api.reprocesar('x1').subscribe();
    esperar('POST', '/contabilidad/pendientes/x1/reprocesar', {}).flush({});
  });
});
