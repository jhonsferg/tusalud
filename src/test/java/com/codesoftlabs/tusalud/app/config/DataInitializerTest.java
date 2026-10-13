package com.codesoftlabs.tusalud.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.repositories.HorarioAtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private HorarioAtencionRepository horarioRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void creaRolesYEspecialidades() {
        for (RolNombre nombre : RolNombre.values()) {
            assertThat(rolRepository.existsByNombre(nombre)).as(nombre.name()).isTrue();
        }
        for (String nombre : List.of("Cardiología", "Pediatría", "Medicina General", "Dermatología")) {
            assertThat(especialidadRepository.existsByNombreIgnoreCase(nombre))
                    .as(nombre)
                    .isTrue();
        }
    }

    @Test
    void creaUsuariosConPasswordBCryptYRol() {
        assertUsuario("admin@tusalud.com", "Admin123!", RolNombre.ROLE_ADMIN);
        assertUsuario("dr.garcia@tusalud.com", "Medico123!", RolNombre.ROLE_MEDICO);
        assertUsuario("dra.lopez@tusalud.com", "Medico123!", RolNombre.ROLE_MEDICO);
        assertUsuario("juan.perez@correo.com", "Paciente123!", RolNombre.ROLE_PACIENTE);
        assertUsuario("maria.rodriguez@correo.com", "Paciente123!", RolNombre.ROLE_PACIENTE);
    }

    @Test
    void creaMedicosConHorarios() {
        Medico garcia =
                medicoRepository.findByUsuarioEmail("dr.garcia@tusalud.com").orElseThrow();
        assertThat(garcia.getColegiatura()).isEqualTo("CMP-12345");
        assertThat(garcia.getConsultorio()).isEqualTo("Consultorio 101");
        assertThat(garcia.getPrecioConsulta()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(garcia.getEspecialidad().getNombre()).isEqualTo("Cardiología");
        List<HorarioAtencion> horariosGarcia = horarioRepository.findByMedicoId(garcia.getId());
        assertThat(horariosGarcia).hasSize(5).allSatisfy(h -> {
            assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
            assertThat(h.getHoraFin()).isEqualTo(LocalTime.of(13, 0));
            assertThat(h.getDuracionCitaMinutos()).isEqualTo(30);
        });

        Medico lopez =
                medicoRepository.findByUsuarioEmail("dra.lopez@tusalud.com").orElseThrow();
        assertThat(lopez.getColegiatura()).isEqualTo("CMP-67890");
        assertThat(lopez.getEspecialidad().getNombre()).isEqualTo("Pediatría");
        List<HorarioAtencion> horariosLopez = horarioRepository.findByMedicoId(lopez.getId());
        assertThat(horariosLopez).hasSize(6);
        assertThat(horarioRepository.findByMedicoAndDia(lopez.getId(), DayOfWeek.SATURDAY))
                .singleElement()
                .satisfies(h -> {
                    assertThat(h.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
                    assertThat(h.getHoraFin()).isEqualTo(LocalTime.of(12, 0));
                });
    }

    @Test
    void creaPacientesYCitasDeEjemplo() {
        Paciente juan =
                pacienteRepository.findByUsuarioEmail("juan.perez@correo.com").orElseThrow();
        assertThat(juan.getDni()).isEqualTo("12345678");
        assertThat(juan.getHistorialClinicoNumero()).isEqualTo("HC-000001");
        assertThat(juan.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 15));
        Paciente maria = pacienteRepository
                .findByUsuarioEmail("maria.rodriguez@correo.com")
                .orElseThrow();
        assertThat(maria.getHistorialClinicoNumero()).isEqualTo("HC-000002");

        LocalDate dia = DataInitializer.proximoDiaHabil(LocalDate.now());
        List<Cita> citasJuan =
                citaRepository.findByPacienteAndRango(juan.getId(), dia.atStartOfDay(), dia.atTime(23, 59));
        assertThat(citasJuan).singleElement().satisfies(c -> {
            assertThat(c.getFechaHora()).isEqualTo(dia.atTime(9, 0));
            assertThat(c.getEstado()).isEqualTo(EstadoCita.PENDIENTE);
            assertThat(c.getMonto()).isEqualByComparingTo("150.00");
        });
        List<Cita> citasMaria =
                citaRepository.findByPacienteAndRango(maria.getId(), dia.atStartOfDay(), dia.atTime(23, 59));
        assertThat(citasMaria).singleElement().satisfies(c -> {
            assertThat(c.getFechaHora()).isEqualTo(dia.atTime(15, 0));
            assertThat(c.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        });
    }

    @Test
    void proximoDiaHabilSaltaFinDeSemana() {
        assertThat(DataInitializer.proximoDiaHabil(LocalDate.of(2026, 10, 9))).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(DataInitializer.proximoDiaHabil(LocalDate.of(2026, 10, 10))).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(DataInitializer.proximoDiaHabil(LocalDate.of(2026, 10, 12))).isEqualTo(LocalDate.of(2026, 10, 13));
    }

    @Test
    void segundaEjecucionNoDuplicaDatos() throws Exception {
        long roles = rolRepository.count();
        long especialidades = especialidadRepository.count();
        long usuarios = usuarioRepository.count();
        long medicos = medicoRepository.count();
        long pacientes = pacienteRepository.count();
        long horarios = horarioRepository.count();
        long citas = citaRepository.count();

        dataInitializer.run();

        assertThat(rolRepository.count()).isEqualTo(roles);
        assertThat(especialidadRepository.count()).isEqualTo(especialidades);
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
        assertThat(medicoRepository.count()).isEqualTo(medicos);
        assertThat(pacienteRepository.count()).isEqualTo(pacientes);
        assertThat(horarioRepository.count()).isEqualTo(horarios);
        assertThat(citaRepository.count()).isEqualTo(citas);
    }

    private void assertUsuario(String email, String password, RolNombre rol) {
        Usuario usuario = usuarioRepository.findByEmailWithRoles(email).orElseThrow();
        assertThat(usuario.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches(password, usuario.getPassword()))
                .as(email)
                .isTrue();
        assertThat(usuario.getRoles()).extracting(Rol::getNombre).containsExactly(rol);
    }
}
