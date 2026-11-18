package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AltaHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaAsignacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EmergenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EvolucionHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoHospCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.TriajeRequest;
import com.codesoftlabs.tusalud.app.dto.response.CamaResponse;
import com.codesoftlabs.tusalud.app.dto.response.EvolucionHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.HospResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.MapaCamasResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.ClasificacionTriaje;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HospitalizacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class HospitalizacionServiceTest {

    @Autowired
    private HospitalizacionService hospitalizacionService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private IngresoHospitalarioRepository ingresoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private Paciente otroPaciente;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal otroPacientePrincipal;
    private UsuarioPrincipal enfermeria;
    private UsuarioPrincipal admin;
    private UsuarioPrincipal recepcion;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        otroPaciente = fixture.paciente();
        medicoPrincipal = fixture.principal(medico.getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        otroPacientePrincipal = fixture.principal(otroPaciente.getUsuario());
        enfermeria = fixture.principal(fixture.usuario("enf", RolNombre.ROLE_ENFERMERIA));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        recepcion = fixture.principal(fixture.usuario("recep", RolNombre.ROLE_RECEPCION));
    }

    private CamaResponse cama(AreaCama area, String precio) {
        return hospitalizacionService.crearCama(new CamaRequest(
                "T-" + ServiceTestFixture.siguiente(), area, 1, "Cama de prueba", new BigDecimal(precio), true));
    }

    private IngresoHospResponse ingresar(Paciente destino, CamaResponse cama) {
        fixture.cita(medico, destino, LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        return hospitalizacionService.crearIngreso(
                medicoPrincipal,
                new IngresoHospCreateRequest(destino.getId(), cama.id(), "Motivo de prueba", null, null));
    }

    private static TriajeRequest triaje(int nivel) {
        return new TriajeRequest(
                nivel, "120/80", 90, 18, new BigDecimal("37.0"), 97, 15, "Motivo de emergencia nivel " + nivel);
    }

    private static EvolucionHospRequest evolucion(TipoEvolucion tipo, String nota) {
        return new EvolucionHospRequest(tipo, nota, "120/80", 80, new BigDecimal("36.8"), 98);
    }

    private void retrocederIngreso(UUID id, long dias, long horas) {
        IngresoHospitalario ingreso = ingresoRepository.findById(id).orElseThrow();
        LocalDateTime antes = LocalDateTime.now().minusDays(dias).minusHours(horas);
        ingreso.setFechaIngreso(antes);
        ingreso.setDesdeEnCama(antes);
        ingresoRepository.saveAndFlush(ingreso);
    }

    private List<NotificacionResponse> notificaciones(UsuarioPrincipal actor) {
        return notificacionService.listar(actor, false, 20);
    }

    @Test
    void creaCamasConCodigoUnicoYLasListaPorAreaYEstado() {
        CamaResponse uci = cama(AreaCama.UCI, "650.00");
        CamaResponse general = cama(AreaCama.HOSPITALIZACION, "150.00");

        assertThat(uci.estado()).isEqualTo(EstadoCama.LIBRE);
        assertThat(uci.activa()).isTrue();
        assertThatThrownBy(() -> hospitalizacionService.crearCama(new CamaRequest(
                        uci.codigo().toLowerCase(), AreaCama.UCI, 1, null, new BigDecimal("10.00"), true)))
                .isInstanceOf(ConflictException.class);
        assertThat(hospitalizacionService.listarCamas(AreaCama.UCI, null))
                .extracting(CamaResponse::id)
                .contains(uci.id())
                .doesNotContain(general.id());
        assertThat(hospitalizacionService.listarCamas(null, EstadoCama.MANTENIMIENTO))
                .extracting(CamaResponse::id)
                .doesNotContain(uci.id());
    }

    @Test
    void elIngresoOcupaLaCamaNotificaAlPacienteYNoAdmiteUnSegundoIngreso() {
        CamaResponse libre = cama(AreaCama.HOSPITALIZACION, "150.00");
        CamaResponse otra = cama(AreaCama.HOSPITALIZACION, "150.00");

        IngresoHospResponse ingreso = ingresar(paciente, libre);

        assertThat(ingreso.codigo()).startsWith("HOS-");
        assertThat(ingreso.estado()).isEqualTo(EstadoIngresoHosp.ACTIVO);
        assertThat(ingreso.tipo()).isEqualTo(TipoIngresoHosp.PROGRAMADO);
        assertThat(ingreso.medicoId()).isEqualTo(medico.getId());
        assertThat(ingreso.camaCodigo()).isEqualTo(libre.codigo());
        assertThat(hospitalizacionService.listarCamas(null, EstadoCama.OCUPADA))
                .filteredOn(c -> c.id().equals(libre.id()))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.ingresoActivoId()).isEqualTo(ingreso.id());
                    assertThat(c.pacienteNombre())
                            .isEqualTo(paciente.getUsuario().getNombreCompleto());
                });
        assertThat(notificaciones(pacientePrincipal)).anyMatch(n -> n.tipo() == TipoNotificacion.INGRESO_HOSPITALARIO);

        assertThatThrownBy(() -> ingresar(paciente, otra)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ingresar(otroPaciente, libre)).isInstanceOf(ConflictException.class);
    }

    @Test
    void noIngresaEnUnaCamaDesactivadaOEnMantenimiento() {
        CamaResponse desactivada = hospitalizacionService.crearCama(new CamaRequest(
                "T-" + ServiceTestFixture.siguiente(),
                AreaCama.HOSPITALIZACION,
                1,
                null,
                new BigDecimal("90.00"),
                false));
        CamaResponse mantenimiento = cama(AreaCama.HOSPITALIZACION, "90.00");
        hospitalizacionService.cambiarEstadoCama(mantenimiento.id(), new CamaEstadoRequest(EstadoCama.MANTENIMIENTO));

        assertThatThrownBy(() -> ingresar(paciente, desactivada)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> ingresar(paciente, mantenimiento)).isInstanceOf(ConflictException.class);
    }

    @Test
    void trasladarLiberaLaCamaAnteriorALimpiezaYOcupaLaNueva() {
        CamaResponse origen = cama(AreaCama.HOSPITALIZACION, "100.00");
        CamaResponse destino = cama(AreaCama.UCI, "300.00");
        IngresoHospResponse ingreso = ingresar(paciente, origen);

        IngresoHospResponse trasladado =
                hospitalizacionService.trasladar(ingreso.id(), new CamaAsignacionRequest(destino.id()));

        assertThat(trasladado.camaId()).isEqualTo(destino.id());
        assertThat(hospitalizacionService.listarCamas(null, null))
                .filteredOn(c -> c.id().equals(origen.id()))
                .singleElement()
                .satisfies(c -> assertThat(c.estado()).isEqualTo(EstadoCama.LIMPIEZA));
        assertThat(hospitalizacionService.listarCamas(null, null))
                .filteredOn(c -> c.id().equals(destino.id()))
                .singleElement()
                .satisfies(c -> assertThat(c.estado()).isEqualTo(EstadoCama.OCUPADA));
        assertThatThrownBy(
                        () -> hospitalizacionService.trasladar(ingreso.id(), new CamaAsignacionRequest(destino.id())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elCargoDeLaCamaSeAcumulaPorDiasYSumaLosTramosDeLosTraslados() {
        CamaResponse origen = cama(AreaCama.HOSPITALIZACION, "100.00");
        CamaResponse destino = cama(AreaCama.UCI, "300.00");
        IngresoHospResponse ingreso = ingresar(paciente, origen);
        assertThat(ingreso.diasEstancia()).isEqualTo(1);
        assertThat(ingreso.cargoCama()).isEqualByComparingTo("100.00");

        retrocederIngreso(ingreso.id(), 2, 12);
        IngresoHospResponse tresDias = hospitalizacionService.obtenerIngreso(admin, ingreso.id());
        assertThat(tresDias.diasEstancia()).isEqualTo(3);
        assertThat(tresDias.cargoCama()).isEqualByComparingTo("300.00");

        IngresoHospResponse trasladado =
                hospitalizacionService.trasladar(ingreso.id(), new CamaAsignacionRequest(destino.id()));
        assertThat(trasladado.cargoCama()).isEqualByComparingTo("600.00");

        IngresoHospResponse alta = hospitalizacionService.darAlta(
                medicoPrincipal, ingreso.id(), new AltaHospRequest("Recuperado", "Control en 7 días"));
        assertThat(alta.cargoCama()).isEqualByComparingTo("600.00");
        assertThat(alta.diasEstancia()).isEqualTo(3);
    }

    @Test
    void elAltaCierraElIngresoDejaLaCamaEnLimpiezaYNotificaAlPaciente() {
        CamaResponse cama = cama(AreaCama.HOSPITALIZACION, "120.00");
        IngresoHospResponse ingreso = ingresar(paciente, cama);

        IngresoHospResponse alta = hospitalizacionService.darAlta(
                medicoPrincipal, ingreso.id(), new AltaHospRequest("Neumonía resuelta", "Reposo por una semana"));

        assertThat(alta.estado()).isEqualTo(EstadoIngresoHosp.ALTA);
        assertThat(alta.fechaAlta()).isNotNull();
        assertThat(alta.diagnosticoAlta()).isEqualTo("Neumonía resuelta");
        assertThat(alta.cargoCama()).isEqualByComparingTo("120.00");
        assertThat(hospitalizacionService.listarCamas(null, EstadoCama.LIMPIEZA))
                .extracting(CamaResponse::id)
                .contains(cama.id());
        assertThat(notificaciones(pacientePrincipal)).anyMatch(n -> n.tipo() == TipoNotificacion.ALTA_HOSPITALARIA);
        assertThatThrownBy(() ->
                        hospitalizacionService.darAlta(medicoPrincipal, ingreso.id(), new AltaHospRequest("X", "Y")))
                .isInstanceOf(ConflictException.class);
        assertThat(ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "120.00")).id())
                .isNotNull();
    }

    @Test
    void cadaRolSoloRegistraSuTipoDeEvolucion() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));

        EvolucionHospResponse medica = hospitalizacionService.registrarEvolucion(
                medicoPrincipal, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "Evolución favorable"));
        EvolucionHospResponse deEnfermeria = hospitalizacionService.registrarEvolucion(
                enfermeria, ingreso.id(), evolucion(TipoEvolucion.ENFERMERIA, "Signos estables"));

        assertThat(medica.tipo()).isEqualTo(TipoEvolucion.MEDICA);
        assertThat(deEnfermeria.autorNombre()).isEqualTo(enfermeria.getNombreCompleto());
        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        enfermeria, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "No corresponde")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        medicoPrincipal, ingreso.id(), evolucion(TipoEvolucion.ENFERMERIA, "No corresponde")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        admin, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "Administración no escribe")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(hospitalizacionService.listarEvoluciones(enfermeria, ingreso.id()))
                .hasSize(2);
    }

    @Test
    void elPacienteSoloVeLasEvolucionesMedicasResumidasYSoloLasSuyas() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        String larga = "Evolución detallada. ".repeat(40);
        hospitalizacionService.registrarEvolucion(
                medicoPrincipal, ingreso.id(), evolucion(TipoEvolucion.MEDICA, larga));
        hospitalizacionService.registrarEvolucion(
                enfermeria, ingreso.id(), evolucion(TipoEvolucion.ENFERMERIA, "Nota interna de enfermería"));

        List<EvolucionHospResponse> vistaPaciente =
                hospitalizacionService.listarEvoluciones(pacientePrincipal, ingreso.id());

        assertThat(vistaPaciente).hasSize(1);
        assertThat(vistaPaciente.get(0).tipo()).isEqualTo(TipoEvolucion.MEDICA);
        assertThat(vistaPaciente.get(0).nota().length()).isLessThan(larga.length());
        assertThat(vistaPaciente.get(0).presionArterial()).isNull();
        assertThatThrownBy(() -> hospitalizacionService.listarEvoluciones(otroPacientePrincipal, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.obtenerIngreso(otroPacientePrincipal, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(hospitalizacionService
                        .obtenerIngreso(pacientePrincipal, ingreso.id())
                        .id())
                .isEqualTo(ingreso.id());
    }

    @Test
    void noSeRegistranEvolucionesEnUnIngresoCerrado() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        hospitalizacionService.darAlta(medicoPrincipal, ingreso.id(), new AltaHospRequest("Resuelto", "Alta médica"));

        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        medicoPrincipal, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "Tarde")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void evolucionesPendientesListaLosIngresosSinRegistroDeEnfermeriaReciente() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        assertThat(hospitalizacionService.evolucionesPendientes(8))
                .extracting(com.codesoftlabs.tusalud.app.dto.response.EvolucionPendienteResponse::ingresoId)
                .doesNotContain(ingreso.id());

        retrocederIngreso(ingreso.id(), 0, 10);

        var pendientes = hospitalizacionService.evolucionesPendientes(8);
        assertThat(pendientes)
                .extracting(com.codesoftlabs.tusalud.app.dto.response.EvolucionPendienteResponse::ingresoId)
                .contains(ingreso.id());
        assertThat(pendientes.stream()
                        .filter(item -> item.ingresoId().equals(ingreso.id()))
                        .findFirst()
                        .orElseThrow()
                        .horasSinRegistro())
                .isGreaterThanOrEqualTo(10);

        hospitalizacionService.registrarEvolucion(
                enfermeria, ingreso.id(), evolucion(TipoEvolucion.ENFERMERIA, "Control de turno"));

        assertThat(hospitalizacionService.evolucionesPendientes(8))
                .extracting(com.codesoftlabs.tusalud.app.dto.response.EvolucionPendienteResponse::ingresoId)
                .doesNotContain(ingreso.id());
        assertThatThrownBy(() -> hospitalizacionService.evolucionesPendientes(0))
                .isInstanceOf(com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException.class);
    }

    @Test
    void cancelarIngresoExigeYGuardaElMotivo() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));

        assertThatThrownBy(() -> hospitalizacionService.cancelarIngreso(ingreso.id(), "no"))
                .isInstanceOf(com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException.class);
        IngresoHospResponse cancelado =
                hospitalizacionService.cancelarIngreso(ingreso.id(), "  Ingreso   registrado por error ");

        assertThat(cancelado.motivoCancelacion()).isEqualTo("Ingreso registrado por error");
    }

    @Test
    void cancelarLiberaLaCamaSoloSiNoHayEvoluciones() {
        CamaResponse cama = cama(AreaCama.HOSPITALIZACION, "100.00");
        IngresoHospResponse sinNotas = ingresar(paciente, cama);

        IngresoHospResponse cancelado =
                hospitalizacionService.cancelarIngreso(sinNotas.id(), "Ingreso registrado por error");

        assertThat(cancelado.estado()).isEqualTo(EstadoIngresoHosp.CANCELADO);
        assertThat(hospitalizacionService.listarCamas(null, EstadoCama.LIBRE))
                .extracting(CamaResponse::id)
                .contains(cama.id());

        IngresoHospResponse conNotas = ingresar(otroPaciente, cama);
        hospitalizacionService.registrarEvolucion(
                medicoPrincipal, conNotas.id(), evolucion(TipoEvolucion.MEDICA, "Ya hay evolución"));
        assertThatThrownBy(() -> hospitalizacionService.cancelarIngreso(conNotas.id(), "Ingreso registrado por error"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void lasEmergenciasSeOrdenanPorNivelYAntiguedadYAvisanAMedicosSoloSiSonCriticas() {
        Paciente tercero = fixture.paciente();
        IngresoHospResponse leve = hospitalizacionService.registrarEmergencia(
                enfermeria, new EmergenciaRequest(paciente.getId(), triaje(4)));
        IngresoHospResponse critica = hospitalizacionService.registrarEmergencia(
                recepcion, new EmergenciaRequest(otroPaciente.getId(), triaje(2)));
        IngresoHospResponse otraLeve =
                hospitalizacionService.registrarEmergencia(admin, new EmergenciaRequest(tercero.getId(), triaje(4)));
        IngresoHospitalario primera = ingresoRepository.findById(leve.id()).orElseThrow();
        primera.setFechaIngreso(LocalDateTime.now().minusMinutes(50));
        ingresoRepository.saveAndFlush(primera);

        List<IngresoHospResponse> cola = hospitalizacionService.listarEmergencias(enfermeria);

        assertThat(cola)
                .extracting(IngresoHospResponse::id)
                .containsSubsequence(critica.id(), leve.id(), otraLeve.id());
        assertThat(cola.stream()
                        .filter(e -> e.id().equals(leve.id()))
                        .findFirst()
                        .orElseThrow()
                        .esperaMinutos())
                .isGreaterThanOrEqualTo(49L);
        assertThat(critica.triaje().clasificacion()).isEqualTo(ClasificacionTriaje.NARANJA);
        assertThat(critica.tipo()).isEqualTo(TipoIngresoHosp.EMERGENCIA);
        assertThat(critica.camaId()).isNull();
        assertThat(notificaciones(medicoPrincipal))
                .filteredOn(n -> n.tipo() == TipoNotificacion.EMERGENCIA_TRIAJE)
                .hasSize(1);
        assertThatThrownBy(() -> hospitalizacionService.registrarEmergencia(
                        enfermeria, new EmergenciaRequest(paciente.getId(), triaje(1))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void asignarCamaAUnaEmergenciaExigeUnaCamaDeAreaDeUrgencia() {
        IngresoHospResponse emergencia = hospitalizacionService.registrarEmergencia(
                enfermeria, new EmergenciaRequest(paciente.getId(), triaje(1)));
        CamaResponse general = cama(AreaCama.HOSPITALIZACION, "100.00");
        CamaResponse box = cama(AreaCama.EMERGENCIA, "250.00");

        assertThat(emergencia.medicoId()).isNull();
        assertThatThrownBy(() ->
                        hospitalizacionService.asignarCama(emergencia.id(), new CamaAsignacionRequest(general.id())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> hospitalizacionService.trasladar(emergencia.id(), new CamaAsignacionRequest(box.id())))
                .isInstanceOf(ConflictException.class);

        IngresoHospResponse asignada =
                hospitalizacionService.asignarCama(emergencia.id(), new CamaAsignacionRequest(box.id()));
        assertThat(asignada.camaId()).isEqualTo(box.id());
        assertThat(asignada.esperaMinutos()).isNull();
        assertThatThrownBy(
                        () -> hospitalizacionService.asignarCama(emergencia.id(), new CamaAsignacionRequest(box.id())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unaCamaOcupadaNoCambiaDeEstadoYUnaLibreNoSeOcupaAMano() {
        CamaResponse cama = cama(AreaCama.HOSPITALIZACION, "100.00");

        assertThatThrownBy(() ->
                        hospitalizacionService.cambiarEstadoCama(cama.id(), new CamaEstadoRequest(EstadoCama.OCUPADA)))
                .isInstanceOf(ConflictException.class);
        assertThat(hospitalizacionService
                        .cambiarEstadoCama(cama.id(), new CamaEstadoRequest(EstadoCama.MANTENIMIENTO))
                        .estado())
                .isEqualTo(EstadoCama.MANTENIMIENTO);
        assertThat(hospitalizacionService
                        .cambiarEstadoCama(cama.id(), new CamaEstadoRequest(EstadoCama.LIBRE))
                        .estado())
                .isEqualTo(EstadoCama.LIBRE);

        ingresar(paciente, cama);
        assertThatThrownBy(() ->
                        hospitalizacionService.cambiarEstadoCama(cama.id(), new CamaEstadoRequest(EstadoCama.LIBRE)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> hospitalizacionService.cambiarEstadoCama(
                        cama.id(), new CamaEstadoRequest(EstadoCama.MANTENIMIENTO)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elMapaYElResumenReflejanLaOcupacion() {
        HospResumenResponse antes = hospitalizacionService.resumen();
        CamaResponse cama = cama(AreaCama.OBSERVACION, "120.00");
        cama(AreaCama.OBSERVACION, "120.00");
        ingresar(paciente, cama);
        hospitalizacionService.registrarEmergencia(enfermeria, new EmergenciaRequest(otroPaciente.getId(), triaje(3)));

        HospResumenResponse despues = hospitalizacionService.resumen();
        MapaCamasResponse mapa = hospitalizacionService.mapa();

        assertThat(despues.camasOcupadas()).isEqualTo(antes.camasOcupadas() + 1);
        assertThat(despues.camasLibres()).isEqualTo(antes.camasLibres() + 1);
        assertThat(despues.ingresosActivos()).isEqualTo(antes.ingresosActivos() + 2);
        assertThat(despues.emergenciasEnEspera()).isEqualTo(antes.emergenciasEnEspera() + 1);
        assertThat(despues.ocupacionPorcentaje()).isBetween(0.0, 100.0);
        assertThat(mapa.areas()).extracting(MapaCamasResponse.AreaMapa::area).containsExactly(AreaCama.values());
        MapaCamasResponse.AreaMapa observacion = mapa.areas().stream()
                .filter(a -> a.area() == AreaCama.OBSERVACION)
                .findFirst()
                .orElseThrow();
        assertThat(observacion.total()).isEqualTo(2);
        assertThat(observacion.ocupadas()).isEqualTo(1);
        assertThat(observacion.libres()).isEqualTo(1);
    }

    @Test
    void listaIngresosPorEstadoYBusquedaYElPacienteVeLosSuyos() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        hospitalizacionService.registrarEmergencia(enfermeria, new EmergenciaRequest(otroPaciente.getId(), triaje(5)));

        assertThat(hospitalizacionService.listarIngresos(enfermeria, EstadoIngresoHosp.ACTIVO, paciente.getDni()))
                .extracting(IngresoHospResponse::id)
                .containsExactly(ingreso.id());
        assertThat(hospitalizacionService.listarIngresos(enfermeria, EstadoIngresoHosp.ALTA, paciente.getDni()))
                .isEmpty();
        assertThat(hospitalizacionService.misIngresos(pacientePrincipal))
                .extracting(IngresoHospResponse::id)
                .containsExactly(ingreso.id());
        assertThat(hospitalizacionService.misIngresos(otroPacientePrincipal)).hasSize(1);
    }

    @Test
    void unMedicoSinRelacionNoPuedeIngresarAUnPaciente() {
        CamaResponse cama = cama(AreaCama.HOSPITALIZACION, "100.00");
        Medico ajeno = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));

        assertThatThrownBy(() -> hospitalizacionService.crearIngreso(
                        fixture.principal(ajeno.getUsuario()),
                        new IngresoHospCreateRequest(paciente.getId(), cama.id(), "Sin relación", null, null)))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(hospitalizacionService.listarCamas(null, EstadoCama.OCUPADA))
                .extracting(CamaResponse::id)
                .doesNotContain(cama.id());
    }

    @Test
    void unMedicoAjenoNoVeNiEscribeNiDaDeAltaUnIngreso() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        Medico ajeno = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        UsuarioPrincipal principalAjeno = fixture.principal(ajeno.getUsuario());

        assertThatThrownBy(() -> hospitalizacionService.obtenerIngreso(principalAjeno, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.listarEvoluciones(principalAjeno, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        principalAjeno, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "No es mi paciente")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.darAlta(
                        principalAjeno, ingreso.id(), new AltaHospRequest("Alta ajena", "Indicaciones")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(hospitalizacionService.listarIngresos(principalAjeno, EstadoIngresoHosp.ACTIVO, null))
                .extracting(IngresoHospResponse::id)
                .doesNotContain(ingreso.id());
        assertThat(hospitalizacionService.listarIngresos(medicoPrincipal, EstadoIngresoHosp.ACTIVO, null))
                .extracting(IngresoHospResponse::id)
                .contains(ingreso.id());
    }

    @Test
    void elAdministradorNoDaDeAltaNiRecepcionVeLasEvoluciones() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        hospitalizacionService.registrarEvolucion(
                medicoPrincipal, ingreso.id(), evolucion(TipoEvolucion.MEDICA, "Reservado"));

        assertThatThrownBy(() -> hospitalizacionService.darAlta(
                        admin, ingreso.id(), new AltaHospRequest("Administrador", "No corresponde")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.listarEvoluciones(recepcion, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.listarEvoluciones(admin, ingreso.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void recepcionYAdministracionVenElIngresoSinDatosClinicos() {
        IngresoHospResponse ingreso = ingresar(paciente, cama(AreaCama.HOSPITALIZACION, "100.00"));
        IngresoHospResponse emergencia = hospitalizacionService.registrarEmergencia(
                enfermeria, new EmergenciaRequest(otroPaciente.getId(), triaje(2)));

        for (UsuarioPrincipal administrativo : List.of(recepcion, admin)) {
            IngresoHospResponse visto = hospitalizacionService.obtenerIngreso(administrativo, ingreso.id());
            assertThat(visto.pacienteNombre()).isNotBlank();
            assertThat(visto.camaCodigo()).isNotNull();
            assertThat(visto.motivo()).isNull();
            assertThat(visto.diagnosticoIngreso()).isNull();
            assertThat(visto.diagnosticoAlta()).isNull();
            assertThat(visto.indicacionesAlta()).isNull();

            IngresoHospResponse enCola = hospitalizacionService.listarEmergencias(administrativo).stream()
                    .filter(item -> item.id().equals(emergencia.id()))
                    .findFirst()
                    .orElseThrow();
            assertThat(enCola.triaje().nivel()).isEqualTo(2);
            assertThat(enCola.triaje().motivoConsulta()).isNull();
            assertThat(enCola.triaje().presionArterial()).isNull();
            assertThat(enCola.triaje().frecuenciaCardiaca()).isNull();
            assertThat(enCola.motivo()).isNull();
        }
        assertThat(hospitalizacionService
                        .obtenerIngreso(enfermeria, ingreso.id())
                        .motivo())
                .isEqualTo("Motivo de prueba");
    }

    @Test
    void unaEmergenciaSinMedicoLaReclamaElPrimeroQueEvolucionaYLuegoEsSoloSuya() {
        IngresoHospResponse emergencia = hospitalizacionService.registrarEmergencia(
                enfermeria, new EmergenciaRequest(otroPaciente.getId(), triaje(3)));
        assertThat(emergencia.medicoId()).isNull();
        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        UsuarioPrincipal principalOtro = fixture.principal(otro.getUsuario());

        hospitalizacionService.registrarEvolucion(
                medicoPrincipal, emergencia.id(), evolucion(TipoEvolucion.MEDICA, "Tomo el caso"));

        assertThat(hospitalizacionService
                        .obtenerIngreso(medicoPrincipal, emergencia.id())
                        .medicoId())
                .isEqualTo(medico.getId());
        assertThatThrownBy(() -> hospitalizacionService.registrarEvolucion(
                        principalOtro, emergencia.id(), evolucion(TipoEvolucion.MEDICA, "También lo quiero")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> hospitalizacionService.obtenerIngreso(principalOtro, emergencia.id()))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
