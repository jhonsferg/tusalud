package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.CompletarEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.EventoCalendarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MoverEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ReprogramarRequest;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadRangoResponse;
import com.codesoftlabs.tusalud.app.dto.response.EventoCalendarioResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EventoCalendarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoCalendarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecordatorioService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
class EventoCalendarioServiceTest {

    @Autowired
    private EventoCalendarioService eventoService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private RecordatorioService recordatorioService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EventoCalendarioRepository eventoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal pacientePrincipal;
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        medicoPrincipal = fixture.principal(medico.getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
    }

    private EventoCalendarioRequest evento(TipoEvento tipo, LocalDateTime inicio, LocalDateTime fin) {
        return new EventoCalendarioRequest(
                "Evento " + tipo, "Detalle", "Sala", tipo, inicio, fin, false, null, false, null);
    }

    private EventoCalendarioResponse crear(UsuarioPrincipal actor, TipoEvento tipo, LocalDateTime inicio, int minutos) {
        return eventoService.crear(actor, evento(tipo, inicio, inicio.plusMinutes(minutos)));
    }

    @Test
    void creaYListaLosEventosPropiosDelRango() {
        crear(pacientePrincipal, TipoEvento.REUNION, lunes.atTime(10, 0), 60);
        crear(pacientePrincipal, TipoEvento.TAREA, lunes.plusDays(40).atTime(10, 0), 60);

        List<EventoCalendarioResponse> semana = eventoService.listar(pacientePrincipal, lunes, lunes.plusDays(6));

        assertThat(semana).hasSize(1);
        assertThat(semana.get(0).titulo()).isEqualTo("Evento REUNION");
        assertThat(semana.get(0).createdAt()).isNotNull();
    }

    @Test
    void incluyeLosEventosQueEmpiezanAntesYTerminanDentroDelRango() {
        eventoService.crear(
                pacientePrincipal,
                evento(TipoEvento.PERSONAL, lunes.minusDays(1).atTime(22, 0), lunes.atTime(2, 0)));

        assertThat(eventoService.listar(pacientePrincipal, lunes, lunes)).hasSize(1);
    }

    @Test
    void unUsuarioNoVeNiTocaLosEventosDeOtro() {
        EventoCalendarioResponse ajeno = crear(pacientePrincipal, TipoEvento.TAREA, lunes.atTime(9, 0), 30);

        assertThat(eventoService.listar(medicoPrincipal, lunes, lunes)).isEmpty();
        assertThatThrownBy(() -> eventoService.eliminar(medicoPrincipal, ajeno.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> eventoService.mover(
                        medicoPrincipal,
                        ajeno.id(),
                        new MoverEventoRequest(lunes.atTime(10, 0), lunes.atTime(11, 0), null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> eventoService.completar(medicoPrincipal, ajeno.id(), new CompletarEventoRequest(true)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> eventoService.actualizar(
                        medicoPrincipal, ajeno.id(), evento(TipoEvento.TAREA, lunes.atTime(9, 0), lunes.atTime(10, 0))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rechazaUnFinAnteriorAlInicioYUnaDuracionExcesiva() {
        assertThatThrownBy(() -> crear(pacientePrincipal, TipoEvento.REUNION, lunes.atTime(10, 0), -30))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> eventoService.crear(
                        pacientePrincipal,
                        evento(
                                TipoEvento.PERSONAL,
                                lunes.atTime(8, 0),
                                lunes.plusDays(32).atTime(8, 0))))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaUnRangoDeConsultaInvalido() {
        assertThatThrownBy(() -> eventoService.listar(pacientePrincipal, lunes, lunes.minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> eventoService.listar(pacientePrincipal, lunes, lunes.plusDays(100)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(eventoService.listar(pacientePrincipal, lunes, lunes.plusDays(99)))
                .isEmpty();
    }

    @Test
    void losEventosDeTodoElDiaSeNormalizanAlDiaCompleto() {
        EventoCalendarioResponse respuesta = eventoService.crear(
                pacientePrincipal,
                new EventoCalendarioRequest(
                        "Feriado",
                        null,
                        null,
                        TipoEvento.PERSONAL,
                        lunes.atTime(15, 30),
                        lunes.atTime(8, 0),
                        true,
                        null,
                        false,
                        null));

        assertThat(respuesta.inicio()).isEqualTo(lunes.atStartOfDay());
        assertThat(respuesta.fin()).isEqualTo(lunes.atTime(LocalTime.of(23, 59)));
        assertThat(respuesta.todoElDia()).isTrue();
    }

    @Test
    void soloLasTareasSeCompletan() {
        EventoCalendarioResponse tarea = crear(pacientePrincipal, TipoEvento.TAREA, lunes.atTime(9, 0), 30);
        EventoCalendarioResponse reunion = crear(pacientePrincipal, TipoEvento.REUNION, lunes.atTime(11, 0), 30);

        assertThat(eventoService
                        .completar(pacientePrincipal, tarea.id(), new CompletarEventoRequest(true))
                        .completado())
                .isTrue();
        assertThatThrownBy(() ->
                        eventoService.completar(pacientePrincipal, reunion.id(), new CompletarEventoRequest(true)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void mueveYEliminaUnEvento() {
        EventoCalendarioResponse creado = crear(pacientePrincipal, TipoEvento.REUNION, lunes.atTime(9, 0), 60);

        EventoCalendarioResponse movido = eventoService.mover(
                pacientePrincipal,
                creado.id(),
                new MoverEventoRequest(lunes.atTime(14, 0), lunes.atTime(15, 30), null));
        assertThat(movido.inicio()).isEqualTo(lunes.atTime(14, 0));
        assertThat(movido.fin()).isEqualTo(lunes.atTime(15, 30));

        eventoService.eliminar(pacientePrincipal, creado.id());
        assertThat(eventoRepository.findById(creado.id())).isEmpty();
    }

    @Test
    void soloLosMedicosCreanBloqueos() {
        assertThatThrownBy(() -> crear(pacientePrincipal, TipoEvento.BLOQUEO, lunes.atTime(9, 0), 60))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(9, 0), 60)
                        .tipo())
                .isEqualTo(TipoEvento.BLOQUEO);
    }

    @Test
    void elBloqueoQuitaLasHorasDeLaDisponibilidadDiariaYPorRango() {
        crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(9, 0), 60);

        assertThat(citaService.consultarDisponibilidad(medico.getId(), lunes).horariosDisponibles())
                .doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30))
                .contains(LocalTime.of(8, 30), LocalTime.of(10, 0));
        List<DisponibilidadRangoResponse> rango =
                citaService.consultarDisponibilidadRango(medico.getId(), lunes, lunes.plusDays(6));
        assertThat(rango).hasSize(1);
        assertThat(rango.get(0).horariosDisponibles())
                .doesNotContain(LocalTime.of(9, 0), LocalTime.of(9, 30))
                .contains(LocalTime.of(10, 0));
    }

    @Test
    void noSePuedeReservarNiReprogramarSobreUnBloqueo() {
        crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(9, 0), 60);
        Long especialidadId = medico.getEspecialidades().iterator().next().getId();

        assertThatThrownBy(() -> citaService.crearCita(
                        new CitaCreateRequest(medico.getId(), lunes.atTime(9, 30), "Control", especialidadId),
                        pacientePrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El médico no atiende en ese horario");

        Cita existente = fixture.cita(medico, paciente, lunes.atTime(11, 0), EstadoCita.PENDIENTE);
        assertThatThrownBy(() -> citaService.reprogramarCita(
                        existente.getId(), new ReprogramarRequest(lunes.atTime(9, 0)), pacientePrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El médico no atiende en ese horario");
    }

    @Test
    void noSeCreaNiSeMueveUnBloqueoSobreCitasActivas() {
        fixture.cita(medico, paciente, lunes.atTime(9, 0), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(8, 30), 60))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Hay citas activas en ese horario");

        EventoCalendarioResponse libre = crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(10, 0), 60);
        assertThatThrownBy(() -> eventoService.mover(
                        medicoPrincipal,
                        libre.id(),
                        new MoverEventoRequest(lunes.atTime(9, 15), lunes.atTime(10, 0), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void lasCitasCanceladasNoImpidenUnBloqueo() {
        fixture.cita(medico, paciente, lunes.atTime(9, 0), EstadoCita.CANCELADA);

        assertThat(crear(medicoPrincipal, TipoEvento.BLOQUEO, lunes.atTime(9, 0), 60)
                        .id())
                .isNotNull();
    }

    @Test
    void generaUnRecordatorioUnaSolaVezYLoReiniciaAlMoverElEvento() {
        LocalDateTime ahora = LocalDateTime.now();
        EventoCalendarioResponse creado = eventoService.crear(
                pacientePrincipal,
                new EventoCalendarioRequest(
                        "Llamar al laboratorio",
                        null,
                        null,
                        TipoEvento.RECORDATORIO,
                        ahora.plusMinutes(20),
                        ahora.plusMinutes(50),
                        false,
                        null,
                        false,
                        30));

        recordatorioService.enviarRecordatorios(ahora);
        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDeEventos()).hasSize(1);
        assertThat(eventoRepository.findById(creado.id()).orElseThrow().isRecordatorioEnviado())
                .isTrue();

        eventoService.mover(
                pacientePrincipal,
                creado.id(),
                new MoverEventoRequest(ahora.plusMinutes(25), ahora.plusMinutes(55), null));
        assertThat(eventoRepository.findById(creado.id()).orElseThrow().isRecordatorioEnviado())
                .isFalse();
        recordatorioService.enviarRecordatorios(ahora);
        assertThat(recordatoriosDeEventos()).hasSize(2);
    }

    @Test
    void noAvisaSiTodaviaFaltaMasTiempoQueElRecordatorio() {
        LocalDateTime ahora = LocalDateTime.now();
        eventoService.crear(
                pacientePrincipal,
                new EventoCalendarioRequest(
                        "Reunión",
                        null,
                        null,
                        TipoEvento.REUNION,
                        ahora.plusHours(5),
                        ahora.plusHours(6),
                        false,
                        null,
                        false,
                        30));

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDeEventos()).isEmpty();
    }

    private List<?> recordatoriosDeEventos() {
        return notificacionService.listar(pacientePrincipal, false, 100).stream()
                .filter(n -> n.tipo() == TipoNotificacion.RECORDATORIO_EVENTO)
                .toList();
    }
}
