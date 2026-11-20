import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import {
  AuthService,
  EstadoOrdenCompra,
  NivelAprobacion,
  OrdenCompraDetalle,
  errorInterceptor,
} from '@tusalud/core';

import { OrdenDetalleVista } from './orden-detalle';

function orden(
  estado: EstadoOrdenCompra,
  nivel: NivelAprobacion | null = 'FINANZAS',
  recepciones = 0,
): OrdenCompraDetalle {
  return {
    resumen: {
      id: 'o1',
      codigo: estado === 'BORRADOR' || estado === 'PENDIENTE_APROBACION' ? null : 'OC-2026-000001',
      estado,
      proveedorId: 'p1',
      proveedorNombre: 'Droguería Andina',
      total: 177,
      nivelAprobacion: nivel,
      creadaPorNombre: 'Pedro Ramos',
      requisicionCodigo: null,
      creadaEn: '2026-11-19T10:00:00',
      aprobadaEn: null,
      porcentajeRecibido: 0,
    },
    proveedorId: 'p1',
    proveedorRuc: '20512345671',
    afectaIgv: true,
    subtotal: 150,
    igv: 27,
    total: 177,
    condicionPagoDias: 30,
    facturado: 0,
    items: [
      {
        id: 'i1',
        productoId: 'pr1',
        productoCodigo: 'MED-1',
        productoNombre: 'Paracetamol',
        unidad: 'unidad',
        cantidad: 100,
        precioUnitario: 1.5,
        subtotal: 150,
        recibida: 0,
        rechazada: 0,
        pendiente: 100,
      },
    ],
    recepciones: Array.from({ length: recepciones }, (_, i) => ({
      id: `r${i}`,
      codigo: `REC-2026-00000${i + 1}`,
      ordenId: 'o1',
      ordenCodigo: 'OC-2026-000001',
      proveedorNombre: 'Droguería Andina',
      recibidaPorNombre: 'Pedro Ramos',
      recibidaEn: '2026-11-19T12:00:00',
      conDiscrepancia: false,
    })),
  };
}

describe('OrdenDetalleVista', () => {
  let controller: HttpTestingController;

  function montar(roles: string[], detalle: OrdenCompraDetalle) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            isAlmacen: () => roles.includes('ROLE_ALMACEN'),
            isFinanzas: () => roles.includes('ROLE_FINANZAS'),
            isGerencia: () => roles.includes('ROLE_GERENCIA'),
            isAdmin: () => roles.includes('ROLE_ADMIN'),
          },
        },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(OrdenDetalleVista);
    fixture.componentRef.setInput('id', 'o1');
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/compras/ordenes/o1'))
      .flush(detalle);
    controller
      .match((r) => r.method === 'GET' && r.url.endsWith('/compras/ordenes/o1/auditoria'))
      .forEach((p) => p.flush([]));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => controller.verify());

  function botones(raiz: HTMLElement): string[] {
    return Array.from(raiz.querySelectorAll('button, a')).map((b) => (b.textContent ?? '').trim());
  }

  it('el almacén edita y envía a aprobación una orden en borrador', () => {
    const acciones = botones(montar(['ROLE_ALMACEN'], orden('BORRADOR', null)));

    expect(acciones).toContain('Editar');
    expect(acciones).toContain('Enviar a aprobación');
    expect(acciones).toContain('Anular');
    expect(acciones).not.toContain('Aprobar orden');
  });

  it('finanzas aprueba una orden pendiente de su nivel y el almacén no', () => {
    const finanzas = botones(montar(['ROLE_FINANZAS'], orden('PENDIENTE_APROBACION')));
    expect(finanzas).toContain('Aprobar orden');
    expect(finanzas).toContain('Rechazar');
    expect(finanzas).not.toContain('Registrar recepción');
  });

  it('el almacén no ve la aprobación', () => {
    const almacen = botones(montar(['ROLE_ALMACEN'], orden('PENDIENTE_APROBACION')));
    expect(almacen).not.toContain('Aprobar orden');
    expect(almacen).not.toContain('Rechazar');
  });

  it('una orden del nivel gerencia deshabilita la aprobación de finanzas y explica el motivo', () => {
    const raiz = montar(['ROLE_FINANZAS'], orden('PENDIENTE_APROBACION', 'GERENCIA'));
    const aprobar = Array.from(raiz.querySelectorAll('button')).find((b) =>
      (b.textContent ?? '').includes('Aprobar orden'),
    );

    expect(aprobar?.disabled).toBe(true);
    expect(raiz.textContent).toContain('la aprueba la gerencia');
  });

  it('el almacén registra la recepción de una orden aprobada y no puede anularla con recepciones', () => {
    const aprobada = botones(montar(['ROLE_ALMACEN'], orden('APROBADA')));
    expect(aprobada).toContain('Registrar recepción');
    expect(aprobada).toContain('Anular');
    TestBed.resetTestingModule();

    const parcial = botones(montar(['ROLE_ALMACEN'], orden('PARCIAL', 'FINANZAS', 1)));
    expect(parcial).toContain('Registrar recepción');
    expect(parcial).toContain('Cerrar la orden');
    expect(parcial).not.toContain('Anular');
  });

  it('finanzas registra el comprobante solo cuando ya hay mercadería recibida', () => {
    const sin = botones(montar(['ROLE_FINANZAS'], orden('APROBADA')));
    expect(sin).not.toContain('Registrar comprobante del proveedor');
    TestBed.resetTestingModule();

    const con = botones(montar(['ROLE_FINANZAS'], orden('PARCIAL', 'FINANZAS', 1)));
    expect(con).toContain('Registrar comprobante del proveedor');
  });
});
