import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FacturacionApiService } from './facturacion-api.service';
import { ProteccionDatosApiService } from './proteccion-datos-api.service';

describe('FacturacionApiService', () => {
  let api: FacturacionApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(FacturacionApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(method: string, sufijo: string) {
    return controller.expectOne((r) => r.method === method && r.url.endsWith(sufijo));
  }

  it('filtra la bandeja y omite los filtros vacíos', () => {
    api.documentos({ desde: '2026-11-01', tipo: 'FACTURA', estado: null, q: '' }).subscribe();
    const peticion = esperar('GET', '/facturacion/documentos');
    expect(peticion.request.params.get('desde')).toBe('2026-11-01');
    expect(peticion.request.params.get('tipo')).toBe('FACTURA');
    expect(peticion.request.params.has('estado')).toBe(false);
    expect(peticion.request.params.has('q')).toBe(false);
    peticion.flush([]);
  });

  it('consulta el detalle y el comprobante de un cobro', () => {
    api.detalle('d1').subscribe();
    esperar('GET', '/facturacion/documentos/d1').flush({});
    api.porComprobante('c1').subscribe();
    esperar('GET', '/facturacion/documentos/por-comprobante/c1').flush({});
  });

  it('corrige el cliente, reenvía y comunica la baja con su motivo', () => {
    const cliente = {
      tipoDoc: 'RUC' as const,
      documento: '20100070970',
      nombre: 'Andina',
      direccion: null,
    };
    api.corregirCliente('d1', cliente).subscribe();
    const correccion = esperar('PUT', '/facturacion/documentos/d1/cliente');
    expect(correccion.request.body).toEqual(cliente);
    correccion.flush({});

    api.reenviar('d1').subscribe();
    esperar('POST', '/facturacion/documentos/d1/reenviar').flush({});

    api.baja('d1', 'Error en la emisión').subscribe();
    const baja = esperar('POST', '/facturacion/documentos/d1/baja');
    expect(baja.request.body).toEqual({ motivo: 'Error en la emisión' });
    baja.flush({});
  });

  it('emite una nota con sus ítems', () => {
    const nota = {
      documentoId: 'd1',
      tipo: 'NOTA_CREDITO' as const,
      motivoCodigo: '09',
      descripcion: 'Disminución en el valor',
      items: [{ descripcion: 'Descuento', cantidad: 1, precioUnitario: 10 }],
    };
    api.nota(nota).subscribe();
    const peticion = esperar('POST', '/facturacion/notas');
    expect(peticion.request.body).toEqual(nota);
    peticion.flush({});
  });

  it('administra series, configuración y resúmenes', () => {
    api.estadoSerie('s1', false).subscribe();
    expect(esperar('PATCH', '/facturacion/series/s1').request.body).toEqual({ activa: false });

    api.generarResumen('2026-11-19').subscribe();
    const resumen = esperar('POST', '/facturacion/resumenes');
    expect(resumen.request.body).toEqual({ fecha: '2026-11-19' });
    resumen.flush({});

    api.procesarCola().subscribe();
    esperar('POST', '/facturacion/cola/procesar').flush({ procesados: 2 });
  });

  it('descarga el PDF y el Excel como archivos con el nombre del servidor', () => {
    let nombre = '';
    api.pdf('d1').subscribe((a) => (nombre = a.nombre));
    const pdf = esperar('GET', '/facturacion/documentos/d1/pdf');
    expect(pdf.request.responseType).toBe('blob');
    pdf.flush(new Blob(['%PDF']), {
      headers: { 'Content-Disposition': 'attachment; filename="comprobante-B001-7.pdf"' },
    });
    expect(nombre).toBe('comprobante-B001-7.pdf');

    api.registroVentasExcel('2026-11-01', '2026-11-30').subscribe();
    const excel = esperar('GET', '/facturacion/registro-ventas/excel');
    expect(excel.request.params.get('hasta')).toBe('2026-11-30');
    excel.flush(new Blob(['PK']));
  });
});

describe('ProteccionDatosApiService', () => {
  let api: ProteccionDatosApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(ProteccionDatosApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(method: string, sufijo: string) {
    return controller.expectOne((r) => r.method === method && r.url.endsWith(sufijo));
  }

  it('consulta la política pública y registra su aceptación', () => {
    api.politicaVigente().subscribe();
    esperar('GET', '/privacidad/politica').flush({});
    api.aceptarPolitica().subscribe();
    esperar('POST', '/privacidad/politica/aceptar').flush({});
  });

  it('otorga y revoca consentimientos con su motivo', () => {
    api.otorgar('f1').subscribe();
    esperar('POST', '/privacidad/consentimientos/f1/otorgar').flush({});
    api.revocar('f1', 'Ya no deseo recibirlas').subscribe();
    const revocacion = esperar('POST', '/privacidad/consentimientos/f1/revocar');
    expect(revocacion.request.body).toEqual({ motivo: 'Ya no deseo recibirlas' });
    revocacion.flush({});
  });

  it('registra solicitudes ARCO y descarga la exportación', () => {
    const solicitud = {
      tipo: 'RECTIFICACION' as const,
      descripcion: 'Mi teléfono está mal registrado',
      campo: 'TELEFONO' as const,
      valorPropuesto: '999111222',
      finalidadId: null,
    };
    api.solicitar(solicitud).subscribe();
    const peticion = esperar('POST', '/privacidad/solicitudes');
    expect(peticion.request.body).toEqual(solicitud);
    peticion.flush({});

    let nombre = '';
    api.exportacion('s1').subscribe((a) => (nombre = a.nombre));
    esperar('GET', '/privacidad/solicitudes/s1/exportacion').flush(new Blob(['{}']), {
      headers: { 'Content-Disposition': 'attachment; filename="mis-datos-s1.json"' },
    });
    expect(nombre).toBe('mis-datos-s1.json');
  });

  it('usa las rutas de gestión para la bandeja, la resolución y los incidentes', () => {
    api.bandeja('RECIBIDA', null).subscribe();
    const bandeja = esperar('GET', '/privacidad/gestion/solicitudes');
    expect(bandeja.request.params.get('estado')).toBe('RECIBIDA');
    expect(bandeja.request.params.has('tipo')).toBe(false);
    bandeja.flush([]);

    api
      .resolver('s1', { decision: 'ATENDER', resolucion: 'Se adjunta la copia de sus datos' })
      .subscribe();
    esperar('POST', '/privacidad/gestion/solicitudes/s1/resolver').flush({});

    api.guardarBanco(null, {} as never).subscribe();
    esperar('POST', '/privacidad/gestion/bancos').flush({});
    api.guardarBanco('b1', {} as never).subscribe();
    esperar('PUT', '/privacidad/gestion/bancos/b1').flush({});

    api.actualizarRetencion('r1', 30, true).subscribe();
    expect(esperar('PUT', '/privacidad/gestion/retencion/r1').request.body).toEqual({
      plazoDias: 30,
      activa: true,
    });

    api.ejecutarRetencion().subscribe();
    esperar('POST', '/privacidad/gestion/retencion/ejecutar').flush([]);
    api.registrarIncidente({} as never).subscribe();
    esperar('POST', '/privacidad/gestion/incidentes').flush({});
  });
});
