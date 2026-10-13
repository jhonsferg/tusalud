package com.codesoftlabs.tusalud.app.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CitaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private HorarioAtencionRepository horarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Medico medico;
    private Paciente paciente;
    private final LocalDateTime lunes10 = LocalDate.of(2030, 1, 7).atTime(10, 0);

    @BeforeEach
    void setUp() {
        Especialidad cardio =
                em.persist(Especialidad.builder().nombre("Cardiología").build());

        medico = Medico.builder()
                .colegiatura("CMP-001")
                .consultorio("101")
                .precioConsulta(new BigDecimal("120.00"))
                .usuario(usuario("medico@test.com"))
                .especialidad(cardio)
                .build();
        medico.agregarHorario(HorarioAtencion.builder()
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionCitaMinutos(30)
                .build());
        em.persist(medico);

        paciente = em.persist(Paciente.builder()
                .dni("12345678")
                .historialClinicoNumero("HC-0001")
                .fechaNacimiento(LocalDate.of(1990, 5, 1))
                .usuario(usuario("paciente@test.com"))
                .build());

        em.persist(cita(lunes10, EstadoCita.PENDIENTE));
        em.persist(cita(lunes10.plusHours(1), EstadoCita.CANCELADA));
        em.flush();
        em.clear();
    }

    @Test
    void detectaSolapamientoIgnorandoCanceladas() {
        assertThat(citaRepository.existsSolapamiento(medico.getId(), lunes10)).isTrue();
        assertThat(citaRepository.existsSolapamiento(medico.getId(), lunes10.plusHours(1)))
                .isFalse();
        assertThat(citaRepository.existsCitaMedicoEnFranja(
                        medico.getId(), lunes10.minusMinutes(30), lunes10.plusMinutes(30)))
                .isTrue();
        assertThat(citaRepository.existsCitaPacienteEnFranja(
                        paciente.getId(), lunes10.plusMinutes(30), lunes10.plusMinutes(90)))
                .isFalse();
    }

    @Test
    void listaCitasPorPacienteYAgendaMedico() {
        List<Cita> citas = citaRepository.findByPacienteAndRango(
                paciente.getId(),
                lunes10.toLocalDate().atStartOfDay(),
                lunes10.toLocalDate().atTime(23, 59));
        assertThat(citas)
                .hasSize(2)
                .isSortedAccordingTo((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()));
        assertThat(citas.get(0).getMedico().getUsuario().getEmail()).isEqualTo("medico@test.com");

        assertThat(citaRepository.findAgendaMedicoPorFecha(medico.getId(), lunes10.toLocalDate()))
                .hasSize(2);
        assertThat(citaRepository.findHorasOcupadas(
                        medico.getId(),
                        lunes10.toLocalDate().atStartOfDay(),
                        lunes10.toLocalDate().plusDays(1).atStartOfDay()))
                .containsExactly(lunes10);
    }

    @Test
    void consultasDeMedicoHorarioYAuditoria() {
        Long especialidadId = medicoRepository
                .findById(medico.getId())
                .orElseThrow()
                .getEspecialidad()
                .getId();
        assertThat(medicoRepository.findByEspecialidadId(especialidadId)).hasSize(1);
        assertThat(medicoRepository.findByIdForUpdate(medico.getId())).isPresent();
        assertThat(usuarioRepository.findByEmailWithRoles("MEDICO@test.com")).isPresent();

        assertThat(horarioRepository.findByMedicoAndDia(medico.getId(), DayOfWeek.MONDAY))
                .hasSize(1);
        assertThat(horarioRepository.existsSolapamiento(
                        medico.getId(), DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0)))
                .isTrue();
        assertThat(horarioRepository.existsSolapamiento(
                        medico.getId(), DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0)))
                .isFalse();

        assertThat(citaRepository.contarPorEstado())
                .containsExactlyInAnyOrder(
                        new CitasPorEstadoResponse(EstadoCita.PENDIENTE, 1L),
                        new CitasPorEstadoResponse(EstadoCita.CANCELADA, 1L));
    }

    private Usuario usuario(String email) {
        return Usuario.builder()
                .nombre("Nombre")
                .apellido("Apellido")
                .email(email)
                .password("hash")
                .build();
    }

    private Cita cita(LocalDateTime fechaHora, EstadoCita estado) {
        return Cita.builder()
                .fechaHora(fechaHora)
                .motivo("Control")
                .estado(estado)
                .monto(medico.getPrecioConsulta())
                .medico(medico)
                .paciente(paciente)
                .build();
    }
}
