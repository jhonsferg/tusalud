import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ComprasApiService } from './compras-api.service';

describe('ComprasApiService', () => {
  let api: ComprasApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ComprasApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(method: string, sufijo: string, body: unknown = null) {
    const peticion = controller.expectOne((r) => r.method === method && r.url.endsWith(sufijo));
    expect(peticion.request.body).toEqual(body);
    return peticion;
  }

  it('gestiona las requisiciones y sus cotizaciones', () => {
    api.requisiciones({ estado: 'ENVIADA' }).subscribe();
    const lista = controller.expectOne((r) => r.url.endsWith('/compras/requisiciones'));
    expect(lista.request.params.get('estado')).toBe('ENVIADA');
    lista.flush([]);

    api.sugeridas().subscribe();
    esperar('GET', '/compras/requisiciones/sugeridas').flush([]);

    const request = {
      prioridad: 'URGENTE' as const,
      items: [{ productoId: 'p1', cantidad: 5 }],
    };
    api.crearRequisicion(request).subscribe();
    esperar('POST', '/compras/requisiciones', request).flush({});

    api.enviarRequisicion('r1').subscribe();
    esperar('POST', '/compras/requisiciones/r1/enviar', {}).flush({});

    api.rechazarRequisicion('r1', 'Sin presupuesto').subscribe();
    esperar('POST', '/compras/requisiciones/r1/rechazar', { motivo: 'Sin presupuesto' }).flush({});

    api.comparativa('r1').subscribe();
    esperar('GET', '/compras/requisiciones/r1/comparativa').flush({});

    api.seleccionarCotizacion('c1').subscribe();
    esperar('POST', '/compras/cotizaciones/c1/seleccionar', {}).flush({});
  });

  it('recorre el ciclo de la orden de compra', () => {
    api.ordenDesdeCotizacion('c1').subscribe();
    esperar('POST', '/compras/ordenes/desde-cotizacion/c1', {}).flush({});

    api.enviarAprobacion('o1').subscribe();
    esperar('POST', '/compras/ordenes/o1/enviar-aprobacion', {}).flush({});

    api.aprobarOrden('o1').subscribe();
    esperar('POST', '/compras/ordenes/o1/aprobar', {}).flush({});

    api.rechazarOrden('o1', 'Precio alto').subscribe();
    esperar('POST', '/compras/ordenes/o1/rechazar', { motivo: 'Precio alto' }).flush({});

    api.anularOrden('o1', 'Duplicada').subscribe();
    esperar('POST', '/compras/ordenes/o1/anular', { motivo: 'Duplicada' }).flush({});

    api.cerrarOrden('o1', 'No entregan más').subscribe();
    esperar('POST', '/compras/ordenes/o1/cerrar', { motivo: 'No entregan más' }).flush({});

    const recepcion = {
      guiaRemision: 'T001-1',
      items: [{ ordenItemId: 'i1', cantidadRecibida: 3 }],
    };
    api.recibir('o1', recepcion).subscribe();
    esperar('POST', '/compras/ordenes/o1/recepciones', recepcion).flush({});
  });

  it('registra comprobantes, pagos, devoluciones y créditos', () => {
    api.comprobantes({ soloVencidos: true, proveedorId: 'p1' }).subscribe();
    const lista = controller.expectOne((r) => r.url.endsWith('/compras/comprobantes'));
    expect(lista.request.params.get('soloVencidos')).toBe('true');
    expect(lista.request.params.get('proveedorId')).toBe('p1');
    lista.flush([]);

    const pago = { monto: 50, metodo: 'TRANSFERENCIA' as const, referencia: 'OP-1' };
    api.pagar('k1', pago).subscribe();
    esperar('POST', '/compras/comprobantes/k1/pagos', pago).flush({});

    api.anularComprobante('k1', 'Error').subscribe();
    esperar('POST', '/compras/comprobantes/k1/anular', { motivo: 'Error' }).flush({});

    const devolucion = {
      recepcionId: 'r1',
      motivo: 'Daño',
      items: [{ recepcionItemId: 'ri1', cantidad: 2 }],
    };
    api.devolver(devolucion).subscribe();
    esperar('POST', '/compras/devoluciones', devolucion).flush({});

    api.aplicarCredito('d1', 'k1').subscribe();
    esperar('POST', '/compras/devoluciones/d1/aplicar-credito', { comprobanteId: 'k1' }).flush({});

    api.resumenCuentasPorPagar().subscribe();
    esperar('GET', '/compras/cuentas-por-pagar/resumen').flush({});

    api.reportePorProveedor({ desde: '2026-01-01', hasta: '2026-01-31' }).subscribe();
    const reporte = controller.expectOne((r) => r.url.endsWith('/compras/reportes/por-proveedor'));
    expect(reporte.request.params.get('desde')).toBe('2026-01-01');
    reporte.flush([]);

    api.auditoria('COMPROBANTE').subscribe();
    const auditoria = controller.expectOne((r) => r.url.endsWith('/compras/auditoria'));
    expect(auditoria.request.params.get('entidad')).toBe('COMPROBANTE');
    auditoria.flush([]);
  });
});
