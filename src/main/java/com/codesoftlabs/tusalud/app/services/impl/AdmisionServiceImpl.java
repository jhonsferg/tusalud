package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.CitaMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.PacienteMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMostradorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteRegistradoResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AdmisionService;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdmisionServiceImpl implements AdmisionService {

    private static final int LIMITE_BUSQUEDA = 30;
    private static final int LONGITUD_MINIMA_BUSQUEDA = 2;
    private static final int LONGITUD_PASSWORD = 10;
    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String DOMINIO_INTERNO = "@pacientes.tusalud.local";

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final CitaService citaService;
    private final HistoriaClinicaService historiaClinicaService;
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    @Transactional(readOnly = true)
    public List<PacienteMostradorResponse> buscarPacientes(String q, UsuarioPrincipal actor) {
        String termino = q == null ? "" : q.trim();
        if (termino.length() < LONGITUD_MINIMA_BUSQUEDA) {
            return List.of();
        }
        boolean mostrador = actor.getRoles().contains(RolNombre.ROLE_RECEPCION.name())
                || actor.getRoles().contains(RolNombre.ROLE_ADMIN.name());
        if (mostrador) {
            return pacienteRepository.buscarEnMostrador(termino, PageRequest.of(0, LIMITE_BUSQUEDA)).stream()
                    .map(PacienteMostradorResponse::from)
                    .toList();
        }
        if (actor.getRoles().contains(RolNombre.ROLE_MEDICO.name())) {
            String buscado = termino.toLowerCase(Locale.ROOT);
            return historiaClinicaService.listarPacientesDeMedico(actor).stream()
                    .filter(paciente -> paciente.dni().startsWith(termino)
                            || paciente.nombreCompleto()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(buscado))
                    .limit(LIMITE_BUSQUEDA)
                    .map(paciente -> PacienteMostradorResponse.minimo(
                            paciente.pacienteId(), paciente.nombreCompleto(), paciente.dni(), null))
                    .toList();
        }
        return pacienteRepository.buscarEnMostrador(termino, PageRequest.of(0, LIMITE_BUSQUEDA)).stream()
                .map(PacienteMostradorResponse::from)
                .map(PacienteMostradorResponse::sinDatosPersonales)
                .toList();
    }

    @Override
    public PacienteRegistradoResponse registrarPaciente(PacienteMostradorRequest request) {
        String dni = request.dni().trim();
        if (pacienteRepository.existsByDni(dni)) {
            throw new ConflictException("Ya existe un paciente registrado con el DNI " + dni);
        }
        String email = request.email() == null || request.email().isBlank()
                ? dni + DOMINIO_INTERNO
                : request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario registrado con el email " + email);
        }
        Rol rolPaciente = rolRepository
                .findByNombre(RolNombre.ROLE_PACIENTE)
                .orElseGet(() -> rolRepository.save(
                        Rol.builder().nombre(RolNombre.ROLE_PACIENTE).build()));
        Set<Rol> roles = new HashSet<>();
        roles.add(rolPaciente);
        String passwordTemporal = generarPassword();
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(email)
                .password(passwordEncoder.encode(passwordTemporal))
                .telefono(request.telefono())
                .estado(EstadoUsuario.ACTIVO)
                .roles(roles)
                .build());
        Paciente paciente = pacienteRepository.save(Paciente.builder()
                .dni(dni)
                .fechaNacimiento(request.fechaNacimiento())
                .sexo(request.sexo())
                .historialClinicoNumero(String.format("HC-%06d", pacienteRepository.nextCorrelativo()))
                .usuario(usuario)
                .build());
        log.info(
                "Paciente registrado en mostrador: {} (historia clínica {})",
                paciente.getId(),
                paciente.getHistorialClinicoNumero());
        return new PacienteRegistradoResponse(PacienteMostradorResponse.from(paciente), passwordTemporal);
    }

    @Override
    public CitaResponse reservarCita(CitaMostradorRequest request) {
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        UsuarioPrincipal principalPaciente = UsuarioPrincipal.from(paciente.getUsuario());
        return citaService.crearCita(
                new CitaCreateRequest(
                        request.medicoId(), request.fechaHora(), request.motivo(), request.especialidadId()),
                principalPaciente);
    }

    private String generarPassword() {
        String todos = MAYUSCULAS + MINUSCULAS + DIGITOS;
        StringBuilder sb = new StringBuilder();
        sb.append(MAYUSCULAS.charAt(aleatorio.nextInt(MAYUSCULAS.length())));
        sb.append(MINUSCULAS.charAt(aleatorio.nextInt(MINUSCULAS.length())));
        sb.append(DIGITOS.charAt(aleatorio.nextInt(DIGITOS.length())));
        while (sb.length() < LONGITUD_PASSWORD) {
            sb.append(todos.charAt(aleatorio.nextInt(todos.length())));
        }
        char[] caracteres = sb.toString().toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = aleatorio.nextInt(i + 1);
            char temporal = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temporal;
        }
        return new String(caracteres);
    }
}
