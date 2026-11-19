package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaVerificarRequest;
import com.codesoftlabs.tusalud.app.dto.request.RefreshTokenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RefreshToken;
import com.codesoftlabs.tusalud.app.entities.Rol;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.RolRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import com.codesoftlabs.tusalud.app.services.interfaces.MfaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RefreshTokenService;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String MENSAJE_REFRESH_INVALIDO = "El refresh token no es válido o expiró";

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SesionService sesionService;
    private final MfaService mfaService;
    private final RefreshTokenService refreshTokenService;
    private final NotificacionService notificacionService;
    private final EventoSeguridadService eventoSeguridadService;

    @Override
    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthResponse login(LoginRequest request, ContextoConexion contexto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizarEmail(request.email()), request.password()));
        } catch (AuthenticationException ex) {
            registrarLoginFallido(request.email(), contexto, ex instanceof DisabledException);
            throw ex;
        }
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        Usuario usuario = usuarioRepository
                .findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado inexistente: " + principal.getId()));
        if (mfaService.estaActivo(usuario.getId())) {
            log.info("Inicio de sesión pendiente de verificación en dos pasos para {}", principal.getId());
            return AuthResponse.builder()
                    .tokenType(TOKEN_TYPE)
                    .usuarioId(principal.getId())
                    .email(principal.getEmail())
                    .nombreCompleto(principal.getNombreCompleto())
                    .roles(principal.getRoles())
                    .mfaRequerido(true)
                    .mfaToken(jwtUtils.generateMfaToken(principal))
                    .build();
        }
        UUID tokenId = sesionService.iniciarSesion(usuario, contexto);
        log.info("Inicio de sesión exitoso para {}", principal.getId());
        return construirRespuesta(principal, tokenId);
    }

    private void registrarLoginFallido(String email, ContextoConexion contexto, boolean inactiva) {
        UUID usuarioId = usuarioRepository
                .findByEmailWithRoles(normalizarEmail(email))
                .map(Usuario::getId)
                .orElse(null);
        if (inactiva) {
            eventoSeguridadService.registrar(
                    TipoEventoSeguridad.LOGIN_CUENTA_INACTIVA,
                    usuarioId,
                    null,
                    "Intento de acceso con una cuenta inactiva",
                    contexto);
            return;
        }
        eventoSeguridadService.registrarLoginFallido(
                usuarioId,
                usuarioId == null ? "Credenciales inválidas para una cuenta inexistente" : "Credenciales inválidas",
                contexto);
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, DemasiadosIntentosException.class})
    public AuthResponse verificarMfa(MfaVerificarRequest request, ContextoConexion contexto) {
        UUID usuarioId;
        try {
            usuarioId = jwtUtils.extractMfaUserId(request.mfaToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("El token de verificación no es válido o expiró");
        }
        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new BadCredentialsException("El token de verificación no es válido o expiró"));
        mfaService.validarSegundoPaso(usuarioId, request.codigo());
        UUID tokenId = sesionService.iniciarSesion(usuario, contexto);
        log.info("Inicio de sesión con verificación en dos pasos para {}", usuario.getId());
        return construirRespuesta(UsuarioPrincipal.from(usuario), tokenId);
    }

    @Override
    @Transactional
    public AuthResponse registrarPaciente(RegisterRequest request, ContextoConexion contexto) {
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
                .sexo(request.sexo())
                .historialClinicoNumero(String.format("HC-%06d", pacienteRepository.nextCorrelativo()))
                .usuario(usuario)
                .build();
        pacienteRepository.save(paciente);

        UUID tokenId = sesionService.iniciarSesion(usuario, contexto);
        log.info(
                "Paciente registrado: {} (historia clínica {})",
                paciente.getId(),
                paciente.getHistorialClinicoNumero());
        return construirRespuesta(UsuarioPrincipal.from(usuario), tokenId);
    }

    @Override
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse refrescar(RefreshTokenRequest request) {
        RefreshToken actual = refreshTokenService.buscarPorToken(request.refreshToken());
        if (actual == null) {
            throw new BadCredentialsException(MENSAJE_REFRESH_INVALIDO);
        }
        Usuario usuario = actual.getUsuario();
        SesionUsuario sesion = actual.getSesion();
        if (actual.isUsado()) {
            refreshTokenService.revocarFamilia(actual.getFamilia());
            sesion.setRevocada(true);
            notificacionService.crear(
                    usuario,
                    TipoNotificacion.SESION_SOSPECHOSA,
                    "Sesión cerrada por seguridad",
                    "Detectamos un intento de reutilizar una credencial de sesión ya renovada y cerramos esa sesión. Si no fuiste tú, cambia tu contraseña.",
                    "/perfil");
            log.warn("Reutilización de refresh token detectada para {}", usuario.getId());
            throw new BadCredentialsException(MENSAJE_REFRESH_INVALIDO);
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (actual.isRevocado()
                || !actual.getExpiraEn().isAfter(ahora)
                || !usuario.isActivo()
                || !sesion.estaActiva(ahora)) {
            throw new BadCredentialsException(MENSAJE_REFRESH_INVALIDO);
        }
        String nuevo = refreshTokenService.rotar(actual);
        sesion.setUltimoAcceso(ahora);
        sesion.setExpiraEn(ahora.plus(Duration.ofMillis(refreshTokenService.getRefreshExpirationMs())));
        return construirRespuesta(UsuarioPrincipal.from(usuario), sesion.getTokenId(), nuevo);
    }

    @Override
    @Transactional
    public void cerrarSesion(UUID tokenId) {
        sesionService.cerrarSesion(tokenId);
    }

    private Rol obtenerOCrearRol(RolNombre nombre) {
        return rolRepository
                .findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private AuthResponse construirRespuesta(UsuarioPrincipal principal, UUID tokenId) {
        return construirRespuesta(principal, tokenId, refreshTokenService.emitirParaSesion(tokenId));
    }

    private AuthResponse construirRespuesta(UsuarioPrincipal principal, UUID tokenId, String refreshToken) {
        return AuthResponse.builder()
                .token(jwtUtils.generateToken(principal, tokenId))
                .refreshToken(refreshToken)
                .refreshExpiresIn(jwtUtils.getRefreshExpirationMs())
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtUtils.getExpirationMs())
                .usuarioId(principal.getId())
                .email(principal.getEmail())
                .nombreCompleto(principal.getNombreCompleto())
                .roles(principal.getRoles())
                .mfaRequerido(false)
                .build();
    }

    private static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
