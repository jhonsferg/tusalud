import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService, DocumentoElectronicoDetalle, errorInterceptor } from '@tusalud/core';

import { CpeDetalle } from './cpe-detalle';

function detalle(estado: DocumentoElectronicoDetalle['estado']): DocumentoElectronicoDetalle {
  return {
    id: 'd1',
    tipo: 'FACTURA',
    serie: 'F001',
    numero: 15,
    numeroCompleto: 'F001-00000015',
    emitidoEn: '2026-11-19T10:00:00',
    moneda: 'PEN',
    clienteTipoDoc: 'RUC',
    clienteDoc: '20100070970',
    clienteNombre: 'Distribuidora Andina SAC',
    clienteDireccion: null,
    opGravada: 100,
    opExonerada: 0,
    opInafecta: 0,
    igv: 18,
    total: 118,
    estado,
    motivoCodigo: null,
    motivoDescripcion: null,
    digestValue: 'abc=',
    xmlSha256: null,
    cdrCodigo: estado === 'RECHAZADO' ? '2017' : '0',
    cdrDescripcion: estado === 'RECHAZADO' ? 'RUC inválido' : 'La factura ha sido aceptada',
    cdrRecibidoEn: null,
    intentos: 1,
    proximoIntento: null,
    ultimoError: null,
    motivoBaja: null,
    comprobanteId: 'c1',
    documentoReferenciaId: null,
    documentoReferenciaNumero: null,
    acreditado: 0,
    items: [
      {
        posicion: 0,
        codigo: null,
        descripcion: 'Chequeo ocupacional',
        unidad: 'ZZ',
        cantidad: 1,
        precioUnitario: 118,
        valorVenta: 100,
        afectacion: 'GRAVADO',
        igv: 18,
        total: 118,
      },
    ],
    intercambios: [],
  };
}

describe('CpeDetalle', () => {
  let controller: HttpTestingController;

  function montar(estado: DocumentoElectronicoDetalle['estado'], roles: string[]) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { isAdmin: () => roles.includes('ROLE_ADMIN'), roles: () => roles },
        },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CpeDetalle);
    fixture.componentRef.setInput('id', 'd1');
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/facturacion/documentos/d1'))
      .flush(detalle(estado));
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => controller.verify());

  function botones(raiz: HTMLElement): string[] {
    return Array.from(raiz.querySelectorAll('button')).map((b) => (b.textContent ?? '').trim());
  }

  it('un comprobante rechazado muestra el motivo y permite corregir y reenviar', () => {
    const raiz = montar('RECHAZADO', ['ROLE_FINANZAS']);

    expect(raiz.textContent).toContain('SUNAT rechazó el comprobante');
    expect(raiz.textContent).toContain('RUC inválido');
    const acciones = botones(raiz);
    expect(acciones).toContain('Corregir datos del cliente');
    expect(acciones).toContain('Reenviar');
    expect(acciones).not.toContain('Emitir nota');
    expect(acciones).not.toContain('Dar de baja');
  });

  it('un comprobante aceptado permite emitir notas y dar de baja solo a finanzas', () => {
    const raiz = montar('ACEPTADO', ['ROLE_FINANZAS']);

    const acciones = botones(raiz);
    expect(acciones).toContain('Emitir nota');
    expect(acciones).toContain('Dar de baja');
    expect(acciones).toContain('XML firmado');
    expect(acciones).not.toContain('Corregir datos del cliente');
    expect(acciones).not.toContain('Reenviar');
  });

  it('la caja no ve las acciones fiscales ni los archivos firmados', () => {
    const raiz = montar('ACEPTADO', ['ROLE_CAJA']);

    const acciones = botones(raiz);
    expect(acciones).not.toContain('Emitir nota');
    expect(acciones).not.toContain('Dar de baja');
    expect(acciones).not.toContain('XML firmado');
    expect(acciones).toContain('Ver PDF');
  });
});
