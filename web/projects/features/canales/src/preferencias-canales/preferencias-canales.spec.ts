import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService } from 'primeng/api';

import { MisCanales, errorInterceptor } from '@tusalud/core';

import { PreferenciasCanales } from './preferencias-canales';

function mis(estadoSms: 'ACTIVO' | 'SIN_CONFIGURAR' = 'SIN_CONFIGURAR'): MisCanales {
  return {
    canales: [
      {
        canal: 'CORREO',
        etiqueta: 'Correo electrónico',
        destinoMascara: 'm***@tusalud.test',
        estado: 'ACTIVO',
        consentimientoEn: '2026-03-10T10:00:00',
        verificadoEn: '2026-03-10T10:01:00',
        destinoFijo: true,
      },
      {
        canal: 'SMS',
        etiqueta: 'Mensaje de texto',
        destinoMascara: estadoSms === 'ACTIVO' ? '***321' : '',
        estado: estadoSms,
        consentimientoEn: null,
        verificadoEn: null,
        destinoFijo: false,
      },
      {
        canal: 'WHATSAPP',
        etiqueta: 'WhatsApp',
        destinoMascara: '',
        estado: 'SIN_CONFIGURAR',
        consentimientoEn: null,
        verificadoEn: null,
        destinoFijo: false,
      },
    ],
    preferencias: [
      {
        tipo: 'CITA_CREADA',
        etiqueta: 'Cita registrada',
        categoria: 'Citas',
        obligatoria: false,
        activos: { CORREO: true, SMS: false, WHATSAPP: false },
      },
      {
        tipo: 'SESION_SOSPECHOSA',
        etiqueta: 'Actividad inusual',
        categoria: 'Seguridad',
        obligatoria: true,
        activos: { CORREO: true, SMS: true, WHATSAPP: true },
      },
    ],
    silencio: { activo: true, desde: '22:00:00', hasta: '07:00:00' },
  };
}

interface Interno {
  abrir(canal: MisCanales['canales'][number], paso: 'datos' | 'codigo'): void;
  enviarCodigo(): void;
  consentimiento: { set(v: boolean): void };
  errorDialogo(): string | null;
  cambiar(tipo: MisCanales['preferencias'][number], canal: 'CORREO' | 'SMS', v: boolean): void;
  sucio(): boolean;
  valor(tipo: MisCanales['preferencias'][number], canal: 'CORREO'): boolean;
  activo(canal: 'CORREO' | 'SMS' | 'WHATSAPP'): boolean;
}

describe('PreferenciasCanales', () => {
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

  function montar(datos: MisCanales = mis()) {
    const fixture = TestBed.createComponent(PreferenciasCanales);
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/canales/mis-canales'))
      .flush(datos);
    fixture.detectChanges();
    return { fixture, c: fixture.componentInstance as unknown as Interno };
  }

  it('muestra cada canal con su estado y el destino enmascarado', () => {
    const { fixture } = montar();
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Correo electrónico');
    expect(texto).toContain('m***@tusalud.test');
    expect(texto).toContain('Activo');
    expect(texto).toContain('Sin activar');
    expect(texto).toContain('nunca incluyen diagnósticos');
  });

  it('marca como bloqueadas las alertas de seguridad y los canales sin activar', () => {
    const { fixture } = montar();
    const raiz = fixture.nativeElement as HTMLElement;
    const filas = Array.from(raiz.querySelectorAll('tbody tr')).filter(
      (fila) => !fila.classList.contains('grupo'),
    );
    expect(filas.length).toBe(2);
    expect(raiz.querySelector('.candado')).not.toBeNull();
    expect(raiz.textContent).toContain('Obligatoria');
  });

  it('refleja el canal activo y habilita el guardado al cambiar una preferencia', () => {
    const { fixture, c } = montar();
    expect(c.activo('CORREO')).toBe(true);
    expect(c.activo('SMS')).toBe(false);
    expect(c.sucio()).toBe(false);

    const tipo = mis().preferencias[0];
    c.cambiar(tipo, 'CORREO', false);
    fixture.detectChanges();
    expect(c.valor(tipo, 'CORREO')).toBe(false);
    expect(c.sucio()).toBe(true);
  });

  it('exige el consentimiento y el número antes de enviar el código', () => {
    const { c } = montar();
    const sms = mis().canales[1];
    c.abrir(sms, 'datos');
    c.enviarCodigo();
    expect(c.errorDialogo()).toBe('Para continuar debes aceptar recibir avisos por este canal.');

    c.consentimiento.set(true);
    c.enviarCodigo();
    expect(c.errorDialogo()).toBe('Indica el número de teléfono.');
    controller.expectNone((r) => r.url.includes('/solicitar'));
  });

  it('envía la solicitud con el consentimiento al activar el correo', () => {
    const { c } = montar();
    c.abrir(mis().canales[0], 'datos');
    c.consentimiento.set(true);
    c.enviarCodigo();
    const peticion = controller.expectOne(
      (r) => r.method === 'POST' && r.url.endsWith('/canales/contactos/CORREO/solicitar'),
    );
    expect(peticion.request.body).toEqual({ destino: null, consentimiento: true });
    peticion.flush(mis().canales[0]);
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/canales/mis-canales'))
      .flush(mis());
  });
});
