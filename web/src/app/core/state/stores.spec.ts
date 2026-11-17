import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';

import { createSession } from '@tusalud/testing/auth-fixtures';
import { CitaResponse, HorarioResponse, MedicoResponse } from '@tusalud/app/core/models';
import { AuthService, SESSION_STORAGE_KEY } from '@tusalud/app/core/services/auth.service';
import { AsyncState } from '@tusalud/app/core/state/async-state';
import { AuditoriaStore } from '@tusalud/app/core/state/auditoria.store';
import { CitasStore } from '@tusalud/app/core/state/citas.store';
import { EspecialidadesStore } from '@tusalud/app/core/state/especialidades.store';
import { MedicosStore, ordenarHorarios } from '@tusalud/app/core/state/medicos.store';

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

function cita(id: string, fechaHora: string, estado: CitaResponse['estado']): CitaResponse {
  return {
    id,
    fechaHora,
    estado,
    motivo: 'Control',
    monto: 150,
    medicoId: u(1),
    medicoNombre: 'Luis García',
    especialidad: 'Cardiología',
    especialidadId: u(1),
    especialidadNombre: 'Cardiología',
    precio: 150,
    consultorio: 'Consultorio 101',
    pacienteId: u(1),
    pacienteNombre: 'Juan Pérez',
    pacienteDni: '12345678',
    createdAt: '2026-10-12T20:00:00',
  };
}

function horario(
  id: string,
  diaSemana: HorarioResponse['diaSemana'],
  horaInicio: string,
): HorarioResponse {
  return { id, medicoId: u(1), diaSemana, horaInicio, horaFin: '13:00', duracionCitaMinutos: 30 };
}

describe('AsyncState', () => {
  it('expone valor, carga y error como signals', () => {
    const state = new AsyncState<number>();
    const source = new Subject<number>();
    state.load(source);
    expect(state.loading()).toBe(true);
    source.next(5);
    source.complete();
    expect(state.value()).toBe(5);
    expect(state.loading()).toBe(false);
    expect(state.hasValue()).toBe(true);

    const failing = new Subject<number>();
    state.load(failing);
    failing.error({
      status: 409,
      message: 'Conflicto',
      path: null,
      timestamp: null,
      validationErrors: {},
    });
    expect(state.error()?.status).toBe(409);
    expect(state.value()).toBe(5);

    state.reset();
    expect(state.hasValue()).toBe(false);
    expect(state.error()).toBeNull();
  });

  it('cancela la carga anterior al iniciar una nueva', () => {
    const state = new AsyncState<string>();
    const primera = new Subject<string>();
    const segunda = new Subject<string>();
    state.load(primera);
    state.load(segunda);
    primera.next('vieja');
    segunda.next('nueva');
    expect(state.value()).toBe('nueva');
  });
});

describe('stores', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(createSession(['ROLE_ADMIN'])));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('EspecialidadesStore mantiene la lista ordenada tras crear, actualizar y eliminar', () => {
    const store = TestBed.inject(EspecialidadesStore);
    store.load();
    controller.expectOne('/api/v1/especialidades').flush([
      { id: u(1), nombre: 'Cardiología' },
      { id: u(2), nombre: 'Pediatría' },
    ]);

    store.crear({ nombre: 'Dermatología', precioConsulta: 130 }).subscribe();
    controller.expectOne('/api/v1/especialidades').flush({ id: u(3), nombre: 'Dermatología' });
    expect(store.especialidades().map((e) => e.nombre)).toEqual([
      'Cardiología',
      'Dermatología',
      'Pediatría',
    ]);

    store.actualizar(u(2), { nombre: 'Alergología', precioConsulta: 120 }).subscribe();
    controller
      .expectOne(`/api/v1/especialidades/${u(2)}`)
      .flush({ id: u(2), nombre: 'Alergología' });
    expect(store.opciones()[0]).toEqual({ value: u(2), label: 'Alergología' });

    store.eliminar(u(1)).subscribe();
    controller.expectOne(`/api/v1/especialidades/${u(1)}`).flush(null);
    expect(store.especialidades().map((e) => e.id)).toEqual([u(2), u(3)]);
  });

  it('MedicosStore agrupa y ordena los horarios por día de la semana', () => {
    const store = TestBed.inject(MedicosStore);
    store.loadHorarios(u(1));
    controller
      .expectOne(`/api/v1/medicos/${u(1)}/horarios`)
      .flush([
        horario(u(1), 'FRIDAY', '08:00'),
        horario(u(2), 'MONDAY', '14:00'),
        horario(u(3), 'MONDAY', '08:00'),
      ]);

    expect(store.horariosOrdenados().map((h) => h.id)).toEqual([u(3), u(2), u(1)]);
    expect(store.horariosPorDia().map((g) => [g.dia, g.bloques.length])).toEqual([
      ['MONDAY', 2],
      ['FRIDAY', 1],
    ]);

    store.eliminarHorario(u(1), u(2)).subscribe();
    controller.expectOne(`/api/v1/medicos/${u(1)}/horarios/${u(2)}`).flush(null);
    expect(store.horariosOrdenados().map((h) => h.id)).toEqual([u(3), u(1)]);
  });

  it('MedicosStore aplica el filtro de especialidad al crear médicos', () => {
    const store = TestBed.inject(MedicosStore);
    store.load(u(2));
    controller.expectOne((req) => req.url === '/api/v1/medicos').flush([]);
    const medico = {
      id: u(9),
      especialidades: [{ id: u(1), nombre: 'Cardiología', precioConsulta: 150 }],
      apellido: 'Torres',
      nombre: 'Ana',
    } as MedicoResponse;
    store.crear({} as never).subscribe();
    controller.expectOne('/api/v1/medicos').flush(medico);
    expect(store.medicos()).toEqual([]);
    expect(store.filtroEspecialidad()).toBe(u(2));
  });

  it('CitasStore separa próximas e historial y sincroniza los cambios de estado', () => {
    const store = TestBed.inject(CitasStore);
    store.loadMisCitas({ inicio: '2026-10-01' });
    controller
      .expectOne((req) => req.url === '/api/v1/citas/mis-citas')
      .flush([
        cita(u(2), '2026-10-21T09:00:00', 'PENDIENTE'),
        cita(u(1), '2026-10-20T09:00:00', 'CONFIRMADA'),
        cita(u(3), '2026-10-01T09:00:00', 'ATENDIDA'),
      ]);

    expect(store.proximas().map((c) => c.id)).toEqual([u(1), u(2)]);
    expect(store.historial().map((c) => c.id)).toEqual([u(3)]);
    expect(store.conteoPorEstado()).toEqual({
      PENDIENTE: 1,
      CONFIRMADA: 1,
      ATENDIDA: 1,
      CANCELADA: 0,
    });

    store.cancelar(u(2)).subscribe();
    controller
      .expectOne(`/api/v1/citas/${u(2)}/cancelar`)
      .flush(cita(u(2), '2026-10-21T09:00:00', 'CANCELADA'));
    expect(store.proximas().map((c) => c.id)).toEqual([u(1)]);
    expect(store.conteoPorEstado().CANCELADA).toBe(1);
  });

  it('CitasStore carga la agenda del médico', () => {
    const store = TestBed.inject(CitasStore);
    store.loadAgenda('2026-10-20');
    controller
      .expectOne((req) => req.url === '/api/v1/medicos/me/agenda')
      .flush([
        cita(u(5), '2026-10-20T11:00:00', 'PENDIENTE'),
        cita(u(4), '2026-10-20T09:00:00', 'PENDIENTE'),
      ]);
    expect(store.citasAgenda().map((c) => c.id)).toEqual([u(4), u(5)]);
    expect(store.fechaAgenda()).toBe('2026-10-20');

    store.atender(u(4)).subscribe();
    controller
      .expectOne(`/api/v1/citas/${u(4)}/atender`)
      .flush(cita(u(4), '2026-10-20T09:00:00', 'ATENDIDA'));
    expect(store.citasAgenda()[0]?.estado).toBe('ATENDIDA');
  });

  it('AuditoriaStore completa los estados faltantes y suma totales', () => {
    const store = TestBed.inject(AuditoriaStore);
    store.loadPorEstado();
    controller.expectOne('/api/v1/admin/auditoria/citas-por-estado').flush([
      { estado: 'PENDIENTE', cantidad: 3 },
      { estado: 'ATENDIDA', cantidad: 2 },
    ]);
    expect(store.resumen()).toEqual([
      { estado: 'PENDIENTE', cantidad: 3 },
      { estado: 'CONFIRMADA', cantidad: 0 },
      { estado: 'ATENDIDA', cantidad: 2 },
      { estado: 'CANCELADA', cantidad: 0 },
    ]);
    expect(store.totalCitas()).toBe(5);

    store.loadCitas({ inicio: '2026-10-01', fin: '2026-10-31' });
    controller
      .expectOne((req) => req.url === '/api/v1/admin/auditoria/citas')
      .flush([
        cita(u(1), '2026-10-02T09:00:00', 'ATENDIDA'),
        cita(u(2), '2026-10-03T09:00:00', 'CANCELADA'),
      ]);
    expect(store.ingresosAtendidos()).toBe(150);
  });

  it('los stores se vacían al cerrar sesión', () => {
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const store = TestBed.inject(EspecialidadesStore);
    TestBed.tick();
    store.load();
    controller.expectOne('/api/v1/especialidades').flush([{ id: u(1), nombre: 'Cardiología' }]);
    expect(store.especialidades().length).toBe(1);

    TestBed.inject(AuthService).logout();
    TestBed.tick();
    controller.expectOne('/api/v1/auth/logout').flush(null);
    expect(store.especialidades()).toEqual([]);
  });
});

describe('ordenarHorarios', () => {
  it('ordena de lunes a domingo y por hora de inicio', () => {
    const ordenados = ordenarHorarios([
      horario(u(1), 'SUNDAY', '08:00'),
      horario(u(2), 'WEDNESDAY', '09:00'),
    ]);
    expect(ordenados.map((h) => h.diaSemana)).toEqual(['WEDNESDAY', 'SUNDAY']);
  });
});
