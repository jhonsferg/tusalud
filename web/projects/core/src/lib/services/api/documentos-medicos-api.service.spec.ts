import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DocumentosMedicosApiService } from './documentos-medicos-api.service';

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

describe('DocumentosMedicosApiService', () => {
  let api: DocumentosMedicosApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(DocumentosMedicosApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(
    method: string,
    url: string,
    body: unknown = null,
    params: Record<string, string> = {},
  ) {
    const peticion = controller.expectOne((r) => r.method === method && r.url === url);
    expect(peticion.request.body).toEqual(body);
    expect(
      Object.fromEntries(
        peticion.request.params.keys().map((k) => [k, peticion.request.params.get(k)]),
      ),
    ).toEqual(params);
    return peticion;
  }

  it('cubre el ciclo de vida de los documentos', () => {
    const request = {
      pacienteId: u(1),
      tipo: 'CERTIFICADO' as const,
      autorizaDiagnostico: false,
      contenido: { motivo: 'Control' },
    };
    api.crear(request).subscribe();
    esperar('POST', '/api/v1/documentos-medicos', request).flush({});
    api.epicrisisBorrador(u(2)).subscribe();
    esperar('POST', '/api/v1/documentos-medicos/epicrisis/borrador', { ingresoId: u(2) }).flush({});
    api.actualizar(u(3), { autorizaDiagnostico: true, contenido: {} }).subscribe();
    esperar('PUT', `/api/v1/documentos-medicos/${u(3)}`, {
      autorizaDiagnostico: true,
      contenido: {},
    }).flush({});
    api.emitir(u(3)).subscribe();
    esperar('POST', `/api/v1/documentos-medicos/${u(3)}/emitir`, {}).flush({});
    api.anular(u(3), 'Error de datos').subscribe();
    esperar('POST', `/api/v1/documentos-medicos/${u(3)}/anular`, {
      motivo: 'Error de datos',
    }).flush({});
    api.estadoReferencia(u(3), { estado: 'RECIBIDA' }).subscribe();
    esperar('PUT', `/api/v1/documentos-medicos/${u(3)}/referencia/estado`, {
      estado: 'RECIBIDA',
    }).flush({});
  });

  it('cubre las consultas de documentos', () => {
    api.obtener(u(3)).subscribe();
    esperar('GET', `/api/v1/documentos-medicos/${u(3)}`).flush({});
    api.mios().subscribe();
    esperar('GET', '/api/v1/documentos-medicos/mios').flush([]);
    api.emitidos().subscribe();
    esperar('GET', '/api/v1/documentos-medicos/emitidos').flush([]);
    api.delPaciente(u(1)).subscribe();
    esperar('GET', '/api/v1/documentos-medicos', null, { pacienteId: u(1) }).flush([]);
    api.resumenIngreso(u(2)).subscribe();
    esperar('GET', `/api/v1/documentos-medicos/ingreso/${u(2)}/resumen`).flush({});
  });

  it('descarga el PDF con el nombre que indica el servidor', () => {
    let nombre = '';
    api.pdf(u(3)).subscribe((archivo) => (nombre = archivo.nombre));
    const peticion = esperar('GET', `/api/v1/documentos-medicos/${u(3)}/pdf`);
    expect(peticion.request.responseType).toBe('blob');
    peticion.flush(new Blob(['%PDF']), {
      headers: { 'Content-Disposition': 'attachment; filename="CM-2026-000001.pdf"' },
    });
    expect(nombre).toContain('CM-2026-000001');
  });

  it('cubre establecimientos y plantillas de consentimiento', () => {
    const establecimiento = { nombre: 'Hospital', nivel: 'SEGUNDO_NIVEL' as const, activo: true };
    api.establecimientos().subscribe();
    esperar('GET', '/api/v1/establecimientos-salud').flush([]);
    api.crearEstablecimiento(establecimiento).subscribe();
    esperar('POST', '/api/v1/establecimientos-salud', establecimiento).flush({});
    api.actualizarEstablecimiento(u(4), establecimiento).subscribe();
    esperar('PUT', `/api/v1/establecimientos-salud/${u(4)}`, establecimiento).flush({});

    const plantilla = {
      codigo: 'ANESTESIA',
      nombre: 'Anestesia',
      texto: 'Texto',
      riesgos: 'Riesgos',
      beneficios: 'Beneficios',
      alternativas: 'Alternativas',
      requiereTestigo: false,
      vigenciaDias: 30,
      activa: true,
    };
    api.plantillasConsentimiento().subscribe();
    esperar('GET', '/api/v1/consentimientos/plantillas').flush([]);
    api.crearPlantillaConsentimiento(plantilla).subscribe();
    esperar('POST', '/api/v1/consentimientos/plantillas', plantilla).flush({});
    api.actualizarPlantillaConsentimiento(u(5), plantilla).subscribe();
    esperar('PUT', `/api/v1/consentimientos/plantillas/${u(5)}`, plantilla).flush({});
  });

  it('cubre la solicitud, la firma y la revocación de consentimientos', () => {
    const solicitud = { plantillaId: u(5), pacienteId: u(1), procedimiento: 'Sutura' };
    api.solicitarConsentimiento(solicitud).subscribe();
    esperar('POST', '/api/v1/consentimientos', solicitud).flush({});
    const firma = {
      firmaPng: 'data:image/png;base64,AAAA',
      firmanteTipo: 'PACIENTE' as const,
      nombre: 'Juan Pérez',
      dni: '12345678',
      via: 'APP_PACIENTE' as const,
    };
    api.firmarConsentimiento(u(6), firma).subscribe();
    esperar('POST', `/api/v1/consentimientos/${u(6)}/firma`, firma).flush({});
    api.revocarConsentimiento(u(6), 'Cambié de opinión').subscribe();
    esperar('POST', `/api/v1/consentimientos/${u(6)}/revocar`, {
      motivo: 'Cambié de opinión',
    }).flush({});
    api.consentimientosMios().subscribe();
    esperar('GET', '/api/v1/consentimientos/mios').flush([]);
    api.consentimientosDelPaciente(u(1)).subscribe();
    esperar('GET', '/api/v1/consentimientos', null, { pacienteId: u(1) }).flush([]);
    api.consentimiento(u(6)).subscribe();
    esperar('GET', `/api/v1/consentimientos/${u(6)}`).flush({});
  });

  it('verifica un código sin espacios y lo codifica en la URL', () => {
    api.verificar('  ABCD-EFGH-JKLM-NPQR ').subscribe();
    esperar('GET', '/api/v1/verificacion/ABCD-EFGH-JKLM-NPQR').flush({ resultado: 'VALIDO' });
  });
});
