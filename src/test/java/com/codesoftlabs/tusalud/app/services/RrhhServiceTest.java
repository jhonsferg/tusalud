package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AusenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EmpleadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.TurnoPersonalRequest;
import com.codesoftlabs.tusalud.app.dto.response.AusenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.EmpleadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RrhhResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.SemanaTurnosResponse;
import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoContrato;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AuditoriaRrhhRepository;
import com.codesoftlabs.tusalud.app.repositories.PlantillaTurnoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AusenciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.EmpleadoService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.TurnoPersonalService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class RrhhServiceTest {

    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private TurnoPersonalService turnoService;

    @Autowired
    private AusenciaService ausenciaService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PlantillaTurnoRepository plantillaRepository;

    @Autowired
    private AuditoriaRrhhRepository auditoriaRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal rrhh;
    private UsuarioPrincipal admin;
    private UsuarioPrincipal gerencia;
    private Usuario usuarioEnfermera;
    private UsuarioPrincipal enfermera;
    private PlantillaTurno manana;
    private PlantillaTurno tarde;
    private PlantillaTurno noche;
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        rrhh = fixture.principal(fixture.usuario("rrhh", RolNombre.ROLE_RRHH));
        admin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));
        gerencia = fixture.principal(fixture.usuario("ger", RolNombre.ROLE_GERENCIA));
        usuarioEnfermera = fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA);
        enfermera = fixture.principal(usuarioEnfermera);
        manana = plantilla("Mañana", 7, 15);
        tarde = plantilla("Tarde", 15, 23);
        noche = plantilla("Noche", 23, 7);
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private PlantillaTurno plantilla(String nombre, int inicio, int fin) {
        return plantillaRepository.save(PlantillaTurno.builder()
                .nombre(nombre + ServiceTestFixture.siguiente())
                .horaInicio(LocalTime.of(inicio, 0))
                .horaFin(LocalTime.of(fin, 0))
                .build());
    }

    private EmpleadoRequest ficha(UUID usuarioId, TipoContrato contrato, LocalDate fin) {
        return new EmpleadoRequest(
                usuarioId,
                "Enfermera de emergencia",
                AreaLaboral.ENFERMERIA,
                contrato,
                LocalDate.now().minusYears(1),
                fin,
                null,
                new BigDecimal("4800.00"),
                "Rosa Gutiérrez",
                "987654321",
                30);
    }

    private EmpleadoResponse crearFicha() {
        return empleadoService.crear(ficha(usuarioEnfermera.getId(), TipoContrato.INDEFINIDO, null), rrhh);
    }

    private TurnoPersonalRequest turno(UUID empleadoId, PlantillaTurno plantilla, LocalDate fecha) {
        return new TurnoPersonalRequest(empleadoId, plantilla.getId(), fecha, null);
    }

    private AusenciaRequest ausencia(TipoAusencia tipo, LocalDate inicio, LocalDate fin) {
        return new AusenciaRequest(tipo, inicio, fin, "Motivo de prueba");
    }

    @Test
    void creaLaFichaYOcultaElSueldoSegunElRol() {
        EmpleadoResponse creada = crearFicha();

        assertThat(creada.sueldo()).isEqualByComparingTo("4800.00");
        assertThat(creada.contactoEmergenciaNombre()).isEqualTo("Rosa Gutiérrez");
        assertThat(creada.estado()).isEqualTo(EstadoEmpleado.ACTIVO);
        assertThat(empleadoService.obtener(creada.id(), gerencia).sueldo()).isEqualByComparingTo("4800.00");
        EmpleadoResponse paraAdmin = empleadoService.obtener(creada.id(), admin);
        assertThat(paraAdmin.sueldo()).isNull();
        assertThat(paraAdmin.contactoEmergenciaNombre()).isNull();
        assertThat(paraAdmin.contactoEmergenciaTelefono()).isNull();
        assertThat(empleadoService.listar(null, null, null, admin))
                .allSatisfy(e -> assertThat(e.sueldo()).isNull());
        assertThat(auditoriaRepository.count()).isEqualTo(1);
    }

    @Test
    void validaLaFichaYRechazaDuplicadosYPacientes() {
        crearFicha();
        assertThatThrownBy(() ->
                        empleadoService.crear(ficha(usuarioEnfermera.getId(), TipoContrato.INDEFINIDO, null), rrhh))
                .isInstanceOf(ConflictException.class);
        UsuarioPrincipal paciente = fixture.principal(fixture.usuario("pac", RolNombre.ROLE_PACIENTE));
        assertThatThrownBy(() -> empleadoService.crear(ficha(paciente.getId(), TipoContrato.INDEFINIDO, null), rrhh))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("paciente");
        Usuario otro = fixture.usuario("otro", RolNombre.ROLE_CAJA);
        assertThatThrownBy(() -> empleadoService.crear(ficha(otro.getId(), TipoContrato.PLAZO_FIJO, null), rrhh))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("fecha de fin");
        assertThatThrownBy(() ->
                        empleadoService.crear(ficha(otro.getId(), TipoContrato.INDEFINIDO, LocalDate.now()), rrhh))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> empleadoService.crear(ficha(UUID.randomUUID(), TipoContrato.INDEFINIDO, null), rrhh))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizaLaFichaYAuditaSinGuardarMontos() {
        EmpleadoResponse creada = crearFicha();
        EmpleadoRequest cambio = new EmpleadoRequest(
                null,
                "Jefa de enfermería",
                AreaLaboral.ENFERMERIA,
                TipoContrato.INDEFINIDO,
                LocalDate.now().minusYears(1),
                null,
                EstadoEmpleado.LICENCIA,
                new BigDecimal("5200.00"),
                null,
                null,
                25);

        EmpleadoResponse actualizada = empleadoService.actualizar(creada.id(), cambio, rrhh);

        assertThat(actualizada.cargo()).isEqualTo("Jefa de enfermería");
        assertThat(actualizada.estado()).isEqualTo(EstadoEmpleado.LICENCIA);
        assertThat(actualizada.diasVacacionesAnuales()).isEqualTo(25);
        assertThat(auditoriaRepository.findAll())
                .anySatisfy(a -> assertThat(a.getDetalle()).contains("sueldo").doesNotContain("5200"));
    }

    @Test
    void elEmpleadoVeSuFichaSinSueldoYSoloSiLaTiene() {
        assertThatThrownBy(() -> empleadoService.miFicha(enfermera)).isInstanceOf(ResourceNotFoundException.class);
        crearFicha();
        EmpleadoResponse propia = empleadoService.miFicha(enfermera);
        assertThat(propia.sueldo()).isNull();
        assertThat(propia.cargo()).isEqualTo("Enfermera de emergencia");
    }

    @Test
    void listaLosUsuariosDelPersonalSinFicha() {
        crearFicha();
        assertThat(empleadoService.usuariosDisponibles())
                .extracting(u -> u.usuarioId())
                .doesNotContain(usuarioEnfermera.getId())
                .contains(admin.getId());
    }

    @Test
    void asignaTurnosYDetectaChoquesExcesoDeHorasYAusencias() {
        EmpleadoResponse emp = crearFicha();
        turnoService.asignar(turno(emp.id(), manana, lunes), rrhh);

        assertThatThrownBy(() -> turnoService.asignar(turno(emp.id(), manana, lunes), rrhh))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("se cruza");

        for (int d = 1; d <= 5; d++) {
            turnoService.asignar(turno(emp.id(), manana, lunes.plusDays(d)), rrhh);
        }
        assertThatThrownBy(() -> turnoService.asignar(turno(emp.id(), tarde, lunes), rrhh))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("48");
        assertThat(turnoService.listar(lunes, lunes.plusDays(6), AreaLaboral.ENFERMERIA, null))
                .hasSize(6);
        assertThatThrownBy(() -> turnoService.asignar(
                        turno(emp.id(), manana, LocalDate.now().minusDays(1)), rrhh))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void unTurnoNocturnoNoChocaConElSiguienteDiaPeroSiConUnoQueLoCruza() {
        EmpleadoResponse emp = crearFicha();
        turnoService.asignar(turno(emp.id(), noche, lunes), rrhh);

        turnoService.asignar(turno(emp.id(), manana, lunes.plusDays(1)), rrhh);
        PlantillaTurno madrugada = plantilla("Madrugada", 5, 9);
        assertThatThrownBy(() -> turnoService.asignar(turno(emp.id(), madrugada, lunes.plusDays(1)), rrhh))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void noAsignaTurnosSobreUnaAusenciaNiAEmpleadosInactivos() {
        EmpleadoResponse emp = crearFicha();
        ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.PERMISO, lunes, lunes.plusDays(1)));
        assertThatThrownBy(() -> turnoService.asignar(turno(emp.id(), manana, lunes), rrhh))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ausencia");

        EmpleadoRequest cese = new EmpleadoRequest(
                null,
                "Enfermera",
                AreaLaboral.ENFERMERIA,
                TipoContrato.INDEFINIDO,
                LocalDate.now().minusYears(1),
                null,
                EstadoEmpleado.CESADO,
                new BigDecimal("4800.00"),
                null,
                null,
                30);
        empleadoService.actualizar(emp.id(), cese, rrhh);
        assertThatThrownBy(() -> turnoService.asignar(turno(emp.id(), manana, lunes.plusDays(3)), rrhh))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("activos");
    }

    @Test
    void quitaTurnosFuturosYMuestraLaVistaSemanalYMisTurnos() {
        EmpleadoResponse emp = crearFicha();
        var turno = turnoService.asignar(turno(emp.id(), manana, lunes), rrhh);

        SemanaTurnosResponse semana = turnoService.semana(lunes, AreaLaboral.ENFERMERIA);
        assertThat(semana.desde()).isEqualTo(lunes);
        assertThat(semana.filas()).hasSize(1);
        assertThat(semana.filas().get(0).horasSemana()).isEqualTo(8.0);
        assertThat(semana.filas().get(0).dias()).hasSize(7);
        assertThat(turnoService.misTurnos(enfermera, lunes, lunes)).hasSize(1);

        turnoService.eliminar(turno.id(), rrhh);
        assertThat(turnoService.misTurnos(enfermera, lunes, lunes)).isEmpty();
        assertThatThrownBy(() -> turnoService.eliminar(turno.id(), rrhh)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> turnoService.listar(lunes, lunes.plusDays(90), null, null))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void solicitaUnaAusenciaYValidaFechasSuperposicionYSaldo() {
        crearFicha();
        LocalDate inicio = LocalDate.now().plusDays(10);

        AusenciaResponse solicitada =
                ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.VACACIONES, inicio, inicio.plusDays(4)));
        assertThat(solicitada.estado()).isEqualTo(EstadoAusencia.SOLICITADA);
        assertThat(solicitada.dias()).isEqualTo(5);
        assertThat(ausenciaService.misAusencias(enfermera).saldo().pendientesAprobacion())
                .isEqualTo(5);

        assertThatThrownBy(() -> ausenciaService.solicitar(
                        enfermera, ausencia(TipoAusencia.PERMISO, inicio.plusDays(2), inicio.plusDays(3))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ausenciaService.solicitar(
                        enfermera, ausencia(TipoAusencia.PERMISO, inicio.plusDays(20), inicio.plusDays(19))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> ausenciaService.solicitar(
                        enfermera,
                        ausencia(TipoAusencia.PERMISO, LocalDate.now().minusDays(1), LocalDate.now())))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> ausenciaService.solicitar(
                        enfermera, ausencia(TipoAusencia.VACACIONES, inicio.plusDays(30), inicio.plusDays(60))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("saldo");
        ausenciaService.solicitar(
                enfermera,
                ausencia(
                        TipoAusencia.DESCANSO_MEDICO,
                        LocalDate.now().minusDays(3),
                        LocalDate.now().minusDays(2)));
    }

    @Test
    void sinFichaNoSePuedeSolicitar() {
        assertThatThrownBy(() -> ausenciaService.solicitar(
                        enfermera,
                        ausencia(
                                TipoAusencia.PERMISO,
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(3))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rrhhApruebaYRechazaYNotificaAlEmpleadoSinResolverLoPropio() {
        crearFicha();
        LocalDate inicio = LocalDate.now().plusDays(10);
        AusenciaResponse a =
                ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.VACACIONES, inicio, inicio.plusDays(2)));
        AusenciaResponse b = ausenciaService.solicitar(
                enfermera, ausencia(TipoAusencia.PERMISO, inicio.plusDays(20), inicio.plusDays(20)));

        AusenciaResponse aprobada = ausenciaService.aprobar(a.id(), "Autorizada", rrhh);
        assertThat(aprobada.estado()).isEqualTo(EstadoAusencia.APROBADA);
        assertThat(aprobada.resueltoPor()).isNotNull();
        assertThat(ausenciaService.misAusencias(enfermera).saldo().tomados()).isEqualTo(3);
        assertThatThrownBy(() -> ausenciaService.aprobar(a.id(), null, rrhh)).isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> ausenciaService.rechazar(b.id(), " ", rrhh))
                .isInstanceOf(DatosInvalidosException.class);
        AusenciaResponse rechazada = ausenciaService.rechazar(b.id(), "Cobertura insuficiente", rrhh);
        assertThat(rechazada.estado()).isEqualTo(EstadoAusencia.RECHAZADA);
        assertThat(rechazada.motivoResolucion()).isEqualTo("Cobertura insuficiente");

        assertThat(notificacionService.listar(enfermera, false, 50))
                .anyMatch(n -> n.tipo() == TipoNotificacion.AUSENCIA_RESUELTA);
    }

    @Test
    void avisaARrhhDeLasSolicitudesYNoDejaResolverLasPropias() {
        Usuario usuarioRrhh = fixture.usuario("rrhh2", RolNombre.ROLE_RRHH);
        UsuarioPrincipal otroRrhh = fixture.principal(usuarioRrhh);
        empleadoService.crear(
                new EmpleadoRequest(
                        usuarioRrhh.getId(),
                        "Analista",
                        AreaLaboral.RRHH,
                        TipoContrato.INDEFINIDO,
                        LocalDate.now().minusYears(2),
                        null,
                        null,
                        new BigDecimal("5000.00"),
                        null,
                        null,
                        30),
                rrhh);
        AusenciaResponse propia = ausenciaService.solicitar(
                otroRrhh,
                ausencia(
                        TipoAusencia.PERMISO,
                        LocalDate.now().plusDays(3),
                        LocalDate.now().plusDays(3)));

        assertThatThrownBy(() -> ausenciaService.aprobar(propia.id(), null, otroRrhh))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(notificacionService.listar(rrhh, false, 50))
                .anyMatch(n -> n.tipo() == TipoNotificacion.SOLICITUD_AUSENCIA);
    }

    @Test
    void cancelaSolicitudesPropiasYRrhhCancelaAprobadasFuturas() {
        crearFicha();
        LocalDate inicio = LocalDate.now().plusDays(10);
        AusenciaResponse a = ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.PERMISO, inicio, inicio));
        assertThat(ausenciaService.cancelarPropia(enfermera, a.id()).estado()).isEqualTo(EstadoAusencia.CANCELADA);
        assertThatThrownBy(() -> ausenciaService.cancelarPropia(enfermera, a.id()))
                .isInstanceOf(ConflictException.class);

        AusenciaResponse b = ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.PERMISO, inicio, inicio));
        ausenciaService.aprobar(b.id(), null, rrhh);
        assertThatThrownBy(() -> ausenciaService.cancelarPropia(enfermera, b.id()))
                .isInstanceOf(ConflictException.class);
        assertThat(ausenciaService.cancelar(b.id(), rrhh).estado()).isEqualTo(EstadoAusencia.CANCELADA);
    }

    @Test
    void unEmpleadoNoVeNiCancelaLasAusenciasDeOtro() {
        crearFicha();
        UsuarioPrincipal otra = fixture.principal(fixture.usuario("otra", RolNombre.ROLE_CAJA));
        empleadoService.crear(
                new EmpleadoRequest(
                        otra.getId(),
                        "Cajera",
                        AreaLaboral.CAJA,
                        TipoContrato.INDEFINIDO,
                        LocalDate.now().minusYears(1),
                        null,
                        null,
                        new BigDecimal("3000.00"),
                        null,
                        null,
                        30),
                rrhh);
        LocalDate inicio = LocalDate.now().plusDays(10);
        AusenciaResponse ajena = ausenciaService.solicitar(otra, ausencia(TipoAusencia.PERMISO, inicio, inicio));

        assertThatThrownBy(() -> ausenciaService.cancelarPropia(enfermera, ajena.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(ausenciaService.misAusencias(enfermera).ausencias()).isEmpty();
    }

    @Test
    void elCalendarioDeAusenciasOcultaLosMotivosAlAdministrador() {
        crearFicha();
        LocalDate inicio = LocalDate.now().plusDays(10);
        ausenciaService.solicitar(enfermera, ausencia(TipoAusencia.DESCANSO_MEDICO, inicio, inicio));

        assertThat(ausenciaService.listar(null, null, null, null, rrhh))
                .anySatisfy(a -> assertThat(a.motivo()).isEqualTo("Motivo de prueba"));
        assertThat(ausenciaService.listar(null, null, null, null, admin))
                .isNotEmpty()
                .allSatisfy(a -> assertThat(a.motivo()).isNull());
        assertThatThrownBy(() -> ausenciaService.listar(null, null, inicio, inicio.minusDays(1), rrhh))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void elResumenDeLaPlantillaRespetaLoQueVeCadaRol() {
        EmpleadoResponse emp = crearFicha();
        LocalDate hoy = LocalDate.now();
        empleadoService.crear(
                new EmpleadoRequest(
                        fixture.usuario("contrato", RolNombre.ROLE_CAJA).getId(),
                        "Cajero temporal",
                        AreaLaboral.CAJA,
                        TipoContrato.PLAZO_FIJO,
                        hoy.minusMonths(2),
                        hoy.plusDays(15),
                        null,
                        new BigDecimal("2500.00"),
                        null,
                        null,
                        30),
                rrhh);
        ausenciaService.aprobar(
                ausenciaService
                        .solicitar(enfermera, ausencia(TipoAusencia.DESCANSO_MEDICO, hoy, hoy.plusDays(1)))
                        .id(),
                null,
                rrhh);

        RrhhResumenResponse paraRrhh = empleadoService.resumen(rrhh);
        assertThat(paraRrhh.totalEmpleados()).isEqualTo(2);
        assertThat(paraRrhh.activos()).isEqualTo(2);
        assertThat(paraRrhh.masaSalarial()).isEqualByComparingTo("7300.00");
        assertThat(paraRrhh.contratosPorVencer()).hasSize(1);
        assertThat(paraRrhh.ausentesHoy()).isEqualTo(1);
        assertThat(paraRrhh.ausentesHoyDetalle()).hasSize(1);
        assertThat(paraRrhh.porArea()).hasSize(2);

        RrhhResumenResponse paraAdmin = empleadoService.resumen(admin);
        assertThat(paraAdmin.masaSalarial()).isNull();
        assertThat(paraAdmin.ausentesHoyDetalle()).hasSize(1);

        RrhhResumenResponse paraGerencia = empleadoService.resumen(gerencia);
        assertThat(paraGerencia.masaSalarial()).isEqualByComparingTo("7300.00");
        assertThat(paraGerencia.ausentesHoyDetalle()).isNull();
        assertThat(paraGerencia.ausentesHoy()).isEqualTo(1);
        assertThat(emp.id()).isNotNull();
    }

    @Test
    void elSaldoDeVacacionesSeCalculaPorEmpleado() {
        EmpleadoResponse emp = crearFicha();
        assertThat(ausenciaService.saldo(emp.id()).saldo()).isEqualTo(30);
        assertThatThrownBy(() -> ausenciaService.saldo(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
