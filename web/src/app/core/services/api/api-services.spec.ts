import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuditoriaApiService } from './auditoria-api.service';
import { AuthApiService } from './auth-api.service';
import { CitasApiService } from './citas-api.service';
import { EspecialidadesApiService } from './especialidades-api.service';
import { MedicosApiService } from './medicos-api.service';
import { PerfilApiService } from './perfil-api.service';
import { AdminApiService } from './admin-api.service';
import { ClinicaApiService } from './clinica-api.service';
import { NotificacionesApiService } from './notificaciones-api.service';
import { AlmacenApiService } from './almacen-api.service';
import { CalendarioApiService } from './calendario-api.service';
import { FarmaciaApiService } from './farmacia-api.service';
import { LaboratorioApiService } from './laboratorio-api.service';
import { CajaApiService } from './caja-api.service';
import { HospitalizacionApiService } from './hospitalizacion-api.service';
import { PagosApiService } from './pagos-api.service';
import { RecepcionApiService } from './recepcion-api.service';

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

  it('PerfilApiService cubre perfil y cambio de contraseña', () => {
    const api = TestBed.inject(PerfilApiService);
    const perfil = { nombre: 'Rosa', apellido: 'Quispe', telefono: '987654321' };
    const password = { passwordActual: 'Paciente123!', passwordNueva: 'NuevaClave456!' };
    api.obtener().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/perfil' });
    api.actualizar(perfil).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/perfil', body: perfil });
    api.cambiarPassword(password).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/perfil/password', body: password });
  });

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
    const body = { nombre: 'Neurología', descripcion: 'Sistema nervioso', precioConsulta: 170 };
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
      especialidadIds: [1, 3],
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
    const cita = {
      medicoId: 1,
      especialidadId: 1,
      fechaHora: '2026-10-20T10:00:00',
      motivo: 'Control',
    };

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

  it('PerfilApiService cubre avatar, preferencias y sesiones', () => {
    const api = TestBed.inject(PerfilApiService);
    const preferencias = {
      recordatoriosActivos: true,
      horasAntelacionRecordatorio: 24,
      alertasInicioSesion: true,
      tourCompletado: false,
    };
    api.actualizarAvatar('data:image/png;base64,AAAA').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/perfil/avatar',
      body: { imagen: 'data:image/png;base64,AAAA' },
    });
    api.eliminarAvatar().subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/perfil/avatar' });
    api.obtenerPreferencias().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/perfil/preferencias' });
    api.actualizarPreferencias(preferencias).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/perfil/preferencias', body: preferencias });
    api.listarSesiones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/perfil/sesiones' });
    api.cerrarSesion(4).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/perfil/sesiones/4' });
    api.cerrarOtrasSesiones().subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/perfil/sesiones' });
  });

  it('NotificacionesApiService cubre bandeja y contador', () => {
    const api = TestBed.inject(NotificacionesApiService);
    api.listar(true, 10).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/notificaciones',
      params: { soloNoLeidas: 'true', limit: '10' },
    });
    api.resumen().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/notificaciones/resumen' });
    api.marcarLeida(3).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/notificaciones/3/leida' });
    api.marcarTodasLeidas().subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/notificaciones/leidas' });
    api.eliminar(3).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/notificaciones/3' });
  });

  it('PagosApiService cubre métodos de pago, cupones y pagos', () => {
    const api = TestBed.inject(PagosApiService);
    const metodo = {
      tipo: 'TARJETA_CREDITO' as const,
      titular: 'JUAN PEREZ',
      numeroTarjeta: '4242424242424242',
      vencimientoMes: 12,
      vencimientoAnio: 2030,
      cvv: '123',
      predeterminado: true,
    };
    api.listarMetodos().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/metodos-pago' });
    api.crearMetodo(metodo).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/metodos-pago', body: metodo });
    api.marcarPredeterminado(2).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/metodos-pago/2/predeterminado' });
    api.eliminarMetodo(2).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/metodos-pago/2' });
    api.validarCupon('SALUD20', 150).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/cupones/validar',
      params: { codigo: 'SALUD20', monto: '150' },
    });
    api.pagarCita(9, { metodoPagoId: 2, codigoCupon: 'SALUD20' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/citas/9/pago',
      body: { metodoPagoId: 2, codigoCupon: 'SALUD20' },
    });
    api.listarPagos().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pagos' });
    api.obtenerPago(5).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pagos/5' });
  });

  it('ClinicaApiService cubre atención, historia clínica y valoraciones', () => {
    const api = TestBed.inject(ClinicaApiService);
    const atencion = { diagnostico: 'Hipertensión leve', receta: [{ medicamento: 'Enalapril' }] };
    api.obtenerAtencion(7).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/citas/7/atencion' });
    api.registrarAtencion(7, atencion).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/citas/7/atencion', body: atencion });
    api.actualizarAtencion(7, atencion).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/citas/7/atencion', body: atencion });
    api.miHistoria().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/historia-clinica' });
    api.actualizarDatosMedicos({ grupoSanguineo: 'O+' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/historia-clinica/datos-medicos',
      body: { grupoSanguineo: 'O+' },
    });
    api.historiaDePaciente(3).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pacientes/3/historia-clinica' });
    api.misPacientes().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/me/pacientes' });
    api.obtenerValoracion(7).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/citas/7/valoracion' });
    api.valorar(7, { puntuacion: 5 }).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/citas/7/valoracion', body: { puntuacion: 5 } });
    api.valoracionesDeMedico(1).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/1/valoraciones' });
  });

  it('AdminApiService cubre estadísticas, cupones, pagos y usuarios', () => {
    const api = TestBed.inject(AdminApiService);
    const cupon = {
      codigo: 'SALUD20',
      tipo: 'PORCENTAJE' as const,
      valor: 20,
      fechaInicio: '2026-01-01',
      fechaFin: '2026-12-31',
      activo: true,
    };
    api.estadisticas().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/estadisticas' });
    api.listarCupones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/cupones' });
    api.crearCupon(cupon).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/admin/cupones', body: cupon });
    api.actualizarCupon(1, cupon).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/admin/cupones/1', body: cupon });
    api.eliminarCupon(1).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/admin/cupones/1' });
    api.listarPagos({ inicio: '2026-01-01', fin: '2026-01-31' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/admin/pagos',
      params: { inicio: '2026-01-01', fin: '2026-01-31' },
    });
    api.listarUsuarios('ROLE_MEDICO', 'garcia').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/admin/usuarios',
      params: { rol: 'ROLE_MEDICO', q: 'garcia' },
    });
    api.cambiarEstadoUsuario(4, 'INACTIVO').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/admin/usuarios/4/estado',
      body: { estado: 'INACTIVO' },
    });
  });

  it('MedicosApiService actualiza las especialidades de un médico', () => {
    const api = TestBed.inject(MedicosApiService);
    api.actualizarEspecialidades(5, [1, 4]).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/medicos/5/especialidades',
      body: { especialidadIds: [1, 4] },
    });
  });

  it('AdminApiService gestiona los roles de los usuarios', () => {
    const api = TestBed.inject(AdminApiService);
    api.listarRoles().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/roles' });
    api.actualizarRoles(4, ['ROLE_ADMIN', 'ROLE_PACIENTE']).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/admin/usuarios/4/roles',
      body: { roles: ['ROLE_ADMIN', 'ROLE_PACIENTE'] },
    });
  });

  it('ClinicaApiService comparte pacientes entre médicos', () => {
    const api = TestBed.inject(ClinicaApiService);
    api.listarCompartidos(3).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pacientes/3/compartidos' });
    api.compartirPaciente(3, { medicoId: 2, nota: 'Interconsulta' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/pacientes/3/compartir',
      body: { medicoId: 2, nota: 'Interconsulta' },
    });
    api.dejarDeCompartir(3, 2).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/pacientes/3/compartir/2' });
  });

  it('PagosApiService cubre el flujo de pago con QR de Yape y Plin', () => {
    const api = TestBed.inject(PagosApiService);
    api.crearPagoQr({ citaId: 8, billetera: 'YAPE', codigoCupon: 'SALUD20' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/pagos/qr',
      body: { citaId: 8, billetera: 'YAPE', codigoCupon: 'SALUD20' },
    });
    api.consultarPagoQr('abc').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pagos/qr/abc' });
    api.simularConfirmacionQr('abc').subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/pagos/qr/abc/simular-confirmacion' });
    api.cancelarPagoQr('abc').subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/pagos/qr/abc' });
  });

  it('AuthApiService cubre la verificación en dos pasos', () => {
    const api = TestBed.inject(AuthApiService);
    api.estadoMfa().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/auth/mfa/estado' });
    api.configurarMfa().subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/auth/mfa/configurar' });
    api.activarMfa('123456').subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/auth/mfa/activar', body: { codigo: '123456' } });
    api.desactivarMfa({ password: 'x', codigo: '123456' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/auth/mfa/desactivar',
      body: { password: 'x', codigo: '123456' },
    });
    api.regenerarCodigosMfa('123456').subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/auth/mfa/codigos-recuperacion/regenerar',
      body: { codigo: '123456' },
    });
  });

  it('CitasApiService reprograma una cita', () => {
    const api = TestBed.inject(CitasApiService);
    api.reprogramar(8, '2026-12-01T10:00:00').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/citas/8/reprogramar',
      body: { fechaHora: '2026-12-01T10:00:00' },
    });
  });

  it('CalendarioApiService cubre los eventos del calendario', () => {
    const api = TestBed.inject(CalendarioApiService);
    const evento = {
      titulo: 'Reunión',
      tipo: 'REUNION' as const,
      inicio: '2026-11-18T10:00:00',
      fin: '2026-11-18T11:00:00',
      todoElDia: false,
    };
    api.listar('2026-11-01', '2026-11-30').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/calendario/eventos',
      params: { inicio: '2026-11-01', fin: '2026-11-30' },
    });
    api.crear(evento).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/calendario/eventos', body: evento });
    api.actualizar(4, evento).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/calendario/eventos/4', body: evento });
    api.mover(4, { inicio: '2026-11-18T12:00:00', fin: '2026-11-18T13:00:00' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/calendario/eventos/4/mover',
      body: { inicio: '2026-11-18T12:00:00', fin: '2026-11-18T13:00:00' },
    });
    api.completar(4, true).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/calendario/eventos/4/completado',
      body: { completado: true },
    });
    api.eliminar(4).subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/calendario/eventos/4' });
  });

  it('AlmacenApiService cubre productos, ingresos, salidas y kardex', () => {
    const api = TestBed.inject(AlmacenApiService);
    api.resumen().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/almacen/resumen' });
    api.alertas().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/almacen/alertas' });
    api.listarProductos({ q: 'para', bajoStock: true }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/almacen/productos',
      params: { q: 'para', bajoStock: 'true' },
    });
    api.listarLotes(4).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/almacen/productos/4/lotes' });
    api.registrarIngreso({ items: [] }).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/almacen/ingresos', body: { items: [] } });
    api.registrarSalida({ productoId: 4, cantidad: 2, motivo: 'MERMA' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/almacen/salidas',
      body: { productoId: 4, cantidad: 2, motivo: 'MERMA' },
    });
    api.listarMovimientos({ productoId: 4, tipo: 'SALIDA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/almacen/movimientos',
      params: { productoId: '4', tipo: 'SALIDA' },
    });
    api.listarProveedores({ activo: true }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/almacen/proveedores',
      params: { activo: 'true' },
    });
  });

  it('FarmaciaApiService cubre recetas, dispensaciones y anulación', () => {
    const api = TestBed.inject(FarmaciaApiService);
    api.recetas('PENDIENTE', '1234').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/farmacia/recetas',
      params: { estado: 'PENDIENTE', q: '1234' },
    });
    api.catalogo('para').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/farmacia/catalogo', params: { q: 'para' } });
    const request = { items: [], metodoPago: 'YAPE' as const };
    api.dispensar(request).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/farmacia/dispensaciones', body: request });
    api.anular(9).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/farmacia/dispensaciones/9/anular' });
    api.misDispensaciones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/farmacia/mis-dispensaciones' });
  });

  it('LaboratorioApiService cubre exámenes, órdenes y resultados', () => {
    const api = TestBed.inject(LaboratorioApiService);
    api.listarExamenes({ categoria: 'BIOQUIMICA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/laboratorio/examenes',
      params: { categoria: 'BIOQUIMICA' },
    });
    api.crearOrden({ pacienteId: 1, prioridad: 'URGENTE', examenIds: [2] }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/laboratorio/ordenes',
      body: { pacienteId: 1, prioridad: 'URGENTE', examenIds: [2] },
    });
    api.tomarMuestra(5).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/laboratorio/ordenes/5/muestra' });
    api.registrarResultados(5, 8, { resultados: [{ parametroId: 3, valor: '90' }] }).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/laboratorio/ordenes/5/items/8/resultados',
      body: { resultados: [{ parametroId: 3, valor: '90' }] },
    });
    api.validar(5).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/laboratorio/ordenes/5/validar' });
    api.cancelar(5, 'Error de registro').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/laboratorio/ordenes/5/cancelar',
      body: { motivo: 'Error de registro' },
    });
    api.misOrdenes().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/laboratorio/mis-ordenes' });
  });

  it('RecepcionApiService cubre llegadas, turnos y pacientes de mostrador', () => {
    const api = TestBed.inject(RecepcionApiService);
    api.citasHoy('1234').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/recepcion/citas-hoy', params: { q: '1234' } });
    api.registrarLlegada(9).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/recepcion/citas/9/llegada' });
    api.crearTurno({ pacienteId: 2, prioridad: 'URGENTE' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/recepcion/turnos',
      body: { pacienteId: 2, prioridad: 'URGENTE' },
    });
    api.cola('EN_ESPERA', 3).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/recepcion/cola',
      params: { estado: 'EN_ESPERA', medicoId: '3' },
    });
    api.cambiarTurno(5, 'llamar').subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/recepcion/turnos/5/llamar' });
    api.buscarPacientes('juan').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/recepcion/pacientes', params: { q: 'juan' } });
    api.pantalla().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/recepcion/pantalla' });
  });

  it('CajaApiService cubre apertura, cobros, anulación y reportes', () => {
    const api = TestBed.inject(CajaApiService);
    api.abrir(100).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/caja/aperturas', body: { montoApertura: 100 } });
    api.cerrar({ montoContado: 180 }).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/caja/cierre', body: { montoContado: 180 } });
    api.pendientes('perez').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/caja/pendientes', params: { q: 'perez' } });
    const cobro = {
      origen: 'CITA' as const,
      referenciaId: 4,
      tipoComprobante: 'BOLETA' as const,
      clienteTipoDoc: 'DNI' as const,
      clienteNombre: 'Juan',
      metodoPago: 'YAPE' as const,
    };
    api.cobrar(cobro).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/caja/cobros', body: cobro });
    api.anular(7, 'Error de digitación').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/caja/comprobantes/7/anular',
      body: { motivo: 'Error de digitación' },
    });
    api.reporteIngresos('2026-11-01', '2026-11-30').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/caja/reportes/ingresos',
      params: { desde: '2026-11-01', hasta: '2026-11-30' },
    });
  });

  it('HospitalizacionApiService cubre camas, ingresos, emergencias y altas', () => {
    const api = TestBed.inject(HospitalizacionApiService);
    api.mapa().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/hospitalizacion/mapa' });
    api.camas('UCI', 'LIBRE').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/hospitalizacion/camas',
      params: { area: 'UCI', estado: 'LIBRE' },
    });
    api.estadoCama(3, 'LIMPIEZA').subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/hospitalizacion/camas/3/estado',
      body: { estado: 'LIMPIEZA' },
    });
    api.crearIngreso({ pacienteId: 1, camaId: 2, motivo: 'Neumonía' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/hospitalizacion/ingresos',
      body: { pacienteId: 1, camaId: 2, motivo: 'Neumonía' },
    });
    api.trasladar(8, 5).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/hospitalizacion/ingresos/8/trasladar',
      body: { camaId: 5 },
    });
    api.alta(8, { diagnosticoAlta: 'Recuperado', indicacionesAlta: 'Reposo' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: '/api/v1/hospitalizacion/ingresos/8/alta',
      body: { diagnosticoAlta: 'Recuperado', indicacionesAlta: 'Reposo' },
    });
    api
      .registrarEmergencia({
        pacienteId: 2,
        triaje: { nivel: 2, motivoConsulta: 'Dolor torácico' },
      })
      .subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/hospitalizacion/emergencias',
      body: { pacienteId: 2, triaje: { nivel: 2, motivoConsulta: 'Dolor torácico' } },
    });
    api.registrarEvolucion(8, { tipo: 'MEDICA', nota: 'Evoluciona favorablemente' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/hospitalizacion/ingresos/8/evoluciones',
      body: { tipo: 'MEDICA', nota: 'Evoluciona favorablemente' },
    });
    api.misIngresos().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/hospitalizacion/mis-ingresos' });
  });
});
