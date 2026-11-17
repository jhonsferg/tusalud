package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.TurnoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaHoyResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PantallaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecepcionResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecepcionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class RecepcionServiceTest {

    @Autowired
    private RecepcionService recepcionService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal recepcion;
    private UsuarioPrincipal admin;
    private UsuarioPrincipal medicoPrincipal;
    private UsuarioPrincipal pacientePrincipal;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        recepcion = fixture.principal(fixture.usuario("recepcion", RolNombre.ROLE_RECEPCION));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        medicoPrincipal = fixture.principal(medico.getUsuario());
        pacientePrincipal = fixture.principal(paciente.getUsuario());
    }

    private Cita citaHoy(Medico delMedico, Paciente delPaciente, EstadoCita estado) {
        return fixture.cita(delMedico, delPaciente, LocalDate.now().atTime(9, 0), estado);
    }

    private TurnoResponse turno(Paciente delPaciente, PrioridadTurno prioridad) {
        return recepcionService.crearTurno(
                new TurnoCreateRequest(delPaciente.getId(), medico.getId(), prioridad, null));
    }

    @Test
    void elFlujoCompletoDeUnaCitaDesdeLaLlegadaHastaLaAtencion() {
        Cita cita = citaHoy(medico, paciente, EstadoCita.CONFIRMADA);

        TurnoResponse espera = recepcionService.registrarLlegada(cita.getId());

        assertThat(espera.numero()).isEqualTo("A-001");
        assertThat(espera.estado()).isEqualTo(EstadoTurno.EN_ESPERA);
        assertThat(espera.citaId()).isEqualTo(cita.getId());
        assertThat(espera.medicoId()).isEqualTo(medico.getId());
        assertThat(espera.consultorio()).isEqualTo("Consultorio 1");
        assertThat(espera.pacienteNombre()).isEqualTo(paciente.getUsuario().getNombreCompleto());
        assertThat(notificacionService.listar(medicoPrincipal, false, 10))
                .extracting(NotificacionResponse::tipo)
                .contains(TipoNotificacion.PACIENTE_EN_ESPERA);

        TurnoResponse llamado = recepcionService.llamar(espera.id(), recepcion);
        assertThat(llamado.estado()).isEqualTo(EstadoTurno.LLAMADO);
        assertThat(llamado.llamadoEn()).isNotNull();
        assertThat(notificacionService.listar(pacientePrincipal, false, 10))
                .filteredOn(n -> n.tipo() == TipoNotificacion.TURNO_LLAMADO)
                .singleElement()
                .satisfies(n -> assertThat(n.mensaje()).contains("A-001").contains("Consultorio 1"));

        TurnoResponse enAtencion = recepcionService.iniciar(espera.id(), medicoPrincipal);
        assertThat(enAtencion.estado()).isEqualTo(EstadoTurno.EN_ATENCION);
        assertThat(enAtencion.inicioAtencionEn()).isNotNull();

        TurnoResponse atendido = recepcionService.finalizar(espera.id(), medicoPrincipal);
        assertThat(atendido.estado()).isEqualTo(EstadoTurno.ATENDIDO);
        assertThat(atendido.finAtencionEn()).isNotNull();

        RecepcionResumenResponse resumen = recepcionService.resumen();
        assertThat(resumen.atendidosHoy()).isEqualTo(1);
        assertThat(resumen.enEspera()).isZero();
        assertThat(resumen.citasPendientesDeLlegada()).isZero();
        assertThat(recepcionService.cola(null, null, recepcion)).isEmpty();
        assertThat(recepcionService.cola(EstadoTurno.ATENDIDO, null, recepcion)).hasSize(1);
    }

    @Test
    void laLlegadaSoloSeRegistraUnaVezYParaCitasDeHoyActivas() {
        Cita cita = citaHoy(medico, paciente, EstadoCita.PENDIENTE);
        recepcionService.registrarLlegada(cita.getId());

        assertThatThrownBy(() -> recepcionService.registrarLlegada(cita.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya registró su llegada");

        Cita manana =
                fixture.cita(medico, fixture.paciente(), LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
        assertThatThrownBy(() -> recepcionService.registrarLlegada(manana.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("mismo día");

        Cita atendida = citaHoy(medico, fixture.paciente(), EstadoCita.ATENDIDA);
        assertThatThrownBy(() -> recepcionService.registrarLlegada(atendida.getId()))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> recepcionService.registrarLlegada(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laNumeracionEsPorDiaYPorPrioridadYLaColaSeOrdenaPorPrioridad() {
        TurnoResponse normal1 = turno(paciente, PrioridadTurno.NORMAL);
        TurnoResponse urgente = turno(fixture.paciente(), PrioridadTurno.URGENTE);
        TurnoResponse normal2 = turno(fixture.paciente(), PrioridadTurno.NORMAL);
        TurnoResponse preferente = turno(fixture.paciente(), PrioridadTurno.PREFERENTE);

        assertThat(normal1.numero()).isEqualTo("A-001");
        assertThat(normal2.numero()).isEqualTo("A-002");
        assertThat(urgente.numero()).isEqualTo("U-001");
        assertThat(preferente.numero()).isEqualTo("P-001");

        List<TurnoResponse> cola = recepcionService.cola(null, null, recepcion);
        assertThat(cola).extracting(TurnoResponse::numero).containsExactly("U-001", "P-001", "A-001", "A-002");
        assertThat(recepcionService.cola(null, medico.getId() + 1_000, admin)).isEmpty();
    }

    @Test
    void unPacienteNoPuedeTenerDosTurnosAbiertosAlMismoTiempo() {
        TurnoResponse primero = turno(paciente, PrioridadTurno.NORMAL);

        assertThatThrownBy(() -> turno(paciente, PrioridadTurno.URGENTE))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("turno abierto");

        recepcionService.cancelar(primero.id(), recepcion);
        assertThat(turno(paciente, PrioridadTurno.NORMAL).numero()).isEqualTo("A-002");
    }

    @Test
    void lasTransicionesInvalidasResponden409() {
        TurnoResponse enEspera = turno(paciente, PrioridadTurno.NORMAL);

        assertThatThrownBy(() -> recepcionService.finalizar(enEspera.id(), medicoPrincipal))
                .isInstanceOf(ConflictException.class);

        recepcionService.llamar(enEspera.id(), recepcion);
        assertThatThrownBy(() -> recepcionService.llamar(enEspera.id(), recepcion))
                .isInstanceOf(ConflictException.class);

        recepcionService.iniciar(enEspera.id(), medicoPrincipal);
        assertThatThrownBy(() -> recepcionService.iniciar(enEspera.id(), medicoPrincipal))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> recepcionService.marcarAusente(enEspera.id(), recepcion))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> recepcionService.cancelar(enEspera.id(), recepcion))
                .isInstanceOf(ConflictException.class);

        recepcionService.finalizar(enEspera.id(), medicoPrincipal);
        assertThatThrownBy(() -> recepcionService.llamar(enEspera.id(), recepcion))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> recepcionService.finalizar(enEspera.id(), medicoPrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void ausenteYCancelarCierranLosTurnosEnEsperaOLlamados() {
        TurnoResponse uno = turno(paciente, PrioridadTurno.NORMAL);
        TurnoResponse dos = turno(fixture.paciente(), PrioridadTurno.NORMAL);
        recepcionService.llamar(uno.id(), recepcion);

        assertThat(recepcionService.marcarAusente(uno.id(), recepcion).estado()).isEqualTo(EstadoTurno.AUSENTE);
        assertThat(recepcionService.cancelar(dos.id(), admin).estado()).isEqualTo(EstadoTurno.CANCELADO);

        RecepcionResumenResponse resumen = recepcionService.resumen();
        assertThat(resumen.ausentesHoy()).isEqualTo(1);
        assertThat(resumen.enEspera()).isZero();
    }

    @Test
    void elMedicoSoloOperaSusPropiosTurnos() {
        Medico otro = fixture.medicoConHorarioLunes(new BigDecimal("80.00"));
        UsuarioPrincipal otroPrincipal = fixture.principal(otro.getUsuario());
        TurnoResponse mio = turno(paciente, PrioridadTurno.NORMAL);
        TurnoResponse ajeno = recepcionService.crearTurno(
                new TurnoCreateRequest(fixture.paciente().getId(), otro.getId(), PrioridadTurno.NORMAL, null));

        assertThatThrownBy(() -> recepcionService.iniciar(mio.id(), otroPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> recepcionService.llamar(mio.id(), otroPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> recepcionService.cancelar(mio.id(), otroPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(recepcionService.cola(null, null, medicoPrincipal))
                .extracting(TurnoResponse::id)
                .containsExactly(mio.id());
        assertThat(recepcionService.cola(null, ajeno.medicoId(), medicoPrincipal))
                .extracting(TurnoResponse::id)
                .containsExactly(mio.id());
        assertThat(recepcionService.llamar(mio.id(), medicoPrincipal).estado()).isEqualTo(EstadoTurno.LLAMADO);
    }

    @Test
    void unTurnoSinMedicoNoLoPuedeIniciarNingunMedico() {
        TurnoResponse sinMedico = recepcionService.crearTurno(
                new TurnoCreateRequest(paciente.getId(), null, PrioridadTurno.PREFERENTE, "  Mareo  "));

        assertThat(sinMedico.medicoId()).isNull();
        assertThat(sinMedico.motivo()).isEqualTo("Mareo");
        assertThatThrownBy(() -> recepcionService.iniciar(sinMedico.id(), medicoPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(recepcionService.cola(null, null, recepcion)).hasSize(1);
    }

    @Test
    void laPantallaMuestraLosTurnosLlamadosOEnAtencionConSuConsultorio() {
        TurnoResponse uno = turno(paciente, PrioridadTurno.NORMAL);
        TurnoResponse dos = turno(fixture.paciente(), PrioridadTurno.NORMAL);
        turno(fixture.paciente(), PrioridadTurno.NORMAL);
        assertThat(recepcionService.pantalla()).isEmpty();

        recepcionService.llamar(uno.id(), recepcion);
        recepcionService.llamar(dos.id(), recepcion);
        recepcionService.iniciar(dos.id(), medicoPrincipal);

        List<PantallaTurnoResponse> pantalla = recepcionService.pantalla();
        assertThat(pantalla).hasSize(2);
        assertThat(pantalla).extracting(PantallaTurnoResponse::consultorio).containsOnly("Consultorio 1");
        assertThat(pantalla)
                .extracting(PantallaTurnoResponse::estado)
                .containsExactlyInAnyOrder(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);
    }

    @Test
    void laPantallaMuestraComoMaximoOchoTurnos() {
        for (int i = 0; i < 10; i++) {
            TurnoResponse t = turno(fixture.paciente(), PrioridadTurno.NORMAL);
            recepcionService.llamar(t.id(), recepcion);
        }
        assertThat(recepcionService.pantalla()).hasSize(8);
    }

    @Test
    void lasCitasDeHoyMarcanLaLlegadaYSeFiltranPorTexto() {
        Paciente otro = fixture.paciente();
        Cita conLlegada = citaHoy(medico, paciente, EstadoCita.CONFIRMADA);
        citaHoy(medico, otro, EstadoCita.PENDIENTE);
        citaHoy(medico, fixture.paciente(), EstadoCita.CANCELADA);
        fixture.cita(medico, fixture.paciente(), LocalDateTime.now().plusDays(2), EstadoCita.CONFIRMADA);
        recepcionService.registrarLlegada(conLlegada.getId());

        List<CitaHoyResponse> todas = recepcionService.citasHoy(null);
        assertThat(todas).hasSize(2);
        assertThat(todas)
                .filteredOn(c -> c.citaId().equals(conLlegada.getId()))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.llego()).isTrue();
                    assertThat(c.turnoNumero()).isEqualTo("A-001");
                });
        assertThat(todas).filteredOn(c -> !c.llego()).hasSize(1);

        assertThat(recepcionService.citasHoy(otro.getDni())).hasSize(1);
        assertThat(recepcionService.citasHoy("zzzz-no-existe")).isEmpty();
        assertThat(recepcionService.resumen().citasPendientesDeLlegada()).isEqualTo(1);
    }

    @Test
    void elResumenCalculaLaEsperaPromedio() {
        TurnoResponse espera = turno(paciente, PrioridadTurno.NORMAL);
        RecepcionResumenResponse antes = recepcionService.resumen();
        assertThat(antes.enEspera()).isEqualTo(1);
        assertThat(antes.esperaPromedioMinutos()).isZero();

        recepcionService.llamar(espera.id(), recepcion);
        RecepcionResumenResponse despues = recepcionService.resumen();
        assertThat(despues.llamados()).isEqualTo(1);
        assertThat(despues.enEspera()).isZero();
        assertThat(despues.esperaPromedioMinutos()).isGreaterThanOrEqualTo(0);
    }
}
