package com.codesoftlabs.tusalud.app.services;

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
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ServiceTestFixture {

    public static final String PASSWORD = "Secreta123!";

    private static final AtomicInteger SECUENCIA = new AtomicInteger(1000);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadRepository especialidadRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final PasswordEncoder passwordEncoder;

    public ServiceTestFixture(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            EspecialidadRepository especialidadRepository,
            MedicoRepository medicoRepository,
            PacienteRepository pacienteRepository,
            CitaRepository citaRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.especialidadRepository = especialidadRepository;
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static int siguiente() {
        return SECUENCIA.incrementAndGet();
    }

    public Rol rol(RolNombre nombre) {
        return rolRepository
                .findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    public Usuario usuario(String prefijo, RolNombre rol) {
        int n = siguiente();
        Set<Rol> roles = new HashSet<>();
        roles.add(rol(rol));
        return usuarioRepository.save(Usuario.builder()
                .nombre("Nombre" + n)
                .apellido("Apellido" + n)
                .email("svc.test." + prefijo + n + "@test.com")
                .password(passwordEncoder.encode(PASSWORD))
                .estado(EstadoUsuario.ACTIVO)
                .roles(roles)
                .build());
    }

    public Especialidad especialidad() {
        return especialidadRepository.save(Especialidad.builder()
                .nombre("SvcTest Especialidad " + siguiente())
                .build());
    }

    public Medico medicoConHorarioLunes(BigDecimal precio) {
        Medico medico = Medico.builder()
                .colegiatura("SVC-" + siguiente())
                .consultorio("Consultorio 1")
                .precioConsulta(precio)
                .usuario(usuario("medico", RolNombre.ROLE_MEDICO))
                .especialidad(especialidad())
                .build();
        medico.agregarHorario(HorarioAtencion.builder()
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .duracionCitaMinutos(30)
                .build());
        return medicoRepository.save(medico);
    }

    public Paciente paciente() {
        int n = siguiente();
        return pacienteRepository.save(Paciente.builder()
                .dni(String.format("9%07d", n))
                .historialClinicoNumero("SVC-HC-" + n)
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .usuario(usuario("paciente", RolNombre.ROLE_PACIENTE))
                .build());
    }

    public Cita cita(Medico medico, Paciente paciente, LocalDateTime fechaHora, EstadoCita estado) {
        return citaRepository.save(Cita.builder()
                .fechaHora(fechaHora)
                .motivo("Control")
                .estado(estado)
                .monto(medico.getPrecioConsulta())
                .medico(medico)
                .paciente(paciente)
                .build());
    }

    public UsuarioPrincipal principal(Usuario usuario) {
        return UsuarioPrincipal.from(usuario);
    }
}
