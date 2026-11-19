import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService } from 'primeng/api';

import { PlantillaMensaje, PlantillasMensaje, errorInterceptor } from '@tusalud/core';

import { PlantillasMensajeAdmin, vistaPrevia } from './plantillas-mensaje';

function plantilla(
  canal: PlantillaMensaje['canal'],
  version = 0,
  etiqueta = 'Cita registrada',
): PlantillaMensaje {
  return {
    tipo: 'CITA_CREADA',
    etiqueta,
    categoria: 'Citas',
    canal,
    asunto: canal === 'CORREO' ? 'Su cita fue registrada' : null,
    cuerpo: 'Hola {{nombre}}, ingrese a {{enlace}}',
    version,
    personalizada: version > 0,
    actualizadaEn: null,
    actualizadaPor: null,
    motivo: null,
  };
}

interface Interno {
  canal: { set(v: string | null): void };
  visibles(): PlantillaMensaje[];
  editar(p: PlantillaMensaje): void;
  guardar(): void;
  error(): string | null;
  motivo: { set(v: string): void };
  previaCuerpo(): string;
  marcador(v: string): string;
}

describe('vistaPrevia', () => {
  it('reemplaza solo las variables conocidas', () => {
    expect(vistaPrevia('Hola {{nombre}} en {{ centro }} {{otra}}')).toBe('Hola María en TuSalud ');
    expect(vistaPrevia(null)).toBe('');
  });
});

describe('PlantillasMensajeAdmin', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        ConfirmationService,
      ],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  function montar() {
    const fixture = TestBed.createComponent(PlantillasMensajeAdmin);
    fixture.detectChanges();
    const datos: PlantillasMensaje = {
      variables: ['centro', 'enlace', 'nombre'],
      plantillas: [plantilla('CORREO'), plantilla('SMS', 2), plantilla('WHATSAPP')],
    };
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/admin/canales/plantillas'))
      .flush(datos);
    fixture.detectChanges();
    return { fixture, c: fixture.componentInstance as unknown as Interno };
  }

  it('lista las plantillas y marca las personalizadas', () => {
    const { fixture } = montar();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Cita registrada');
    expect(texto).toContain('v2');
    expect(texto).toContain('Personalizada');
  });

  it('filtra por canal', () => {
    const { c } = montar();
    expect(c.visibles().length).toBe(3);
    c.canal.set('SMS');
    expect(c.visibles().map((p) => p.canal)).toEqual(['SMS']);
  });

  it('exige un motivo y publica la versión con el canal correcto', () => {
    const { c } = montar();
    c.editar(plantilla('SMS', 2));
    c.guardar();
    expect(c.error()).toBe('Indica el motivo del cambio, mínimo 5 caracteres.');

    c.motivo.set('Mejora de redacción');
    c.guardar();
    const peticion = controller.expectOne(
      (r) => r.method === 'PUT' && r.url.endsWith('/admin/canales/plantillas/CITA_CREADA/SMS'),
    );
    expect(peticion.request.body).toEqual({
      asunto: null,
      cuerpo: 'Hola {{nombre}}, ingrese a {{enlace}}',
      motivo: 'Mejora de redacción',
    });
    peticion.flush(plantilla('SMS', 3));
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/admin/canales/plantillas'))
      .flush({ variables: [], plantillas: [] });
  });

  it('calcula la vista previa con datos de muestra', () => {
    const { c } = montar();
    c.editar(plantilla('CORREO'));
    expect(c.previaCuerpo()).toBe('Hola María, ingrese a https://tusalud.example');
    expect(c.marcador('nombre')).toBe('{{nombre}}');
  });
});
