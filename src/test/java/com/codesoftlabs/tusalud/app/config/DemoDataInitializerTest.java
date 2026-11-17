package com.codesoftlabs.tusalud.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
        properties = {
            "app.demo-data.enabled=true",
            "spring.datasource.url=jdbc:h2:mem:tusalud_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
        })
@ActiveProfiles("test")
@Transactional
class DemoDataInitializerTest {

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Test
    void cargaMedicosPacientesYCitasDeDemostracion() {
        assertThat(medicoRepository.count()).isEqualTo(7);
        assertThat(pacienteRepository.count()).isEqualTo(10);
        assertThat(usuarioRepository.existsByEmailIgnoreCase("dr.delgado@tusalud.com"))
                .isTrue();

        List<Cita> citas = citaRepository.findAll();
        assertThat(citas).extracting(Cita::getEstado).contains(EstadoCita.values());
        assertThat(citas)
                .filteredOn(c -> c.getEstado() == EstadoCita.ATENDIDA)
                .allSatisfy(c -> assertThat(c.getFechaHora()).isBefore(LocalDateTime.now()));
        assertThat(citas)
                .filteredOn(c -> c.getEstado() == EstadoCita.PENDIENTE || c.getEstado() == EstadoCita.CONFIRMADA)
                .allSatisfy(c -> assertThat(c.getFechaHora()).isAfter(LocalDateTime.now()));
    }

    @Test
    void lasCitasCaenEnDiasYHorasDeAtencionDelMedico() {
        citaRepository.findAll().forEach(cita -> {
            DayOfWeek dia = cita.getFechaHora().getDayOfWeek();
            boolean atiende = cita.getMedico().getHorarios().stream()
                    .anyMatch(h -> h.getDiaSemana() == dia
                            && !cita.getFechaHora().toLocalTime().isBefore(h.getHoraInicio())
                            && cita.getFechaHora().toLocalTime().isBefore(h.getHoraFin()));
            assertThat(atiende).as("cita %s", cita.getMotivo()).isTrue();
        });
    }

    @Test
    void esIdempotente() {
        long citas = citaRepository.count();

        demoDataInitializer.run();

        assertThat(citaRepository.count()).isEqualTo(citas);
        assertThat(medicoRepository.count()).isEqualTo(7);
    }
}
