import { HttpEventType, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ArchivosApiService } from './archivos-api.service';
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
import { ImagenesApiService } from './imagenes-api.service';
import { LaboratorioApiService } from './laboratorio-api.service';
import { CajaApiService } from './caja-api.service';
import { HospitalizacionApiService } from './hospitalizacion-api.service';
import { PagosApiService } from './pagos-api.service';
import { RecepcionApiService } from './recepcion-api.service';
import { ReportesApiService } from './reportes-api.service';
import { SeguridadOperativaApiService } from './seguridad-operativa-api.service';
import { RrhhApiService } from './rrhh-api.service';
import { SegurosApiService } from './seguros-api.service';

function u(n: number): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

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
    api.obtener(u(3)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/especialidades/${u(3)}` });
    api.crear(body).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/especialidades', body });
    api.actualizar(u(3), body).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/especialidades/${u(3)}`, body });
    api.eliminar(u(3)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/especialidades/${u(3)}` });
  });

  it('MedicosApiService cubre médicos, horarios, disponibilidad y agenda', () => {
    const api = TestBed.inject(MedicosApiService);
    const medico = {
      nombre: 'Ana',
      apellido: 'Torres',
      email: 'ana.torres@tusalud.com',
      password: 'Medico123!',
      colegiatura: 'CMP-54321',
      especialidadIds: [u(1), u(3)],
    };
    const horario = {
      diaSemana: 'MONDAY' as const,
      horaInicio: '08:00',
      horaFin: '13:00',
      duracionCitaMinutos: 30,
    };

    api.listar().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos' });
    api.listar(u(2)).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos', params: { especialidadId: u(2) } });
    api.obtener(u(5)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/medicos/${u(5)}` });
    api.crear(medico).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/medicos', body: medico });
    api.listarHorarios(u(5)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/medicos/${u(5)}/horarios` });
    api.agregarHorario(u(5), horario).subscribe();
    expectRequest({ method: 'POST', url: `/api/v1/medicos/${u(5)}/horarios`, body: horario });
    api.eliminarHorario(u(5), u(9)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/medicos/${u(5)}/horarios/${u(9)}` });
    api.disponibilidad(u(5), '2026-10-20').subscribe();
    expectRequest({
      method: 'GET',
      url: `/api/v1/medicos/${u(5)}/disponibilidad`,
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
      medicoId: u(1),
      especialidadId: u(1),
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
    api.obtener(u(7)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/citas/${u(7)}` });
    api.cancelar(u(7), 'Viaje imprevisto').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/citas/${u(7)}/cancelar`,
      body: { motivo: 'Viaje imprevisto' },
    });
    api.confirmar(u(7)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/citas/${u(7)}/confirmar` });
    api.atender(u(7)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/citas/${u(7)}/atender` });
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
      minutosAntelacionRecordatorio: 1440,
      alertasInicioSesion: true,
      tourCompletado: false,
    };
    const foto = new File([new Uint8Array([1, 2, 3])], 'yo.png', { type: 'image/png' });
    api.actualizarAvatar(foto).subscribe();
    const avatar = controller.expectOne(
      (req) => req.method === 'PUT' && req.url === '/api/v1/perfil/avatar',
    );
    expect(avatar.request.body).toBeInstanceOf(FormData);
    expect((avatar.request.body as FormData).get('archivo')).toBeInstanceOf(File);
    expect(((avatar.request.body as FormData).get('archivo') as File).name).toBe('yo.png');
    avatar.flush({});
    api.eliminarAvatar().subscribe();
    expectRequest({ method: 'DELETE', url: '/api/v1/perfil/avatar' });
    api.obtenerPreferencias().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/perfil/preferencias' });
    api.actualizarPreferencias(preferencias).subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/perfil/preferencias', body: preferencias });
    api.listarSesiones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/perfil/sesiones' });
    api.cerrarSesion(u(4)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/perfil/sesiones/${u(4)}` });
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
    api.marcarLeida(u(3)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/notificaciones/${u(3)}/leida` });
    api.marcarTodasLeidas().subscribe();
    expectRequest({ method: 'PUT', url: '/api/v1/notificaciones/leidas' });
    api.eliminar(u(3)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/notificaciones/${u(3)}` });
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
    api.marcarPredeterminado(u(2)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/metodos-pago/${u(2)}/predeterminado` });
    api.eliminarMetodo(u(2)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/metodos-pago/${u(2)}` });
    api.validarCupon('SALUD20', 150).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/cupones/validar',
      params: { codigo: 'SALUD20', monto: '150' },
    });
    api.pagarCita(u(9), { metodoPagoId: u(2), codigoCupon: 'SALUD20' }).subscribe();
    expectRequest({
      method: 'POST',
      url: `/api/v1/citas/${u(9)}/pago`,
      body: { metodoPagoId: u(2), codigoCupon: 'SALUD20' },
    });
    api.listarPagos().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/pagos' });
    api.obtenerPago(u(5)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/pagos/${u(5)}` });
  });

  it('ClinicaApiService cubre atención, historia clínica y valoraciones', () => {
    const api = TestBed.inject(ClinicaApiService);
    const atencion = { diagnostico: 'Hipertensión leve', receta: [{ medicamento: 'Enalapril' }] };
    api.obtenerAtencion(u(7)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/citas/${u(7)}/atencion` });
    api.registrarAtencion(u(7), atencion).subscribe();
    expectRequest({ method: 'POST', url: `/api/v1/citas/${u(7)}/atencion`, body: atencion });
    api.actualizarAtencion(u(7), atencion).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/citas/${u(7)}/atencion`, body: atencion });
    api.miHistoria().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/historia-clinica' });
    api.actualizarDatosMedicos(u(3), { grupoSanguineo: 'O+' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/pacientes/${u(3)}/datos-medicos`,
      body: { grupoSanguineo: 'O+' },
    });
    api.historiaDePaciente(u(3)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/pacientes/${u(3)}/historia-clinica` });
    api.misPacientes().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/medicos/me/pacientes' });
    api.obtenerValoracion(u(7)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/citas/${u(7)}/valoracion` });
    api.valorar(u(7), { puntuacion: 5 }).subscribe();
    expectRequest({
      method: 'POST',
      url: `/api/v1/citas/${u(7)}/valoracion`,
      body: { puntuacion: 5 },
    });
    api.valoracionesDeMedico(u(1)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/medicos/${u(1)}/valoraciones` });
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
    api.actualizarCupon(u(1), cupon).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/admin/cupones/${u(1)}`, body: cupon });
    api.eliminarCupon(u(1)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/admin/cupones/${u(1)}` });
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
    api.cambiarEstadoUsuario(u(4), 'INACTIVO').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/admin/usuarios/${u(4)}/estado`,
      body: { estado: 'INACTIVO' },
    });
  });

  it('MedicosApiService actualiza las especialidades de un médico', () => {
    const api = TestBed.inject(MedicosApiService);
    api.actualizarEspecialidades(u(5), [u(1), u(4)]).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/medicos/${u(5)}/especialidades`,
      body: { especialidadIds: [u(1), u(4)] },
    });
  });

  it('AdminApiService gestiona los roles de los usuarios', () => {
    const api = TestBed.inject(AdminApiService);
    api.listarRoles().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/roles' });
    api.actualizarRoles(u(4), ['ROLE_ADMIN', 'ROLE_PACIENTE']).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/admin/usuarios/${u(4)}/roles`,
      body: { roles: ['ROLE_ADMIN', 'ROLE_PACIENTE'] },
    });
  });

  it('ClinicaApiService comparte pacientes entre médicos', () => {
    const api = TestBed.inject(ClinicaApiService);
    api.listarCompartidos(u(3)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/pacientes/${u(3)}/compartidos` });
    api.compartirPaciente(u(3), { medicoId: u(2), nota: 'Interconsulta' }).subscribe();
    expectRequest({
      method: 'POST',
      url: `/api/v1/pacientes/${u(3)}/compartir`,
      body: { medicoId: u(2), nota: 'Interconsulta' },
    });
    api.dejarDeCompartir(u(3), u(2)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/pacientes/${u(3)}/compartir/${u(2)}` });
  });

  it('PagosApiService cubre el flujo de pago con QR de Yape y Plin', () => {
    const api = TestBed.inject(PagosApiService);
    api.crearPagoQr({ citaId: u(8), billetera: 'YAPE', codigoCupon: 'SALUD20' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/pagos/qr',
      body: { citaId: u(8), billetera: 'YAPE', codigoCupon: 'SALUD20' },
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
    api.reprogramar(u(8), '2026-12-01T10:00:00').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/citas/${u(8)}/reprogramar`,
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
    api.actualizar(u(4), evento).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/calendario/eventos/${u(4)}`, body: evento });
    api.mover(u(4), { inicio: '2026-11-18T12:00:00', fin: '2026-11-18T13:00:00' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/calendario/eventos/${u(4)}/mover`,
      body: { inicio: '2026-11-18T12:00:00', fin: '2026-11-18T13:00:00' },
    });
    api.completar(u(4), true).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/calendario/eventos/${u(4)}/completado`,
      body: { completado: true },
    });
    api.eliminar(u(4)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/calendario/eventos/${u(4)}` });
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
    api.listarLotes(u(4)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/almacen/productos/${u(4)}/lotes` });
    api.registrarIngreso({ items: [] }).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/almacen/ingresos', body: { items: [] } });
    api.registrarSalida({ productoId: u(4), cantidad: 2, motivo: 'MERMA' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/almacen/salidas',
      body: { productoId: u(4), cantidad: 2, motivo: 'MERMA' },
    });
    api.listarMovimientos({ productoId: u(4), tipo: 'SALIDA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/almacen/movimientos',
      params: { productoId: u(4), tipo: 'SALIDA' },
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
    api.anular(u(9), 'Producto entregado por error').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/farmacia/dispensaciones/${u(9)}/anular`,
      body: { motivo: 'Producto entregado por error' },
    });
    api.misDispensaciones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/farmacia/mis-dispensaciones' });
  });

  it('ImagenesApiService cubre catálogo, órdenes, archivos e informes', () => {
    const api = TestBed.inject(ImagenesApiService);
    api.listarEstudios({ modalidad: 'RX', q: 'torax' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/imagenes/estudios',
      params: { modalidad: 'RX', q: 'torax' },
    });
    const orden = {
      pacienteId: u(1),
      prioridad: 'URGENTE' as const,
      indicacionClinica: 'Tos persistente',
      estudioIds: [u(2)],
    };
    api.crearOrden(orden).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/imagenes/ordenes', body: orden });
    api.listarOrdenes({ estado: 'PROGRAMADA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/imagenes/ordenes',
      params: { estado: 'PROGRAMADA' },
    });
    api.programar(u(5), { programadaPara: '2026-11-20T09:30:00', sala: 'Sala 1' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/imagenes/ordenes/${u(5)}/programar`,
      body: { programadaPara: '2026-11-20T09:30:00', sala: 'Sala 1' },
    });
    api.realizar(u(5), { observaciones: 'Sin incidencias' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/imagenes/ordenes/${u(5)}/realizar`,
      body: { observaciones: 'Sin incidencias' },
    });
    const informe = {
      hallazgos: 'Sin lesiones',
      conclusion: 'Normal',
      hallazgoCritico: false,
      firmaColegiatura: 'CMP-1234',
    };
    api.informar(u(5), informe).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/imagenes/ordenes/${u(5)}/informe`,
      body: informe,
    });
    api.historialInformes(u(5)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/imagenes/ordenes/${u(5)}/informes` });
    api.cancelar(u(5), 'Error').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/imagenes/ordenes/${u(5)}/cancelar`,
      body: { motivo: 'Error' },
    });
    api.subirArchivos(u(5), [new File([new Uint8Array([1, 2, 3])], 'toma.png')]).subscribe();
    const subida = controller.expectOne('/api/v1/imagenes/ordenes/' + u(5) + '/archivos');
    expect(subida.request.method).toBe('POST');
    expect(subida.request.body instanceof FormData).toBe(true);
    expect((subida.request.body as FormData).getAll('archivos').length).toBe(1);
    subida.flush([]);
    api.descargarArchivo(u(5), u(6)).subscribe();
    const descarga = controller.expectOne('/api/v1/imagenes/ordenes/' + u(5) + '/archivos/' + u(6));
    expect(descarga.request.responseType).toBe('blob');
    descarga.flush(new Blob(['x']));
    api.eliminarArchivo(u(5), u(6)).subscribe();
    expectRequest({
      method: 'DELETE',
      url: `/api/v1/imagenes/ordenes/${u(5)}/archivos/${u(6)}`,
    });
    api.agenda('2026-11-20', '2026-11-21').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/imagenes/agenda',
      params: { desde: '2026-11-20', hasta: '2026-11-21' },
    });
    api.misEstudios().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/imagenes/mis-estudios' });
    api.ordenesDePaciente(u(1)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/imagenes/pacientes/${u(1)}/ordenes` });
    api.miFirma().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/imagenes/mi-firma' });
  });

  it('LaboratorioApiService cubre exámenes, órdenes y resultados', () => {
    const api = TestBed.inject(LaboratorioApiService);
    api.listarExamenes({ categoria: 'BIOQUIMICA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/laboratorio/examenes',
      params: { categoria: 'BIOQUIMICA' },
    });
    api.crearOrden({ pacienteId: u(1), prioridad: 'URGENTE', examenIds: [u(2)] }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/laboratorio/ordenes',
      body: { pacienteId: u(1), prioridad: 'URGENTE', examenIds: [u(2)] },
    });
    api.tomarMuestra(u(5)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/laboratorio/ordenes/${u(5)}/muestra` });
    api
      .registrarResultados(u(5), u(8), { resultados: [{ parametroId: u(3), valor: '90' }] })
      .subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/laboratorio/ordenes/${u(5)}/items/${u(8)}/resultados`,
      body: { resultados: [{ parametroId: u(3), valor: '90' }] },
    });
    api.validar(u(5)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/laboratorio/ordenes/${u(5)}/validar` });
    api.cancelar(u(5), 'Error de registro').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/laboratorio/ordenes/${u(5)}/cancelar`,
      body: { motivo: 'Error de registro' },
    });
    api.misOrdenes().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/laboratorio/mis-ordenes' });
  });

  it('RecepcionApiService cubre llegadas, turnos y pacientes de mostrador', () => {
    const api = TestBed.inject(RecepcionApiService);
    api.citasHoy('1234').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/recepcion/citas-hoy', params: { q: '1234' } });
    api.registrarLlegada(u(9)).subscribe();
    expectRequest({ method: 'POST', url: `/api/v1/recepcion/citas/${u(9)}/llegada` });
    api.crearTurno({ pacienteId: u(2), prioridad: 'URGENTE' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/recepcion/turnos',
      body: { pacienteId: u(2), prioridad: 'URGENTE' },
    });
    api.cola('EN_ESPERA', u(3)).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/recepcion/cola',
      params: { estado: 'EN_ESPERA', medicoId: u(3) },
    });
    api.cambiarTurno(u(5), 'llamar').subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/recepcion/turnos/${u(5)}/llamar` });
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
      referenciaId: u(4),
      tipoComprobante: 'BOLETA' as const,
      clienteTipoDoc: 'DNI' as const,
      clienteNombre: 'Juan',
      metodoPago: 'YAPE' as const,
    };
    api.cobrar(cobro).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/caja/cobros', body: cobro });
    api.anular(u(7), 'Error de digitación').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/caja/comprobantes/${u(7)}/anular`,
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
    api.estadoCama(u(3), 'LIMPIEZA').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/hospitalizacion/camas/${u(3)}/estado`,
      body: { estado: 'LIMPIEZA' },
    });
    api.crearIngreso({ pacienteId: u(1), camaId: u(2), motivo: 'Neumonía' }).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/hospitalizacion/ingresos',
      body: { pacienteId: u(1), camaId: u(2), motivo: 'Neumonía' },
    });
    api.trasladar(u(8), u(5)).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/hospitalizacion/ingresos/${u(8)}/trasladar`,
      body: { camaId: u(5) },
    });
    api.cancelarIngreso(u(8), 'Ingreso registrado por error').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/hospitalizacion/ingresos/${u(8)}/cancelar`,
      body: { motivo: 'Ingreso registrado por error' },
    });
    api.evolucionesPendientes(6).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/hospitalizacion/evoluciones-pendientes',
      params: { horas: '6' },
    });
    api.evolucionesPendientes().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/hospitalizacion/evoluciones-pendientes' });
    api.alta(u(8), { diagnosticoAlta: 'Recuperado', indicacionesAlta: 'Reposo' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/hospitalizacion/ingresos/${u(8)}/alta`,
      body: { diagnosticoAlta: 'Recuperado', indicacionesAlta: 'Reposo' },
    });
    api
      .registrarEmergencia({
        pacienteId: u(2),
        triaje: { nivel: 2, motivoConsulta: 'Dolor torácico' },
      })
      .subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/hospitalizacion/emergencias',
      body: { pacienteId: u(2), triaje: { nivel: 2, motivoConsulta: 'Dolor torácico' } },
    });
    api.registrarEvolucion(u(8), { tipo: 'MEDICA', nota: 'Evoluciona favorablemente' }).subscribe();
    expectRequest({
      method: 'POST',
      url: `/api/v1/hospitalizacion/ingresos/${u(8)}/evoluciones`,
      body: { tipo: 'MEDICA', nota: 'Evoluciona favorablemente' },
    });
    api.misIngresos().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/hospitalizacion/mis-ingresos' });
  });

  it('ReportesApiService consulta cada reporte con su rango y agrupación', () => {
    const api = TestBed.inject(ReportesApiService);
    const filtro = { desde: '2026-11-01', hasta: '2026-11-30', agrupacion: 'SEMANA' as const };
    const params = { desde: '2026-11-01', hasta: '2026-11-30', agrupacion: 'SEMANA' };
    api.ejecutivo(filtro).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/reportes/ejecutivo', params });
    api.financiero(filtro).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/reportes/financiero', params });
    api.operativo(filtro).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/reportes/operativo', params });
    api.calidad(filtro).subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/reportes/calidad', params });
  });

  it('RrhhApiService cubre fichas, turnos y ausencias', () => {
    const api = TestBed.inject(RrhhApiService);
    api.resumen().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/rrhh/resumen' });
    api.empleados({ q: 'ana', area: 'CAJA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/rrhh/empleados',
      params: { q: 'ana', area: 'CAJA' },
    });
    api.empleado(u(4)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/rrhh/empleados/${u(4)}` });
    const ficha = {
      usuarioId: u(5),
      cargo: 'Cajero',
      area: 'CAJA' as const,
      tipoContrato: 'INDEFINIDO' as const,
      fechaIngreso: '2026-01-05',
      estado: 'ACTIVO' as const,
      sueldo: 2500,
    };
    api.crearEmpleado(ficha).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/rrhh/empleados', body: ficha });
    api.actualizarEmpleado(u(4), ficha).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/rrhh/empleados/${u(4)}`, body: ficha });
    api.usuariosDisponibles().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/rrhh/usuarios-disponibles' });
    api.saldoVacaciones(u(4)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/rrhh/empleados/${u(4)}/saldo-vacaciones` });
    api.plantillasTurno().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/rrhh/plantillas-turno' });
    api.semana('2026-11-16', 'MEDICA').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/rrhh/turnos/semana',
      params: { fecha: '2026-11-16', area: 'MEDICA' },
    });
    const turno = { empleadoId: u(4), plantillaId: u(6), fecha: '2026-11-24' };
    api.asignarTurno(turno).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/rrhh/turnos', body: turno });
    api.eliminarTurno(u(7)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/rrhh/turnos/${u(7)}` });
    api.ausencias({ estado: 'SOLICITADA' }).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/rrhh/ausencias',
      params: { estado: 'SOLICITADA' },
    });
    api.resolverAusencia(u(8), 'aprobar', null).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/rrhh/ausencias/${u(8)}/aprobar`, body: {} });
    api.resolverAusencia(u(8), 'rechazar', 'Cobertura insuficiente').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/rrhh/ausencias/${u(8)}/rechazar`,
      body: { motivo: 'Cobertura insuficiente' },
    });
    api.miFicha().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/rrhh/mi-ficha' });
    api.misTurnos('2026-11-16', '2026-11-22').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/rrhh/mis-turnos',
      params: { desde: '2026-11-16', hasta: '2026-11-22' },
    });
    api.misAusencias().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/rrhh/mis-ausencias' });
    const solicitud = {
      tipo: 'VACACIONES' as const,
      fechaInicio: '2026-12-01',
      fechaFin: '2026-12-05',
    };
    api.solicitarAusencia(solicitud).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/rrhh/mis-ausencias', body: solicitud });
    api.cancelarMiAusencia(u(9)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/rrhh/mis-ausencias/${u(9)}/cancelar`, body: {} });
  });

  it('ArchivosApiService sube con progreso, descarga y gestiona archivos', () => {
    const api = TestBed.inject(ArchivosApiService);
    const archivo = new File([new Uint8Array([1, 2, 3, 4])], 'informe.pdf', {
      type: 'application/pdf',
    });
    const progreso: number[] = [];
    let subido: unknown = null;
    api.subir(archivo, 'DOCUMENTO_PACIENTE', u(5), 'PACIENTE').subscribe((estado) => {
      progreso.push(estado.progreso);
      subido = estado.archivo ?? subido;
    });
    const subida = controller.expectOne(
      (req) => req.method === 'POST' && req.url === '/api/v1/archivos',
    );
    const formulario = subida.request.body as FormData;
    expect(formulario.get('proposito')).toBe('DOCUMENTO_PACIENTE');
    expect(formulario.get('referenciaId')).toBe(u(5));
    expect(formulario.get('referenciaTipo')).toBe('PACIENTE');
    expect((formulario.get('archivo') as File).name).toBe('informe.pdf');
    subida.event({ type: HttpEventType.UploadProgress, loaded: 2, total: 4 });
    subida.flush({ id: u(9), nombre: 'informe.pdf' });
    expect(progreso).toEqual([50, 100]);
    expect(subido).toEqual({ id: u(9), nombre: 'informe.pdf' });

    api.descargar(u(9)).subscribe();
    const descarga = controller.expectOne(`/api/v1/archivos/${u(9)}`);
    expect(descarga.request.responseType).toBe('blob');
    descarga.flush(new Blob(['x']));

    api.metadatos(u(9)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/archivos/${u(9)}/metadatos` });
    api.listar('PACIENTE', u(5)).subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/archivos',
      params: { referenciaTipo: 'PACIENTE', referenciaId: u(5) },
    });
    api.eliminar(u(9)).subscribe();
    expectRequest({ method: 'DELETE', url: `/api/v1/archivos/${u(9)}` });
  });

  it('ArchivosApiService omite la referencia cuando no se indica', () => {
    const api = TestBed.inject(ArchivosApiService);
    api.subir(new File([new Uint8Array([1])], 'a.png'), 'AVATAR').subscribe();
    const subida = controller.expectOne((req) => req.url === '/api/v1/archivos');
    const formulario = subida.request.body as FormData;
    expect(formulario.has('referenciaId')).toBe(false);
    expect(formulario.has('referenciaTipo')).toBe(false);
    subida.flush({});
  });

  it('SeguridadOperativaApiService cubre eventos, resumen, estado y uso del almacenamiento', () => {
    const api = TestBed.inject(SeguridadOperativaApiService);
    api.eventos().subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/admin/seguridad/eventos',
      params: { pagina: '0', tamano: '20' },
    });
    api
      .eventos({
        tipo: 'LOGIN_FALLIDO',
        severidad: 'ALTA',
        desde: '2026-11-01',
        pagina: 2,
        tamano: 10,
      })
      .subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/admin/seguridad/eventos',
      params: {
        tipo: 'LOGIN_FALLIDO',
        severidad: 'ALTA',
        desde: '2026-11-01',
        pagina: '2',
        tamano: '10',
      },
    });
    api.resumen().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/seguridad/resumen' });
    api.estadoSistema().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/sistema/estado' });
    api.usoAlmacenamiento().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/admin/almacenamiento/uso' });
  });

  it('CajaApiService calcula el desglose con la cobertura del seguro', () => {
    const api = TestBed.inject(CajaApiService);
    api.desglose('CITA', u(5)).subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/caja/cobros/desglose',
      body: { origen: 'CITA', referenciaId: u(5) },
    });
  });

  it('SegurosApiService cubre catálogo, pólizas, preautorizaciones y liquidaciones', () => {
    const api = TestBed.inject(SegurosApiService);
    api.aseguradoras().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/aseguradoras' });
    const aseguradora = { codigo: 'RIMAC', nombre: 'Rímac', tipo: 'PRIVADO' as const };
    api.crearAseguradora(aseguradora).subscribe();
    expectRequest({ method: 'POST', url: '/api/v1/seguros/aseguradoras', body: aseguradora });
    api.actualizarAseguradora(u(1), aseguradora).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/seguros/aseguradoras/${u(1)}`,
      body: aseguradora,
    });
    api.convenios().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/convenios' });

    api.buscarPolizas('pac').subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/polizas', params: { q: 'pac' } });
    api.polizasDelPaciente(u(2)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/seguros/pacientes/${u(2)}/polizas` });
    api.verificarPoliza(u(3)).subscribe();
    expectRequest({
      method: 'POST',
      url: `/api/v1/seguros/polizas/${u(3)}/verificaciones`,
      body: {},
    });
    api.cambiarEstadoPoliza(u(3), { estado: 'SUSPENDIDA', motivo: 'Revisión' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/seguros/polizas/${u(3)}/estado`,
      body: { estado: 'SUSPENDIDA', motivo: 'Revisión' },
    });
    api.misPolizas().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/mis-polizas' });
    api.coberturaDelPaciente(u(2)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/seguros/cobertura/pacientes/${u(2)}` });

    api.preautorizaciones('SOLICITADA').subscribe();
    expectRequest({
      method: 'GET',
      url: '/api/v1/seguros/preautorizaciones',
      params: { estado: 'SOLICITADA' },
    });
    api.misPreautorizaciones().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/preautorizaciones/mias' });
    api.aprobarPreautorizacion(u(4), { montoAprobado: 600, vigenciaDias: 15 }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/seguros/preautorizaciones/${u(4)}/aprobar`,
      body: { montoAprobado: 600, vigenciaDias: 15 },
    });
    api.rechazarPreautorizacion(u(4), 'No cubierto').subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/seguros/preautorizaciones/${u(4)}/rechazar`,
      body: { motivo: 'No cubierto' },
    });

    api.cartera().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/cartera' });
    api.cuentasDeAseguradora(u(1)).subscribe();
    expectRequest({ method: 'GET', url: `/api/v1/seguros/aseguradoras/${u(1)}/cuentas` });
    api
      .crearLiquidacion({ aseguradoraId: u(1), desde: '2026-11-01', hasta: '2026-11-30' })
      .subscribe();
    expectRequest({
      method: 'POST',
      url: '/api/v1/seguros/liquidaciones',
      body: { aseguradoraId: u(1), desde: '2026-11-01', hasta: '2026-11-30' },
    });
    api.enviarLiquidacion(u(6)).subscribe();
    expectRequest({ method: 'PUT', url: `/api/v1/seguros/liquidaciones/${u(6)}/enviar`, body: {} });
    api.registrarPagoLiquidacion(u(6), { montoPagado: 100, motivoGlosa: 'Glosa' }).subscribe();
    expectRequest({
      method: 'PUT',
      url: `/api/v1/seguros/liquidaciones/${u(6)}/pago`,
      body: { montoPagado: 100, motivoGlosa: 'Glosa' },
    });
    api.resumen().subscribe();
    expectRequest({ method: 'GET', url: '/api/v1/seguros/resumen' });
  });
});
