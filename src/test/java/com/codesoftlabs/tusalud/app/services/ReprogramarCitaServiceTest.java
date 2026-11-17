package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.ReprogramarRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
class ReprogramarCitaServiceTest {

    @Autowired
    private CitaService citaService;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private NotificacionRepository notificacionRepository;

    private Medico medico() {
        return fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
    }

    private LocalDate lunes(int semanas) {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(semanas);
    }

    @Test
    void elPacienteMueveSuCitaYVuelveAPendiente() {
        Medico medico = medico();
        Paciente paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, lunes(0).atTime(9, 0), EstadoCita.CONFIRMADA);
        LocalDateTime nueva = lunes(1).atTime(10, 30);

        CitaResponse respuesta = citaService.reprogramarCita(
                cita.getId(), new ReprogramarRequest(nueva), fixture.principal(paciente.getUsuario()));

        assertThat(respuesta.fechaHora()).isEqualTo(nueva);
        assertThat(respuesta.estado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(notificacionRepository.findByUsuarioIdOrderByCreatedAtDescIdDesc(
                        medico.getUsuario().getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .anyMatch(n ->
                        n.getTipo() == com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion.CITA_REPROGRAMADA);
    }

    @Test
    void permiteReprogramarAlMismoDiaSiLaFranjaLibreEsLaPropia() {
        Medico medico = medico();
        Paciente paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, lunes(0).atTime(9, 0), EstadoCita.PENDIENTE);

        CitaResponse respuesta = citaService.reprogramarCita(
                cita.getId(), new ReprogramarRequest(lunes(0).atTime(9, 30)), fixture.principal(paciente.getUsuario()));

        assertThat(respuesta.fechaHora()).isEqualTo(lunes(0).atTime(9, 30));
    }

    @Test
    void rechazaUnHorarioOcupadoPorOtraCitaDelMedico() {
        Medico medico = medico();
        Paciente paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, lunes(0).atTime(9, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, fixture.paciente(), lunes(1).atTime(10, 0), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> citaService.reprogramarCita(
                        cita.getId(),
                        new ReprogramarRequest(lunes(1).atTime(10, 0)),
                        fixture.principal(paciente.getUsuario())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rechazaUnaHoraFueraDelHorarioDelMedico() {
        Medico medico = medico();
        Paciente paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, lunes(0).atTime(9, 0), EstadoCita.PENDIENTE);

        assertThatThrownBy(() -> citaService.reprogramarCita(
                        cita.getId(),
                        new ReprogramarRequest(lunes(1).atTime(18, 0)),
                        fixture.principal(paciente.getUsuario())))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void rechazaCitasCanceladasYUsuariosAjenos() {
        Medico medico = medico();
        Paciente paciente = fixture.paciente();
        Cita cancelada = fixture.cita(medico, paciente, lunes(0).atTime(9, 0), EstadoCita.CANCELADA);
        Cita activa = fixture.cita(medico, paciente, lunes(0).atTime(10, 0), EstadoCita.PENDIENTE);

        assertThatThrownBy(() -> citaService.reprogramarCita(
                        cancelada.getId(),
                        new ReprogramarRequest(lunes(1).atTime(9, 0)),
                        fixture.principal(paciente.getUsuario())))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> citaService.reprogramarCita(
                        activa.getId(),
                        new ReprogramarRequest(lunes(1).atTime(9, 0)),
                        fixture.principal(fixture.paciente().getUsuario())))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
