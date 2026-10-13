package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizarEmail(request.email()), request.password()));
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        log.info("Inicio de sesión exitoso para {}", principal.getEmail());
        return construirRespuesta(principal);
    }

    @Override
    @Transactional
    public AuthResponse registrarPaciente(RegisterRequest request) {
        String email = normalizarEmail(request.email());
        String dni = request.dni().trim();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe un usuario registrado con el email " + email);
        }
        if (pacienteRepository.existsByDni(dni)) {
            throw new ConflictException("Ya existe un paciente registrado con el DNI " + dni);
        }

        Set<Rol> roles = new HashSet<>();
        roles.add(obtenerOCrearRol(RolNombre.ROLE_PACIENTE));

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .apellido(request.apellido().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .estado(EstadoUsuario.ACTIVO)
                .roles(roles)
                .build();
        usuario = usuarioRepository.save(usuario);

        Paciente paciente = Paciente.builder()
                .dni(dni)
                .fechaNacimiento(request.fechaNacimiento())
                .historialClinicoNumero(String.format("HC-%06d", pacienteRepository.nextCorrelativo()))
                .usuario(usuario)
                .build();
        pacienteRepository.save(paciente);

        log.info("Paciente registrado: {} (historia clínica {})", email, paciente.getHistorialClinicoNumero());
        return construirRespuesta(UsuarioPrincipal.from(usuario));
    }

    private Rol obtenerOCrearRol(RolNombre nombre) {
        return rolRepository
                .findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private AuthResponse construirRespuesta(UsuarioPrincipal principal) {
        return AuthResponse.builder()
                .token(jwtUtils.generateToken(principal))
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtUtils.getExpirationMs())
                .usuarioId(principal.getId())
                .email(principal.getEmail())
                .nombreCompleto(principal.getNombreCompleto())
                .roles(principal.getRoles())
                .build();
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
