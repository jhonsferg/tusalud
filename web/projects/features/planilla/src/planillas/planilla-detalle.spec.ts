import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';

import {
  AuthService,
  EstadoPlanilla,
  PlanillaDetalleResponse,
  errorInterceptor,
} from '@tusalud/core';

import { PlanillaDetalle } from './planilla-detalle';

function detalle(estado: EstadoPlanilla): PlanillaDetalleResponse {
  return {
    planilla: {
      id: 'p1',
      codigo: 'PLA-2026-10',
      anio: 2026,
      mes: 10,
      estado,
      cantidadBoletas: 1,
      totalIngresos: 3000,
      totalDescuentos: 420.33,
      totalNeto: 2579.67,
      totalAporteEmpleador: 270,
      totalRenta: 30.33,
      totalPension: 390,
      hash: 'abc',
      calculadaPor: 'Ximena Solís',
    },
    boletas: [
      {
        id: 'b1',
        numero: 'BP-2026-000001',
        empleadoId: 'e1',
        empleadoNombre: 'Ana Pérez',
        cargo: 'Médica',
        totalIngresos: 3000,
        totalDescuentos: 420.33,
        neto: 2579.67,
        banco: 'Banco de Crédito',
      },
    ],
  };
}

describe('PlanillaDetalle', () => {
  let controller: HttpTestingController;

  function montar(roles: string[], datos: PlanillaDetalleResponse): HTMLElement {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        ConfirmationService,
        MessageService,
        {
          provide: AuthService,
          useValue: {
            isAuthenticated: () => true,
            isRrhh: () => roles.includes('ROLE_RRHH'),
            isFinanzas: () => roles.includes('ROLE_FINANZAS'),
            isGerencia: () => roles.includes('ROLE_GERENCIA'),
          },
        },
      ],
    });
    controller = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PlanillaDetalle);
    fixture.componentRef.setInput('id', 'p1');
    fixture.detectChanges();
    controller
      .expectOne((r) => r.method === 'GET' && r.url.endsWith('/planilla/planillas/p1'))
      .flush(datos);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => controller.verify());

  function botones(raiz: HTMLElement): string[] {
    return Array.from(raiz.querySelectorAll('button, a')).map((b) => (b.textContent ?? '').trim());
  }

  it('recursos humanos recalcula una planilla calculada y no la aprueba', () => {
    const acciones = botones(montar(['ROLE_RRHH'], detalle('CALCULADA')));

    expect(acciones).toContain('Recalcular');
    expect(acciones).not.toContain('Aprobar');
    expect(acciones).not.toContain('Registrar pago');
  });

  it('finanzas aprueba o devuelve una planilla calculada y no la recalcula', () => {
    const acciones = botones(montar(['ROLE_FINANZAS'], detalle('CALCULADA')));

    expect(acciones).toContain('Aprobar');
    expect(acciones).toContain('Devolver');
    expect(acciones).not.toContain('Recalcular');
  });

  it('finanzas registra el pago solo cuando la planilla está aprobada y baja el archivo bancario', () => {
    const acciones = botones(montar(['ROLE_FINANZAS'], detalle('APROBADA')));

    expect(acciones).toContain('Registrar pago');
    expect(acciones).toContain('Archivo bancario');
    expect(acciones).not.toContain('Aprobar');
  });

  it('el archivo PLAME es de recursos humanos y solo con la planilla aprobada', () => {
    expect(botones(montar(['ROLE_RRHH'], detalle('CALCULADA')))).not.toContain('Archivo PLAME');
    TestBed.resetTestingModule();

    expect(botones(montar(['ROLE_RRHH'], detalle('APROBADA')))).toContain('Archivo PLAME');
  });

  it('gerencia ve los totales y no las boletas individuales ni acciones de aprobación', () => {
    const raiz = montar(['ROLE_GERENCIA'], detalle('APROBADA'));

    expect(raiz.textContent).not.toContain('Ana Pérez');
    expect(raiz.textContent).toContain('Gerencia ve los totales');
    const acciones = botones(raiz);
    expect(acciones).not.toContain('Aprobar');
    expect(acciones).not.toContain('Registrar pago');
  });

  it('una planilla pagada se puede cerrar por recursos humanos', () => {
    expect(botones(montar(['ROLE_RRHH'], detalle('PAGADA')))).toContain('Cerrar');
    TestBed.resetTestingModule();

    expect(botones(montar(['ROLE_FINANZAS'], detalle('PAGADA')))).not.toContain('Cerrar');
  });
});
