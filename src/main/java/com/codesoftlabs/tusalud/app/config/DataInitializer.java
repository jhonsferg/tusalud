package com.codesoftlabs.tusalud.app.config;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EspecialidadRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    static final String ADMIN_EMAIL = "admin@tusalud.com";
    static final String ADMIN_PASSWORD = "Admin123!";
    static final String MEDICO1_EMAIL = "dr.garcia@tusalud.com";
    static final String MEDICO2_EMAIL = "dra.lopez@tusalud.com";
    static final String MEDICO_PASSWORD = "Medico123!";
    static final String PACIENTE1_EMAIL = "juan.perez@correo.com";
    static final String PACIENTE2_EMAIL = "maria.rodriguez@correo.com";
    static final String PACIENTE_PASSWORD = "Paciente123!";

    private static final Set<DayOfWeek> LUNES_A_VIERNES = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private final RolRepository rolRepository;
    private final EspecialidadRepository especialidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando datos de prueba...");
        List<String> creados = new ArrayList<>();

        Rol rolAdmin = obtenerOCrearRol(RolNombre.ROLE_ADMIN, creados);
        Rol rolMedico = obtenerOCrearRol(RolNombre.ROLE_MEDICO, creados);
        Rol rolPaciente = obtenerOCrearRol(RolNombre.ROLE_PACIENTE, creados);

        Especialidad cardiologia = obtenerOCrearEspecialidad(
                "Cardiología",
                "Diagnóstico y tratamiento de enfermedades del corazón y del sistema circulatorio",
                creados);
        Especialidad pediatria =
                obtenerOCrearEspecialidad("Pediatría", "Atención médica de bebés, niños y adolescentes", creados);
        obtenerOCrearEspecialidad("Medicina General", "Atención primaria, prevención y diagnóstico general", creados);
        obtenerOCrearEspecialidad("Dermatología", "Diagnóstico y tratamiento de enfermedades de la piel", creados);

        if (!usuarioRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            usuarioRepository.save(
                    nuevoUsuario("Administrador", "General", ADMIN_EMAIL, ADMIN_PASSWORD, "999888777", rolAdmin));
            creados.add("Usuario administrador " + ADMIN_EMAIL);
        }

        Medico drGarcia = null;
        if (!usuarioRepository.existsByEmailIgnoreCase(MEDICO1_EMAIL)
                && !medicoRepository.existsByColegiaturaIgnoreCase("CMP-12345")) {
            drGarcia = Medico.builder()
                    .colegiatura("CMP-12345")
                    .consultorio("Consultorio 101")
                    .precioConsulta(new BigDecimal("150.00"))
                    .especialidad(cardiologia)
                    .usuario(nuevoUsuario("Luis", "García", MEDICO1_EMAIL, MEDICO_PASSWORD, "912345678", rolMedico))
                    .build();
            for (DayOfWeek dia : LUNES_A_VIERNES) {
                drGarcia.agregarHorario(horario(dia, LocalTime.of(8, 0), LocalTime.of(13, 0)));
            }
            drGarcia = medicoRepository.save(drGarcia);
            creados.add("Médico " + MEDICO1_EMAIL + " con 5 horarios de atención");
        }

        Medico draLopez = null;
        if (!usuarioRepository.existsByEmailIgnoreCase(MEDICO2_EMAIL)
                && !medicoRepository.existsByColegiaturaIgnoreCase("CMP-67890")) {
            draLopez = Medico.builder()
                    .colegiatura("CMP-67890")
                    .consultorio("Consultorio 205")
                    .precioConsulta(new BigDecimal("120.00"))
                    .especialidad(pediatria)
                    .usuario(nuevoUsuario("Carmen", "López", MEDICO2_EMAIL, MEDICO_PASSWORD, "923456789", rolMedico))
                    .build();
            for (DayOfWeek dia : LUNES_A_VIERNES) {
                draLopez.agregarHorario(horario(dia, LocalTime.of(14, 0), LocalTime.of(19, 0)));
            }
            draLopez.agregarHorario(horario(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
            draLopez = medicoRepository.save(draLopez);
            creados.add("Médico " + MEDICO2_EMAIL + " con 6 horarios de atención");
        }

        Paciente juan = obtenerOCrearPaciente(
                "Juan",
                "Pérez",
                PACIENTE1_EMAIL,
                "987654321",
                "12345678",
                "HC-000001",
                LocalDate.of(1990, 5, 15),
                rolPaciente,
                creados);
        Paciente maria = obtenerOCrearPaciente(
                "María",
                "Rodríguez",
                PACIENTE2_EMAIL,
                "976543210",
                "87654321",
                "HC-000002",
                LocalDate.of(1985, 11, 20),
                rolPaciente,
                creados);

        LocalDate proximoDiaHabil = proximoDiaHabil(LocalDate.now());
        if (drGarcia != null && juan != null) {
            citaRepository.save(Cita.builder()
                    .medico(drGarcia)
                    .paciente(juan)
                    .fechaHora(proximoDiaHabil.atTime(9, 0))
                    .motivo("Control de presión arterial")
                    .estado(EstadoCita.PENDIENTE)
                    .monto(new BigDecimal("150.00"))
                    .build());
            creados.add("Cita de ejemplo " + proximoDiaHabil + " 09:00 (Juan Pérez - Dr. García, PENDIENTE)");
        }
        if (draLopez != null && maria != null) {
            citaRepository.save(Cita.builder()
                    .medico(draLopez)
                    .paciente(maria)
                    .fechaHora(proximoDiaHabil.atTime(15, 0))
                    .motivo("Control pediátrico de su hijo")
                    .estado(EstadoCita.CONFIRMADA)
                    .monto(new BigDecimal("120.00"))
                    .build());
            creados.add("Cita de ejemplo " + proximoDiaHabil + " 15:00 (María Rodríguez - Dra. López, CONFIRMADA)");
        }

        if (creados.isEmpty()) {
            log.info("Datos de prueba ya existentes: no se creó ningún registro nuevo");
            return;
        }
        creados.forEach(item -> log.info("  [creado] {}", item));
        log.info(
                """

                ==================== CREDENCIALES DE PRUEBA ====================
                 Rol        | Email                          | Contraseña
                ------------+--------------------------------+---------------
                 ADMIN      | {} | {}
                 MEDICO     | {} | {}
                 MEDICO     | {} | {}
                 PACIENTE   | {} | {}
                 PACIENTE   | {} | {}
                ================================================================""",
                pad(ADMIN_EMAIL),
                ADMIN_PASSWORD,
                pad(MEDICO1_EMAIL),
                MEDICO_PASSWORD,
                pad(MEDICO2_EMAIL),
                MEDICO_PASSWORD,
                pad(PACIENTE1_EMAIL),
                PACIENTE_PASSWORD,
                pad(PACIENTE2_EMAIL),
                PACIENTE_PASSWORD);
        log.info("Datos de prueba inicializados correctamente ({} registros nuevos)", creados.size());
    }

    private Rol obtenerOCrearRol(RolNombre nombre, List<String> creados) {
        return rolRepository.findByNombre(nombre).orElseGet(() -> {
            creados.add("Rol " + nombre);
            return rolRepository.save(Rol.builder().nombre(nombre).build());
        });
    }

    private Especialidad obtenerOCrearEspecialidad(String nombre, String descripcion, List<String> creados) {
        return especialidadRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> {
                    creados.add("Especialidad " + nombre);
                    return especialidadRepository.save(Especialidad.builder()
                            .nombre(nombre)
                            .descripcion(descripcion)
                            .build());
                });
    }

    private Paciente obtenerOCrearPaciente(
            String nombre,
            String apellido,
            String email,
            String telefono,
            String dni,
            String historial,
            LocalDate nacimiento,
            Rol rolPaciente,
            List<String> creados) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return pacienteRepository.findByUsuarioEmail(email).orElse(null);
        }
        if (pacienteRepository.existsByDni(dni)) {
            log.warn("No se creó el paciente {}: el DNI {} ya está registrado", email, dni);
            return null;
        }
        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .dni(dni)
                .historialClinicoNumero(historial)
                .fechaNacimiento(nacimiento)
                .usuario(nuevoUsuario(nombre, apellido, email, PACIENTE_PASSWORD, telefono, rolPaciente))
                .build());
        creados.add("Paciente " + email + " (" + historial + ")");
        return paciente;
    }

    private Usuario nuevoUsuario(
            String nombre, String apellido, String email, String password, String telefono, Rol rol) {
        Set<Rol> roles = new HashSet<>();
        roles.add(rol);
        return Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .email(email)
                .password(passwordEncoder.encode(password))
                .telefono(telefono)
                .estado(EstadoUsuario.ACTIVO)
                .roles(roles)
                .build();
    }

    private static HorarioAtencion horario(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        return HorarioAtencion.builder()
                .diaSemana(dia)
                .horaInicio(inicio)
                .horaFin(fin)
                .duracionCitaMinutos(30)
                .build();
    }

    static LocalDate proximoDiaHabil(LocalDate desde) {
        LocalDate fecha = desde.plusDays(1);
        while (!LUNES_A_VIERNES.contains(fecha.getDayOfWeek())) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    private static String pad(String texto) {
        return String.format("%-30s", texto);
    }
}
