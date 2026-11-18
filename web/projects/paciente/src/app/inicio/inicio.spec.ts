import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SESSION_STORAGE_KEY } from '@tusalud/core';
import { createSession } from '@tusalud/core/testing';
import { InicioPaciente } from './inicio';

function montar() {
  TestBed.resetTestingModule();
  localStorage.clear();
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_PACIENTE'])));
  TestBed.configureTestingModule({
    providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
  });
  const fixture = TestBed.createComponent(InicioPaciente);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  return { fixture, http, html: () => fixture.nativeElement as HTMLElement };
}

const futuro = (dias: number) => new Date(Date.now() + dias * 86_400_000).toISOString();

describe('Inicio del paciente', () => {
  it('destaca la próxima cita, avisa el pago pendiente y lista los resultados disponibles', () => {
    const { fixture, http, html } = montar();
    http
      .expectOne((r) => r.url.endsWith('/citas/mis-citas'))
      .flush([
        {
          id: '11111111-1111-4111-8111-111111111111',
          fechaHora: futuro(2),
          motivo: 'Control',
          estado: 'PENDIENTE',
          medicoNombre: 'Luis García',
          especialidadNombre: 'Cardiología',
          pagado: false,
          consultorio: 'Consultorio 101',
        },
        {
          id: '22222222-2222-4222-8222-222222222222',
          fechaHora: futuro(5),
          motivo: 'Otra',
          estado: 'CONFIRMADA',
          medicoNombre: 'Carmen López',
          especialidadNombre: 'Pediatría',
          pagado: true,
        },
      ]);
    http
      .expectOne((r) => r.url.endsWith('/historia-clinica'))
      .flush({
        paciente: { grupoSanguineo: 'O+', alergias: 'Penicilina', historialClinicoNumero: 'HC-1' },
        atenciones: [],
      });
    http
      .expectOne((r) => r.url.endsWith('/laboratorio/mis-ordenes'))
      .flush([
        { id: 'a', estado: 'COMPLETADA' },
        { id: 'b', estado: 'SOLICITADA' },
      ]);
    http.expectOne((r) => r.url.endsWith('/imagenes/mis-estudios')).flush([]);
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('Cardiología con Luis García');
    expect(texto).toContain('Pago pendiente');
    expect(texto).toContain('Penicilina');
    expect(texto).toContain('Resultados de laboratorio disponibles');
    expect(texto).toContain('Citas con pago pendiente');
    expect(html().querySelectorAll('app-timeline li').length).toBe(2);
  });

  it('muestra estados vacíos honestos cuando no hay nada que mostrar', () => {
    const { fixture, http, html } = montar();
    http.expectOne((r) => r.url.endsWith('/citas/mis-citas')).flush([]);
    http
      .expectOne((r) => r.url.endsWith('/historia-clinica'))
      .flush({ paciente: { historialClinicoNumero: 'HC-1' }, atenciones: [] });
    http.expectOne((r) => r.url.endsWith('/laboratorio/mis-ordenes')).flush([]);
    http.expectOne((r) => r.url.endsWith('/imagenes/mis-estudios')).flush([]);
    fixture.detectChanges();

    const texto = html().textContent ?? '';
    expect(texto).toContain('No tienes citas próximas');
    expect(texto).toContain('Estás al día');
    expect(texto).toContain('Sin registrar');
  });
});
