package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.ValoracionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionesMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.services.interfaces.ValoracionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class ValoracionServiceImplTest {

    @Autowired
    private ValoracionService valoracionService;

    @Autowired
    private ServiceTestFixture fixture;

    private Cita atendida(Medico medico, Paciente paciente) {
        return fixture.cita(medico, paciente, LocalDateTime.now().minusDays(3), EstadoCita.ATENDIDA);
    }

    @Test
    void elPacienteValoraUnaCitaAtendida() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        Paciente paciente = fixture.paciente();
        Cita cita = atendida(medico, paciente);

        ValoracionResponse respuesta = valoracionService.valorar(
                cita.getId(), new ValoracionRequest(5, "  Excelente  "), fixture.principal(paciente.getUsuario()));

        assertThat(respuesta.puntuacion()).isEqualTo(5);
        assertThat(respuesta.comentario()).isEqualTo("Excelente");
        assertThat(valoracionService
                        .obtenerDeCita(cita.getId(), fixture.principal(paciente.getUsuario()))
                        .id())
                .isEqualTo(respuesta.id());
    }

    @Test
    void noSePuedeValorarDosVecesNiUnaCitaNoAtendidaNiAjena() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        Paciente paciente = fixture.paciente();
        Cita cita = atendida(medico, paciente);
        Cita pendiente = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.PENDIENTE);
        valoracionService.valorar(
                cita.getId(), new ValoracionRequest(4, null), fixture.principal(paciente.getUsuario()));

        assertThatThrownBy(() -> valoracionService.valorar(
                        cita.getId(), new ValoracionRequest(3, null), fixture.principal(paciente.getUsuario())))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> valoracionService.valorar(
                        pendiente.getId(), new ValoracionRequest(3, null), fixture.principal(paciente.getUsuario())))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> valoracionService.valorar(
                        cita.getId(),
                        new ValoracionRequest(3, null),
                        fixture.principal(fixture.paciente().getUsuario())))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void obtenerSinValoracionLanzaNoEncontrado() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        Paciente paciente = fixture.paciente();
        Cita cita = atendida(medico, paciente);

        assertThatThrownBy(
                        () -> valoracionService.obtenerDeCita(cita.getId(), fixture.principal(paciente.getUsuario())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void elPromedioDelMedicoSeRedondeaAUnDecimal() {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        int[] notas = {5, 4, 4};
        for (int nota : notas) {
            Paciente paciente = fixture.paciente();
            Cita cita = atendida(medico, paciente);
            valoracionService.valorar(
                    cita.getId(), new ValoracionRequest(nota, null), fixture.principal(paciente.getUsuario()));
        }

        ValoracionesMedicoResponse resumen = valoracionService.listarDeMedico(medico.getId());

        assertThat(resumen.total()).isEqualTo(3);
        assertThat(resumen.promedio()).isEqualTo(4.3);
        assertThat(valoracionService
                        .listarDeMedico(
                                fixture.medicoConHorarioLunes(BigDecimal.ONE).getId())
                        .promedio())
                .isZero();
    }
}
