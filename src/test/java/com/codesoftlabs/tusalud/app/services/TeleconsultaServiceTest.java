package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse;
import com.codesoftlabs.tusalud.app.dto.response.TeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TicketTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Teleconsulta;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.TeleconsultaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.impl.TeleconsultaServiceImpl;
import com.codesoftlabs.tusalud.app.services.impl.TicketTeleconsultaService;
import com.codesoftlabs.tusalud.app.services.impl.TurnCredencialesService;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
class TeleconsultaServiceTest {

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private TeleconsultaService servicio;

    @Autowired
    private TeleconsultaRepository teleconsultaRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private TicketTeleconsultaService ticketService;

    @Autowired
    private TurnCredencialesService turnService;

    @Autowired
    private TelemedicinaProperties propiedades;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private Teleconsulta teleconsulta;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        teleconsulta = crear(LocalDateTime.now().plusMinutes(3));
    }

    private Teleconsulta crear(LocalDateTime fechaHora) {
        Cita cita = fixture.cita(medico, paciente, fechaHora, EstadoCita.CONFIRMADA);
        servicio.crearParaCita(cita);
        return teleconsultaRepository.findByCitaId(cita.getId()).orElseThrow();
    }

    @Test
    void laTeleconsultaNaceProgramadaConUnaSalaUnicaYCrearlaDosVecesNoDuplica() {
        assertThat(teleconsulta.getEstado()).isEqualTo(EstadoTeleconsulta.PROGRAMADA);
        assertThat(teleconsulta.getSala()).isNotNull();
        servicio.crearParaCita(teleconsulta.getCita());
        assertThat(teleconsultaRepository.findAll().stream()
                        .filter(t -> t.getCita()
                                .getId()
                                .equals(teleconsulta.getCita().getId()))
                        .count())
                .isEqualTo(1);
    }

    @Test
    void elPacienteDebeAceptarElConsentimientoAntesDeObtenerUnTicket() {
        assertThat(servicio.obtener(teleconsulta.getId(), principalPaciente).consentimientoPendiente())
                .isTrue();
        assertThatThrownBy(() -> servicio.emitirTicket(teleconsulta.getId(), principalPaciente))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("consentimiento");

        servicio.aceptarConsentimiento(principalPaciente, propiedades.versionConsentimiento());

        assertThat(servicio.obtener(teleconsulta.getId(), principalPaciente).consentimientoPendiente())
                .isFalse();
        TicketTeleconsultaResponse ticket = servicio.emitirTicket(teleconsulta.getId(), principalPaciente);
        assertThat(ticket.ticket()).hasSizeGreaterThan(30);
        assertThat(ticket.expiraEnSegundos()).isEqualTo(propiedades.ticketTtlSeg());
    }

    @Test
    void unaVersionDeConsentimientoQueNoEsLaVigenteSeRechaza() {
        assertThatThrownBy(() -> servicio.aceptarConsentimiento(principalPaciente, "0.1"))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void aceptarElConsentimientoDosVecesEsIdempotente() {
        servicio.aceptarConsentimiento(principalPaciente, propiedades.versionConsentimiento());
        assertThat(servicio.aceptarConsentimiento(principalPaciente, propiedades.versionConsentimiento())
                        .aceptado())
                .isTrue();
    }

    @Test
    void elTicketNoSeEmiteAntesDeLaVentanaDeIngreso() {
        Teleconsulta lejana = crear(LocalDateTime.now().plusDays(2));
        assertThat(servicio.obtener(lejana.getId(), principalMedico).puedeIngresar())
                .isFalse();
        assertThatThrownBy(() -> servicio.emitirTicket(lejana.getId(), principalMedico))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("minutos antes");
        assertThatThrownBy(() -> servicio.abrir(lejana.getId(), principalMedico))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void elTicketNoSeEmiteDespuesDeLaVentanaDeIngreso() {
        Teleconsulta pasada = crear(LocalDateTime.now().minusMinutes(propiedades.ventanaDespuesMin() + 5));
        assertThat(servicio.obtener(pasada.getId(), principalMedico).puedeIngresar())
                .isFalse();
        assertThatThrownBy(() -> servicio.emitirTicket(pasada.getId(), principalMedico))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("terminó");
    }

    @Test
    void laVentanaSeAbreDiezMinutosAntesYSeCierraTreintaDespues() {
        TeleconsultaResponse respuesta = servicio.obtener(teleconsulta.getId(), principalMedico);
        assertThat(respuesta.ventanaDesde())
                .isEqualTo(teleconsulta.getCita().getFechaHora().minusMinutes(10));
        assertThat(respuesta.ventanaHasta())
                .isEqualTo(teleconsulta.getCita().getFechaHora().plusMinutes(30));
        assertThat(respuesta.puedeIngresar()).isTrue();
    }

    @Test
    void elTicketEsDeUnSoloUsoYExpira() {
        servicio.aceptarConsentimiento(principalPaciente, propiedades.versionConsentimiento());
        String ticket =
                servicio.emitirTicket(teleconsulta.getId(), principalPaciente).ticket();

        var primero = ticketService.consumir(ticket);
        assertThat(primero).isPresent();
        assertThat(primero.get().usuarioId()).isEqualTo(principalPaciente.getId());
        assertThat(primero.get().teleconsultaId()).isEqualTo(teleconsulta.getId());
        assertThat(ticketService.consumir(ticket)).isEmpty();
        assertThat(ticketService.consumir("no-existe")).isEmpty();
        assertThat(ticketService.consumir(null)).isEmpty();
        assertThat(primero.get().expira()).isBefore(Instant.now().plusSeconds(propiedades.ticketTtlSeg() + 2));
    }

    @Test
    void soloElMedicoYElPacienteDeLaCitaParticipan() {
        UsuarioPrincipal otroMedico = fixture.principal(fixture.usuario("otro-med", RolNombre.ROLE_MEDICO));
        UsuarioPrincipal otroPaciente = fixture.principal(fixture.paciente().getUsuario());
        UsuarioPrincipal admin = fixture.principal(fixture.usuario("adm", RolNombre.ROLE_ADMIN));
        for (UsuarioPrincipal ajeno : new UsuarioPrincipal[] {otroMedico, otroPaciente, admin}) {
            assertThatThrownBy(() -> servicio.obtener(teleconsulta.getId(), ajeno))
                    .isInstanceOf(ForbiddenOperationException.class);
            assertThatThrownBy(() -> servicio.emitirTicket(teleconsulta.getId(), ajeno))
                    .isInstanceOf(ForbiddenOperationException.class);
            assertThatThrownBy(() -> servicio.ice(teleconsulta.getId(), ajeno))
                    .isInstanceOf(ForbiddenOperationException.class);
        }
    }

    @Test
    void soloElMedicoAbreYFinalizaLaSala() {
        assertThatThrownBy(() -> servicio.abrir(teleconsulta.getId(), principalPaciente))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> servicio.finalizar(teleconsulta.getId(), principalPaciente))
                .isInstanceOf(ForbiddenOperationException.class);

        TeleconsultaResponse abierta = servicio.abrir(teleconsulta.getId(), principalMedico);
        assertThat(abierta.estado()).isEqualTo(EstadoTeleconsulta.SALA_ABIERTA);
        assertThat(abierta.salaAbierta()).isTrue();

        TeleconsultaResponse finalizada = servicio.finalizar(teleconsulta.getId(), principalMedico);
        assertThat(finalizada.estado()).isEqualTo(EstadoTeleconsulta.FINALIZADA);
        assertThatThrownBy(() -> servicio.finalizar(teleconsulta.getId(), principalMedico))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void unaCitaCanceladaCancelaLaTeleconsulta() {
        Cita cita = teleconsulta.getCita();
        cita.setEstado(EstadoCita.CANCELADA);
        citaRepository.save(cita);
        TeleconsultaResponse respuesta = servicio.obtener(teleconsulta.getId(), principalPaciente);
        assertThat(respuesta.estado()).isEqualTo(EstadoTeleconsulta.CANCELADA);
        assertThat(respuesta.puedeIngresar()).isFalse();
    }

    @Test
    void lasCredencialesTurnSonTemporalesYSeFirmanConElSecreto() {
        IceServersResponse ice = servicio.ice(teleconsulta.getId(), principalPaciente);
        IceServersResponse.IceServer turn = ice.iceServers().stream()
                .filter(s -> s.username() != null)
                .findFirst()
                .orElseThrow();
        long expira = Long.parseLong(turn.username().split(":")[0]);
        assertThat(expira).isGreaterThan(Instant.now().getEpochSecond());
        assertThat(expira)
                .isLessThanOrEqualTo(
                        Instant.now().getEpochSecond() + propiedades.turn().ttlSeg() + 2);
        assertThat(turn.username()).endsWith(principalPaciente.getId().toString());
        assertThat(turn.credential())
                .isEqualTo(TurnCredencialesService.firmar(propiedades.turn().secreto(), turn.username()));
        assertThat(turn.urls()).allMatch(url -> url.startsWith("turn:"));
        assertThat(ice.iceServers().get(0).username()).isNull();
    }

    @Test
    void lasCredencialesDeDosUsuariosSonDistintas() {
        String a = turnService.generar(UUID.randomUUID()).iceServers().get(1).credential();
        String b = turnService.generar(UUID.randomUUID()).iceServers().get(1).credential();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void elResumenDelPacienteSoloLoVeElMedicoDeLaCita() {
        assertThat(servicio.resumenPaciente(teleconsulta.getId(), principalMedico)
                        .pacienteNombre())
                .isNotBlank();
        assertThatThrownBy(() -> servicio.resumenPaciente(teleconsulta.getId(), principalPaciente))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elRecordatorioAvisaUnaSolaVezAMedicoYPaciente() {
        TeleconsultaServiceImpl impl =
                (TeleconsultaServiceImpl) org.springframework.test.util.AopTestUtils.getTargetObject(servicio);
        impl.enviarRecordatorios();
        assertThat(teleconsultaRepository
                        .findById(teleconsulta.getId())
                        .orElseThrow()
                        .isRecordatorioEnviado())
                .isTrue();
    }

    @Test
    void unaTeleconsultaVencidaSinIniciarSeMarcaNoAsistio() {
        Teleconsulta pasada = crear(LocalDateTime.now().minusMinutes(propiedades.ventanaDespuesMin() + 10));
        TeleconsultaServiceImpl impl =
                (TeleconsultaServiceImpl) org.springframework.test.util.AopTestUtils.getTargetObject(servicio);
        impl.cerrarVencidas();
        assertThat(teleconsultaRepository.findById(pasada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoTeleconsulta.NO_ASISTIO);
    }
}
