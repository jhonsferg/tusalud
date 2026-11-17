package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ReprogramarRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadRangoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
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
class CitaCalendarioServiceTest {

    private static final BigDecimal PRECIO = new BigDecimal("100.00");

    @Autowired
    private CitaService citaService;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Medico medicoDeUnaHora;
    private Paciente paciente;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal medicoPrincipal;
    private LocalDate lunes;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(PRECIO);
        medicoDeUnaHora = fixture.medicoConHorarioLunes(PRECIO, 60);
        paciente = fixture.paciente();
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        medicoPrincipal = fixture.principal(medico.getUsuario());
        lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private CitaCreateRequest solicitud(LocalDateTime fechaHora) {
        return new CitaCreateRequest(
                medico.getId(),
                fechaHora,
                "Control",
                medico.getEspecialidades().iterator().next().getId());
    }

    @Test
    void rechazaUnaCitaQueCoincideExactamenteConOtraDelPacienteConOtroMedico() {
        fixture.cita(medicoDeUnaHora, paciente, lunes.atTime(10, 0), EstadoCita.PENDIENTE);

        assertThatThrownBy(() -> citaService.crearCita(solicitud(lunes.atTime(10, 0)), pacientePrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya tienes otra cita activa que se cruza con ese horario");
    }

    @Test
    void rechazaUnSolapamientoParcialUsandoLaDuracionRealDeLaOtraCita() {
        fixture.cita(medicoDeUnaHora, paciente, lunes.atTime(10, 0), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> citaService.crearCita(solicitud(lunes.atTime(10, 30)), pacientePrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rechazaSiLaNuevaCitaEmpiezaAntesPeroTerminaDentroDeOtra() {
        Medico otroMedico = fixture.medicoConHorarioLunes(PRECIO, 60);
        CitaCreateRequest solicitud = new CitaCreateRequest(
                otroMedico.getId(),
                lunes.atTime(9, 0),
                "Control",
                otroMedico.getEspecialidades().iterator().next().getId());
        fixture.cita(medico, paciente, lunes.atTime(9, 30), EstadoCita.PENDIENTE);

        assertThatThrownBy(() -> citaService.crearCita(solicitud, pacientePrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void permiteCitasAdyacentesQueSoloSeTocanEnElBorde() {
        fixture.cita(medicoDeUnaHora, paciente, lunes.atTime(10, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, paciente, lunes.atTime(9, 0), EstadoCita.PENDIENTE);

        assertThatCode(() -> citaService.crearCita(solicitud(lunes.atTime(11, 0)), pacientePrincipal))
                .doesNotThrowAnyException();
        assertThatCode(() -> citaService.crearCita(solicitud(lunes.atTime(9, 30)), pacientePrincipal))
                .doesNotThrowAnyException();
    }

    @Test
    void ignoraLasCitasCanceladasYLasYaAtendidasDelPaciente() {
        fixture.cita(medicoDeUnaHora, paciente, lunes.atTime(10, 0), EstadoCita.CANCELADA);
        fixture.cita(medicoDeUnaHora, paciente, lunes.atTime(8, 0), EstadoCita.ATENDIDA);

        assertThatCode(() -> citaService.crearCita(solicitud(lunes.atTime(10, 0)), pacientePrincipal))
                .doesNotThrowAnyException();
        assertThatCode(() -> citaService.crearCita(solicitud(lunes.atTime(8, 0)), pacientePrincipal))
                .doesNotThrowAnyException();
    }

    @Test
    void noCuentaLaPropiaCitaAlReprogramarPeroSiLasDemas() {
        Cita propia = fixture.cita(medico, paciente, lunes.atTime(9, 0), EstadoCita.PENDIENTE);
        fixture.cita(medicoDeUnaHora, paciente, lunes.plusWeeks(1).atTime(10, 0), EstadoCita.CONFIRMADA);

        assertThatCode(() -> citaService.reprogramarCita(
                        propia.getId(), new ReprogramarRequest(lunes.atTime(9, 30)), pacientePrincipal))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> citaService.reprogramarCita(
                        propia.getId(),
                        new ReprogramarRequest(lunes.plusWeeks(1).atTime(10, 30)),
                        pacientePrincipal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laRespuestaIncluyeLaDuracionDelBloqueYConflictoFalso() {
        CitaResponse treinta = citaService.crearCita(solicitud(lunes.atTime(8, 0)), pacientePrincipal);
        Cita deUnaHora = fixture.cita(medicoDeUnaHora, fixture.paciente(), lunes.atTime(9, 0), EstadoCita.PENDIENTE);

        assertThat(treinta.duracionMinutos()).isEqualTo(30);
        assertThat(treinta.conflicto()).isFalse();
        assertThat(citaService
                        .obtenerCita(deUnaHora.getId(), fixture.principal(medicoDeUnaHora.getUsuario()))
                        .duracionMinutos())
                .isEqualTo(60);
    }

    @Test
    void disponibilidadPorRangoDevuelveSoloLosDiasConAtencionConSusHorasLibres() {
        fixture.cita(medico, fixture.paciente(), lunes.atTime(9, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, fixture.paciente(), lunes.atTime(10, 0), EstadoCita.CANCELADA);

        List<DisponibilidadRangoResponse> dias =
                citaService.consultarDisponibilidadRango(medico.getId(), lunes, lunes.plusDays(13));

        assertThat(dias).extracting(DisponibilidadRangoResponse::fecha).containsExactly(lunes, lunes.plusWeeks(1));
        DisponibilidadRangoResponse primero = dias.get(0);
        assertThat(primero.totalHoras())
                .isEqualTo(7)
                .isEqualTo(primero.horariosDisponibles().size());
        assertThat(primero.horariosDisponibles())
                .doesNotContain(LocalTime.of(9, 0))
                .contains(LocalTime.of(8, 0), LocalTime.of(10, 0));
        assertThat(dias.get(1).totalHoras()).isEqualTo(8);
    }

    @Test
    void disponibilidadPorRangoOmiteLosDiasPasadosYValidaElRango() {
        assertThat(citaService.consultarDisponibilidadRango(
                        medico.getId(),
                        LocalDate.now().minusDays(10),
                        LocalDate.now().minusDays(1)))
                .isEmpty();
        assertThatThrownBy(() -> citaService.consultarDisponibilidadRango(medico.getId(), lunes, lunes.minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> citaService.consultarDisponibilidadRango(medico.getId(), lunes, lunes.plusDays(62)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatCode(() -> citaService.consultarDisponibilidadRango(medico.getId(), lunes, lunes.plusDays(61)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> citaService.consultarDisponibilidadRango(999_999L, lunes, lunes))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agendaDelMedicoPorRangoIncluyeElUltimoDiaYExigeAmbosExtremos() {
        fixture.cita(medico, fixture.paciente(), lunes.atTime(8, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, fixture.paciente(), lunes.plusDays(7).atTime(11, 30), EstadoCita.CONFIRMADA);
        fixture.cita(medico, fixture.paciente(), lunes.plusDays(8).atTime(8, 0), EstadoCita.CONFIRMADA);

        List<CitaResponse> rango = citaService.agendaMedico(medicoPrincipal, null, lunes, lunes.plusDays(7));

        assertThat(rango).hasSize(2);
        assertThat(citaService.agendaMedico(medicoPrincipal, lunes, null, null)).hasSize(1);
        assertThatThrownBy(() -> citaService.agendaMedico(medicoPrincipal, null, lunes, null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> citaService.agendaMedico(medicoPrincipal, null, lunes, lunes.plusDays(70)))
                .isInstanceOf(DatosInvalidosException.class);
    }
}
