package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecordatorioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class RecordatorioServiceImplTest {

    @Autowired
    private RecordatorioService recordatorioService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private PerfilService perfilService;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;

    private void prepararMedicoYPaciente() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
    }

    private List<NotificacionResponse> recordatoriosDe(Cita cita, Paciente destinatario) {
        return notificacionService.listar(fixture.principal(destinatario.getUsuario()), false, 100).stream()
                .filter(n -> n.tipo() == TipoNotificacion.RECORDATORIO_CITA)
                .filter(n -> ("/citas/" + cita.getId()).equals(n.enlace()))
                .toList();
    }

    @Test
    void citaDentroDeLaVentanaGeneraRecordatorioParaPacienteYMedico() {
        prepararMedicoYPaciente();
        LocalDateTime ahora = LocalDateTime.now();
        Cita cita = fixture.cita(medico, paciente, ahora.plusHours(5), EstadoCita.CONFIRMADA);

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(cita, paciente)).hasSize(1);
        assertThat(notificacionService.listar(fixture.principal(medico.getUsuario()), false, 100).stream()
                        .filter(n -> n.tipo() == TipoNotificacion.RECORDATORIO_CITA)
                        .count())
                .isEqualTo(1);
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().isRecordatorioEnviado())
                .isTrue();
    }

    @Test
    void noRepiteElRecordatorioYaEnviado() {
        prepararMedicoYPaciente();
        LocalDateTime ahora = LocalDateTime.now();
        Cita cita = fixture.cita(medico, paciente, ahora.plusHours(3), EstadoCita.PENDIENTE);

        recordatorioService.enviarRecordatorios(ahora);
        int segundaVuelta = recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(cita, paciente)).hasSize(1);
        assertThat(segundaVuelta).isZero();
    }

    @Test
    void citaFueraDeLaVentanaDeAntelacionNoSeNotifica() {
        prepararMedicoYPaciente();
        LocalDateTime ahora = LocalDateTime.now();
        Cita cita = fixture.cita(medico, paciente, ahora.plusHours(30), EstadoCita.PENDIENTE);

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(cita, paciente)).isEmpty();
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().isRecordatorioEnviado())
                .isFalse();
    }

    @Test
    void laAntelacionDelPacienteSeRespeta() {
        prepararMedicoYPaciente();
        perfilService.actualizarPreferencias(
                fixture.principal(paciente.getUsuario()), new PreferenciasRequest(true, 2, true, null));
        LocalDateTime ahora = LocalDateTime.now();
        Cita lejana = fixture.cita(medico, paciente, ahora.plusHours(5), EstadoCita.CONFIRMADA);
        Cita cercana = fixture.cita(medico, paciente, ahora.plusHours(1), EstadoCita.CONFIRMADA);

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(lejana, paciente)).isEmpty();
        assertThat(recordatoriosDe(cercana, paciente)).hasSize(1);
    }

    @Test
    void losRecordatoriosDesactivadosNoSeEnvian() {
        prepararMedicoYPaciente();
        perfilService.actualizarPreferencias(
                fixture.principal(paciente.getUsuario()), new PreferenciasRequest(false, 24, true, null));
        LocalDateTime ahora = LocalDateTime.now();
        Cita cita = fixture.cita(medico, paciente, ahora.plusHours(4), EstadoCita.PENDIENTE);

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(cita, paciente)).isEmpty();
    }

    @Test
    void citasCanceladasOAtendidasNoRecibenRecordatorio() {
        prepararMedicoYPaciente();
        LocalDateTime ahora = LocalDateTime.now();
        Cita cancelada = fixture.cita(medico, paciente, ahora.plusHours(2), EstadoCita.CANCELADA);

        recordatorioService.enviarRecordatorios(ahora);

        assertThat(recordatoriosDe(cancelada, paciente)).isEmpty();
    }
}
