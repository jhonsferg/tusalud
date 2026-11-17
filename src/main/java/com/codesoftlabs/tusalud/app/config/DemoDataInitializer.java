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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private static final String MEDICO_PASSWORD = "Medico123!";
    private static final String PACIENTE_PASSWORD = "Paciente123!";

    private record Bloque(LocalTime inicio, LocalTime fin) {}

    private record MedicoSeed(
            String nombre,
            String apellido,
            String email,
            String telefono,
            String colegiatura,
            String consultorio,
            String precio,
            String especialidad,
            String descripcionEspecialidad,
            Set<DayOfWeek> dias,
            List<Bloque> bloques) {}

    private record PacienteSeed(
            String nombre, String apellido, String email, String telefono, String dni, LocalDate nacimiento) {}

    private record CitaSeed(
            String medicoEmail,
            String pacienteEmail,
            int diasDesdeHoy,
            LocalTime hora,
            EstadoCita estado,
            String motivo) {}

    private static final List<MedicoSeed> MEDICOS = List.of(
            new MedicoSeed(
                    "Ricardo",
                    "Salazar",
                    "dr.salazar@tusalud.com",
                    "934567890",
                    "CMP-23456",
                    "Consultorio 102",
                    "90.00",
                    "Medicina General",
                    "Atención primaria, prevención y diagnóstico general",
                    dias(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY),
                    List.of(bloque(8, 12), bloque(15, 18))),
            new MedicoSeed(
                    "Patricia",
                    "Vega",
                    "dra.vega@tusalud.com",
                    "945678901",
                    "CMP-34567",
                    "Consultorio 310",
                    "130.00",
                    "Dermatología",
                    "Diagnóstico y tratamiento de enfermedades de la piel",
                    dias(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                    List.of(bloque(9, 14))),
            new MedicoSeed(
                    "Andrés",
                    "Quispe",
                    "dr.quispe@tusalud.com",
                    "956789012",
                    "CMP-45678",
                    "Consultorio 215",
                    "160.00",
                    "Traumatología",
                    "Lesiones y enfermedades del aparato locomotor",
                    dias(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    List.of(bloque(10, 16))),
            new MedicoSeed(
                    "Lucía",
                    "Fernández",
                    "dra.fernandez@tusalud.com",
                    "967890123",
                    "CMP-56789",
                    "Consultorio 402",
                    "140.00",
                    "Ginecología",
                    "Salud de la mujer, control prenatal y planificación familiar",
                    dias(
                            DayOfWeek.MONDAY,
                            DayOfWeek.TUESDAY,
                            DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY,
                            DayOfWeek.FRIDAY),
                    List.of(bloque(14, 19))),
            new MedicoSeed(
                    "Marcos",
                    "Delgado",
                    "dr.delgado@tusalud.com",
                    "978901234",
                    "CMP-67801",
                    "Consultorio 118",
                    "110.00",
                    "Oftalmología",
                    "Diagnóstico y tratamiento de enfermedades de los ojos",
                    dias(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                    List.of(bloque(8, 13))));

    private static final List<PacienteSeed> PACIENTES = List.of(
            new PacienteSeed(
                    "Sofía", "Ramírez", "sofia.ramirez@correo.com", "911223344", "45123678", LocalDate.of(1994, 3, 8)),
            new PacienteSeed(
                    "Carlos",
                    "Mendoza",
                    "carlos.mendoza@correo.com",
                    "922334455",
                    "40213456",
                    LocalDate.of(1978, 9, 21)),
            new PacienteSeed(
                    "Valeria",
                    "Castillo",
                    "valeria.castillo@correo.com",
                    "933445566",
                    "71234509",
                    LocalDate.of(1998, 12, 2)),
            new PacienteSeed(
                    "Diego", "Herrera", "diego.herrera@correo.com", "944556677", "46789012", LocalDate.of(1989, 6, 17)),
            new PacienteSeed(
                    "Camila", "Torres", "camila.torres@correo.com", "955667788", "72345611", LocalDate.of(2001, 1, 30)),
            new PacienteSeed(
                    "Luis Alberto",
                    "Vargas",
                    "luis.vargas@correo.com",
                    "966778899",
                    "43219876",
                    LocalDate.of(1972, 11, 5)),
            new PacienteSeed(
                    "Ana Lucía",
                    "Paredes",
                    "ana.paredes@correo.com",
                    "977889900",
                    "70123987",
                    LocalDate.of(1992, 7, 14)),
            new PacienteSeed(
                    "Jorge", "Medina", "jorge.medina@correo.com", "988990011", "41567890", LocalDate.of(1965, 4, 25)));

    private static final String GARCIA = "dr.garcia@tusalud.com";
    private static final String LOPEZ = "dra.lopez@tusalud.com";
    private static final String SALAZAR = "dr.salazar@tusalud.com";
    private static final String VEGA = "dra.vega@tusalud.com";
    private static final String QUISPE = "dr.quispe@tusalud.com";
    private static final String FERNANDEZ = "dra.fernandez@tusalud.com";
    private static final String DELGADO = "dr.delgado@tusalud.com";

    private static final List<CitaSeed> CITAS = List.of(
            cita(SALAZAR, "sofia.ramirez@correo.com", -21, 8, 30, EstadoCita.ATENDIDA, "Chequeo general anual"),
            cita(GARCIA, "carlos.mendoza@correo.com", -14, 9, 0, EstadoCita.ATENDIDA, "Seguimiento de hipertensión"),
            cita(VEGA, "valeria.castillo@correo.com", -10, 10, 0, EstadoCita.ATENDIDA, "Control de acné"),
            cita(
                    QUISPE,
                    "diego.herrera@correo.com",
                    -9,
                    11,
                    0,
                    EstadoCita.ATENDIDA,
                    "Dolor de rodilla tras una lesión deportiva"),
            cita(FERNANDEZ, "camila.torres@correo.com", -7, 15, 0, EstadoCita.ATENDIDA, "Control ginecológico anual"),
            cita(
                    DELGADO,
                    "luis.vargas@correo.com",
                    -6,
                    9,
                    30,
                    EstadoCita.ATENDIDA,
                    "Revisión de la vista por fatiga visual"),
            cita(
                    LOPEZ,
                    "ana.paredes@correo.com",
                    -5,
                    14,
                    30,
                    EstadoCita.CANCELADA,
                    "Control de crecimiento de mi hija"),
            cita(SALAZAR, "jorge.medina@correo.com", -4, 16, 0, EstadoCita.CANCELADA, "Certificado médico laboral"),
            cita(VEGA, "sofia.ramirez@correo.com", -3, 11, 30, EstadoCita.ATENDIDA, "Revisión de manchas en la piel"),
            cita(GARCIA, "sofia.ramirez@correo.com", 2, 10, 0, EstadoCita.CONFIRMADA, "Palpitaciones ocasionales"),
            cita(
                    GARCIA,
                    "camila.torres@correo.com",
                    2,
                    11,
                    0,
                    EstadoCita.PENDIENTE,
                    "Dolor en el pecho al caminar rápido"),
            cita(
                    GARCIA,
                    "diego.herrera@correo.com",
                    2,
                    12,
                    0,
                    EstadoCita.CONFIRMADA,
                    "Chequeo cardiológico preventivo"),
            cita(SALAZAR, "carlos.mendoza@correo.com", 2, 9, 0, EstadoCita.PENDIENTE, "Resfriado persistente"),
            cita(FERNANDEZ, "valeria.castillo@correo.com", 3, 16, 0, EstadoCita.CONFIRMADA, "Control prenatal"),
            cita(DELGADO, "diego.herrera@correo.com", 4, 10, 0, EstadoCita.PENDIENTE, "Cambio de lentes"),
            cita(VEGA, "camila.torres@correo.com", 5, 10, 30, EstadoCita.CONFIRMADA, "Revisión de lunares"),
            cita(QUISPE, "luis.vargas@correo.com", 5, 12, 0, EstadoCita.PENDIENTE, "Dolor lumbar persistente"),
            cita(LOPEZ, "ana.paredes@correo.com", 6, 16, 0, EstadoCita.CONFIRMADA, "Vacunación de refuerzo"),
            cita(GARCIA, "jorge.medina@correo.com", 7, 11, 0, EstadoCita.PENDIENTE, "Control de colesterol"),
            cita(SALAZAR, "juan.perez@correo.com", 8, 15, 30, EstadoCita.PENDIENTE, "Dolor de garganta"),
            cita(VEGA, "maria.rodriguez@correo.com", 9, 12, 0, EstadoCita.CONFIRMADA, "Dermatitis en las manos"));

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
        log.info("Cargando datos de demostración...");
        Rol rolMedico = rolRepository.findByNombre(RolNombre.ROLE_MEDICO).orElseThrow();
        Rol rolPaciente = rolRepository.findByNombre(RolNombre.ROLE_PACIENTE).orElseThrow();

        int nuevosMedicos = 0;
        for (MedicoSeed seed : MEDICOS) {
            if (usuarioRepository.existsByEmailIgnoreCase(seed.email())
                    || medicoRepository.existsByColegiaturaIgnoreCase(seed.colegiatura())) {
                continue;
            }
            Medico medico = Medico.builder()
                    .colegiatura(seed.colegiatura())
                    .consultorio(seed.consultorio())
                    .precioConsulta(new BigDecimal(seed.precio()))
                    .especialidad(obtenerOCrearEspecialidad(seed.especialidad(), seed.descripcionEspecialidad()))
                    .usuario(nuevoUsuario(
                            seed.nombre(), seed.apellido(), seed.email(), MEDICO_PASSWORD, seed.telefono(), rolMedico))
                    .build();
            for (DayOfWeek dia : seed.dias()) {
                for (Bloque bloque : seed.bloques()) {
                    medico.agregarHorario(HorarioAtencion.builder()
                            .diaSemana(dia)
                            .horaInicio(bloque.inicio())
                            .horaFin(bloque.fin())
                            .duracionCitaMinutos(30)
                            .build());
                }
            }
            medicoRepository.save(medico);
            nuevosMedicos++;
        }

        int nuevosPacientes = 0;
        for (PacienteSeed seed : PACIENTES) {
            if (usuarioRepository.existsByEmailIgnoreCase(seed.email()) || pacienteRepository.existsByDni(seed.dni())) {
                continue;
            }
            pacienteRepository.save(Paciente.builder()
                    .dni(seed.dni())
                    .fechaNacimiento(seed.nacimiento())
                    .historialClinicoNumero(String.format("HC-%06d", pacienteRepository.nextCorrelativo()))
                    .usuario(nuevoUsuario(
                            seed.nombre(),
                            seed.apellido(),
                            seed.email(),
                            PACIENTE_PASSWORD,
                            seed.telefono(),
                            rolPaciente))
                    .build());
            nuevosPacientes++;
        }

        Set<String> ocupadas = new HashSet<>();
        citaRepository.findAll().forEach(c -> ocupadas.add(c.getMedico().getId() + "|" + c.getFechaHora()));
        int nuevasCitas = 0;
        for (CitaSeed seed : CITAS) {
            Medico medico =
                    medicoRepository.findByUsuarioEmail(seed.medicoEmail()).orElse(null);
            Paciente paciente =
                    pacienteRepository.findByUsuarioEmail(seed.pacienteEmail()).orElse(null);
            if (medico == null || paciente == null) {
                continue;
            }
            LocalDateTime fechaHora = resolverFecha(medico, seed).atTime(seed.hora());
            String clave = medico.getId() + "|" + fechaHora;
            boolean existe = citaRepository.findAll().stream()
                    .anyMatch(c -> c.getMedico().getId().equals(medico.getId())
                            && c.getPaciente().getId().equals(paciente.getId())
                            && c.getMotivo().equals(seed.motivo()));
            if (existe || !ocupadas.add(clave)) {
                continue;
            }
            citaRepository.save(Cita.builder()
                    .medico(medico)
                    .paciente(paciente)
                    .fechaHora(fechaHora)
                    .motivo(seed.motivo())
                    .estado(seed.estado())
                    .monto(medico.getPrecioConsulta())
                    .build());
            nuevasCitas++;
        }

        log.info(
                "Datos de demostración: {} médicos, {} pacientes y {} citas nuevos",
                nuevosMedicos,
                nuevosPacientes,
                nuevasCitas);
    }

    private LocalDate resolverFecha(Medico medico, CitaSeed seed) {
        Set<DayOfWeek> atiende = new HashSet<>();
        medico.getHorarios().forEach(h -> atiende.add(h.getDiaSemana()));
        int paso = seed.diasDesdeHoy() < 0 ? -1 : 1;
        LocalDate fecha = LocalDate.now().plusDays(seed.diasDesdeHoy());
        while (!atiende.contains(fecha.getDayOfWeek())) {
            fecha = fecha.plusDays(paso);
        }
        return fecha;
    }

    private Especialidad obtenerOCrearEspecialidad(String nombre, String descripcion) {
        return especialidadRepository.findAll().stream()
                .filter(e -> e.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> especialidadRepository.save(Especialidad.builder()
                        .nombre(nombre)
                        .descripcion(descripcion)
                        .build()));
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

    private static Set<DayOfWeek> dias(DayOfWeek... dias) {
        return Set.of(dias);
    }

    private static Bloque bloque(int inicio, int fin) {
        return new Bloque(LocalTime.of(inicio, 0), LocalTime.of(fin, 0));
    }

    private static CitaSeed cita(
            String medico, String paciente, int dias, int hora, int minuto, EstadoCita estado, String motivo) {
        return new CitaSeed(medico, paciente, dias, LocalTime.of(hora, minuto), estado, motivo);
    }
}
