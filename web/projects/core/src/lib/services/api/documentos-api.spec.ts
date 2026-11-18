import { HttpHeaders, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DocumentosApiService, nombreDeDisposicion } from './documentos-api.service';
import { ExportacionesApiService } from './exportaciones-api.service';
import { PlantillasApiService } from './plantillas-api.service';

const ID = '00000000-0000-4000-8000-000000000001';

describe('documentos, exportaciones y plantillas', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  describe('nombreDeDisposicion', () => {
    it('usa el nombre por defecto sin cabecera', () => {
      expect(nombreDeDisposicion(null, 'x.pdf')).toBe('x.pdf');
    });

    it('lee el nombre codificado en UTF-8', () => {
      expect(
        nombreDeDisposicion("attachment; filename*=UTF-8''Constancia%20Mar%C3%ADa.pdf", 'x.pdf'),
      ).toBe('Constancia María.pdf');
    });

    it('lee el nombre simple', () => {
      expect(nombreDeDisposicion('attachment; filename="receta.pdf"', 'x.pdf')).toBe('receta.pdf');
    });

    it('ignora una codificación inválida', () => {
      expect(nombreDeDisposicion("attachment; filename*=UTF-8''%E0%A4%A", 'x.pdf')).toBe('x.pdf');
    });
  });

  it('descarga un documento como blob con el nombre del servidor', () => {
    let nombre = '';
    TestBed.inject(DocumentosApiService)
      .generar('receta', ID)
      .subscribe((archivo) => (nombre = archivo.nombre));

    const peticion = controller.expectOne((r) => r.url.endsWith(`/documentos/receta/${ID}`));
    expect(peticion.request.method).toBe('GET');
    expect(peticion.request.responseType).toBe('blob');
    expect(peticion.request.urlWithParams).not.toContain('token');
    peticion.flush(new Blob(['%PDF']), {
      headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="receta-1.pdf"' }),
    });

    expect(nombre).toBe('receta-1.pdf');
  });

  it('solicita, consulta, descarga y elimina exportaciones', () => {
    const api = TestBed.inject(ExportacionesApiService);

    api.solicitar({ tipo: 'kardex', formato: 'XLSX', parametros: { tipo: 'ENTRADA' } }).subscribe();
    const post = controller.expectOne((r) => r.method === 'POST');
    expect(post.request.url.endsWith('/exportaciones')).toBe(true);
    expect(post.request.body).toEqual({
      tipo: 'kardex',
      formato: 'XLSX',
      parametros: { tipo: 'ENTRADA' },
    });
    post.flush({});

    api.mias().subscribe();
    controller.expectOne((r) => r.method === 'GET' && r.url.endsWith('/exportaciones')).flush([]);

    api.obtener(ID).subscribe();
    controller.expectOne((r) => r.url.endsWith(`/exportaciones/${ID}`)).flush({});

    api.descargar(ID).subscribe();
    const descarga = controller.expectOne((r) => r.url.endsWith(`/exportaciones/${ID}/descargar`));
    expect(descarga.request.responseType).toBe('blob');
    descarga.flush(new Blob(['x']));

    api.eliminar(ID).subscribe();
    const baja = controller.expectOne((r) => r.method === 'DELETE');
    expect(baja.request.url.endsWith(`/exportaciones/${ID}`)).toBe(true);
    baja.flush(null);
  });

  it('gestiona las plantillas con subida multipart y restauración', () => {
    const api = TestBed.inject(PlantillasApiService);

    api.listar().subscribe();
    controller.expectOne((r) => r.url.endsWith('/plantillas')).flush([]);

    api.historial('receta').subscribe();
    controller.expectOne((r) => r.url.endsWith('/plantillas/receta/historial')).flush([]);

    api
      .reemplazar('receta', new File(['<jasperReport/>'], 'receta.jrxml'), 'ajuste de logo')
      .subscribe();
    const put = controller.expectOne((r) => r.method === 'PUT');
    expect(put.request.url.endsWith('/plantillas/receta')).toBe(true);
    const cuerpo = put.request.body as FormData;
    expect(cuerpo.get('motivo')).toBe('ajuste de logo');
    expect((cuerpo.get('archivo') as File).name).toBe('receta.jrxml');
    put.flush({});

    api.restaurar('receta', 'volver').subscribe();
    const restaurar = controller.expectOne((r) => r.method === 'POST');
    expect(restaurar.request.url.endsWith('/plantillas/receta/restaurar')).toBe(true);
    expect(restaurar.request.params.get('motivo')).toBe('volver');
    restaurar.flush({});

    api.descargar('receta').subscribe();
    controller.expectOne((r) => r.url.endsWith('/plantillas/receta/descargar')).flush(new Blob());
  });
});
