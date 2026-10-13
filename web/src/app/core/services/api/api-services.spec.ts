import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuditoriaApiService } from './auditoria-api.service';
import { AuthApiService } from './auth-api.service';
import { CitasApiService } from './citas-api.service';
import { EspecialidadesApiService } from './especialidades-api.service';
import { MedicosApiService } from './medicos-api.service';

interface Expected {
  method: string;
  url: string;
  params?: Record<string, string>;
  body?: unknown;
}

describe('servicios de la API', () => {
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  function expectRequest({ method, url, params = {}, body = null }: Expected): void {
    const request = controller.expectOne((req) => req.method === method && req.url === url);
    expect(
      Object.fromEntries(
        request.request.params.keys().map((k) => [k, request.request.params.get(k)]),
      ),
    ).toEqual(params);
    expect(request.request.body).toEqual(body);
    request.flush(method === 'DELETE' ? null : {});
  }

  it('AuthApiService cubre login y registro', () => {
    const api = TestBed.inject(AuthApiService);
    const login = { email: 'admin@tusalud.com', password: 'Admin123!' };
    const registro = {
      nombre: 'Rosa',
      apellido: 'Quispe',
      email: 'rosa@correo.com',
      password: 'Paciente123!',
      dni: '12345678',
      fechaNacimiento: '1995-03-10',
    };
    api.login(login).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/auth/login', body: login });
    api.register(registro).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/auth/register', body: registro });
  });

  it('EspecialidadesApiService cubre el CRUD completo', () => {
    const api = TestBed.inject(EspecialidadesApiService);
    const body = { nombre: 'Neurología', descripcion: 'Sistema nervioso' };
    api.listar().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/especialidades' });
    api.obtener(3).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/especialidades/3' });
    api.crear(body).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/especialidades', body });
    api.actualizar(3, body).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/especialidades/3', body });
    api.eliminar(3).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/especialidades/3' });
  });

  it('MedicosApiService cubre médicos, horarios, disponibilidad y agenda', () => {
    const api = TestBed.inject(MedicosApiService);
    const medico = {
      nombre: 'Ana',
      apellido: 'Torres',
      email: 'ana.torres@tusalud.com',
      password: 'Medico123!',
      colegiatura: 'CMP-54321',
      precioConsulta: 150,
      especialidadId: 1,
    };
    const horario = {
      diaSemana: 'MONDAY' as const,
      horaInicio: '08:00',
      horaFin: '13:00',
      duracionCitaMinutos: 30,
    };

    api.listar().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos' });
    api.listar(2).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos', params: { especialidadId: '2' } });
    api.obtener(5).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/5' });
    api.crear(medico).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/medicos', body: medico });
    api.listarHorarios(5).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/5/horarios' });
    api.agregarHorario(5, horario).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/medicos/5/horarios', body: horario });
    api.eliminarHorario(5, 9).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/medicos/5/horarios/9' });
    api.disponibilidad(5, '2026-10-20').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/medicos/5/disponibilidad',
      params: { fecha: '2026-10-20' },
    });
    api.miAgenda().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/me/agenda' });
    api.miAgenda('2026-10-20').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/medicos/me/agenda',
      params: { fecha: '2026-10-20' },
    });
  });

  it('CitasApiService cubre la reserva y los cambios de estado', () => {
    const api = TestBed.inject(CitasApiService);
    const cita = { medicoId: 1, fechaHora: '2026-10-20T10:00:00', motivo: 'Control' };

    api.crear(cita).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/citas', body: cita });
    api.misCitas().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/citas/mis-citas' });
    api.misCitas({ inicio: '2026-10-01', fin: '2026-10-31' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/citas/mis-citas',
      params: { inicio: '2026-10-01', fin: '2026-10-31' },
    });
    api.obtener(7).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/citas/7' });
    api.cancelar(7).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/citas/7/cancelar' });
    api.confirmar(7).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/citas/7/confirmar' });
    api.atender(7).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/citas/7/atender' });
  });

  it('AuditoriaApiService cubre los reportes', () => {
    const api = TestBed.inject(AuditoriaApiService);
    api.citasPorEstado().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/auditoria/citas-por-estado' });
    api.citas({ inicio: '2026-10-01' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/admin/auditoria/citas',
      params: { inicio: '2026-10-01' },
    });
  });
});
