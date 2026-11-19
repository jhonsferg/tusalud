import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CanalesApiService } from './canales-api.service';

describe('CanalesApiService', () => {
  let api: CanalesApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CanalesApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function esperar(method: string, sufijo: string, body: unknown = null) {
    const peticion = controller.expectOne((r) => r.method === method && r.url.endsWith(sufijo));
    expect(peticion.request.body).toEqual(body);
    return peticion;
  }

  it('consulta y activa los canales del usuario', () => {
    api.misCanales().subscribe();
    esperar('GET', '/canales/mis-canales').flush({});

    api.solicitar('SMS', { destino: '+51987654321', consentimiento: true }).subscribe();
    esperar('POST', '/canales/contactos/SMS/solicitar', {
      destino: '+51987654321',
      consentimiento: true,
    }).flush({});

    api.verificar('CORREO', '123456').subscribe();
    esperar('POST', '/canales/contactos/CORREO/verificar', { codigo: '123456' }).flush({});

    api.revocar('WHATSAPP').subscribe();
    esperar('DELETE', '/canales/contactos/WHATSAPP').flush({});
  });

  it('guarda las preferencias y la ventana de silencio', () => {
    const request = {
      preferencias: [{ tipo: 'CITA_CREADA' as const, canal: 'CORREO' as const, activo: false }],
      silencio: { activo: true, desde: '22:00', hasta: '07:00' },
    };
    api.guardarPreferencias(request).subscribe();
    esperar('PUT', '/canales/preferencias', request).flush({});
  });

  it('da de baja con el token firmado en la query', () => {
    api.darDeBaja('abc.def').subscribe();
    const peticion = controller.expectOne(
      (r) => r.method === 'POST' && r.url.endsWith('/canales/baja'),
    );
    expect(peticion.request.params.get('t')).toBe('abc.def');
    peticion.flush({ canal: 'CORREO', etiqueta: 'Correo electrónico' });
  });

  it('filtra las entregas y pide el resumen', () => {
    api.entregas({ canal: 'SMS', estado: 'FALLIDO', pagina: 2, tamano: 10 }).subscribe();
    const peticion = controller.expectOne(
      (r) => r.method === 'GET' && r.url.endsWith('/canales/entregas'),
    );
    expect(peticion.request.params.get('canal')).toBe('SMS');
    expect(peticion.request.params.get('estado')).toBe('FALLIDO');
    expect(peticion.request.params.get('pagina')).toBe('2');
    expect(peticion.request.params.get('tamano')).toBe('10');
    expect(peticion.request.params.has('desde')).toBe(false);
    peticion.flush({});

    api.resumenEntregas(30).subscribe();
    const resumen = controller.expectOne(
      (r) => r.method === 'GET' && r.url.endsWith('/canales/entregas/resumen'),
    );
    expect(resumen.request.params.get('dias')).toBe('30');
    resumen.flush({});

    api.reintentar('id-1').subscribe();
    esperar('POST', '/canales/entregas/id-1/reintentar').flush({});
  });

  it('administra las plantillas de mensajes', () => {
    api.plantillas().subscribe();
    esperar('GET', '/admin/canales/plantillas').flush({});

    const request = { asunto: 'Asunto', cuerpo: 'Hola {{nombre}}', motivo: 'Mejora' };
    api.actualizarPlantilla('CITA_CREADA', 'CORREO', request).subscribe();
    esperar('PUT', '/admin/canales/plantillas/CITA_CREADA/CORREO', request).flush({});

    api.restaurarPlantilla('CITA_CREADA', 'CORREO').subscribe();
    esperar('DELETE', '/admin/canales/plantillas/CITA_CREADA/CORREO').flush({});

    api.historialPlantilla('CITA_CREADA', 'SMS').subscribe();
    esperar('GET', '/admin/canales/plantillas/CITA_CREADA/SMS/historial').flush([]);

    api.proveedores().subscribe();
    esperar('GET', '/admin/canales/proveedores').flush({});
  });

  it('consulta el estado y el historial de respaldos', () => {
    api.estadoRespaldos().subscribe();
    esperar('GET', '/respaldos/estado').flush({});

    api.respaldos('PRUEBA_RESTAURACION', 5).subscribe();
    const peticion = controller.expectOne(
      (r) => r.method === 'GET' && r.url.endsWith('/respaldos'),
    );
    expect(peticion.request.params.get('tipo')).toBe('PRUEBA_RESTAURACION');
    expect(peticion.request.params.get('limite')).toBe('5');
    peticion.flush([]);
  });
});
