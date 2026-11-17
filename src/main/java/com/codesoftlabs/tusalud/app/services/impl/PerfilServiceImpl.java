package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AvatarRequest;
import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.dto.response.PreferenciasResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.MfaUsuario;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PreferenciaUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.MfaUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import com.codesoftlabs.tusalud.app.services.interfaces.RefreshTokenService;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerfilServiceImpl implements PerfilService {

    private static final long TAMANO_MAXIMO_AVATAR_BYTES = 300L * 1024;
    private static final Pattern PATRON_AVATAR =
            Pattern.compile("^data:(image/(?:png|jpeg|webp));base64,([A-Za-z0-9+/]+={0,2})$");

    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PreferenciaUsuarioRepository preferenciaRepository;
    private final NotificacionService notificacionService;
    private final MfaUsuarioRepository mfaUsuarioRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public PerfilResponse obtener(UsuarioPrincipal principal) {
        Usuario usuario = cargarUsuario(principal);
        return construir(usuario, principal);
    }

    @Override
    @Transactional
    public PerfilResponse actualizar(UsuarioPrincipal principal, PerfilUpdateRequest request) {
        Usuario usuario = cargarUsuario(principal);
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setTelefono(vacioANulo(request.telefono()));

        pacienteRepository.findByUsuarioEmail(usuario.getEmail()).ifPresent(paciente -> {
            if (request.fechaNacimiento() != null) {
                paciente.setFechaNacimiento(request.fechaNacimiento());
            }
        });
        medicoRepository
                .findByUsuarioEmail(usuario.getEmail())
                .ifPresent(medico -> medico.setConsultorio(vacioANulo(request.consultorio())));

        log.info("Perfil actualizado para {}", usuario.getId());
        return construir(usuario, principal);
    }

    @Override
    @Transactional
    public void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request) {
        cambiarPassword(principal, request, null);
    }

    @Override
    @Transactional
    public void cambiarPassword(UsuarioPrincipal principal, PasswordChangeRequest request, UUID tokenActual) {
        Usuario usuario = cargarUsuario(principal);
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new BusinessLogicException("La contraseña actual no es correcta");
        }
        if (request.passwordActual().equals(request.passwordNueva())) {
            throw new BusinessLogicException("La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        refreshTokenService.revocarDeUsuarioExceptoSesion(usuario.getId(), tokenActual);
        notificacionService.crear(
                usuario,
                TipoNotificacion.CONTRASENA_CAMBIADA,
                "Contraseña actualizada",
                "Su contraseña fue cambiada. Si no fue usted, comuníquese con soporte.",
                "/perfil");
        log.info("Contraseña actualizada para {}", usuario.getId());
    }

    @Override
    public PreferenciasResponse obtenerPreferencias(UsuarioPrincipal principal) {
        return preferenciaRepository
                .findByUsuarioId(principal.getId())
                .map(PreferenciasResponse::from)
                .orElseGet(PreferenciasResponse::porDefecto);
    }

    @Override
    @Transactional
    public PreferenciasResponse actualizarPreferencias(UsuarioPrincipal principal, PreferenciasRequest request) {
        PreferenciaUsuario preferencia = preferenciaRepository
                .findByUsuarioId(principal.getId())
                .orElseGet(() -> PreferenciaUsuario.builder()
                        .usuario(cargarUsuario(principal))
                        .build());
        preferencia.setRecordatoriosActivos(request.recordatoriosActivos());
        preferencia.setMinutosAntelacionRecordatorio(request.minutosAntelacionRecordatorio());
        preferencia.setAlertasInicioSesion(request.alertasInicioSesion());
        if (request.tourCompletado() != null) {
            preferencia.setTourCompletado(request.tourCompletado());
        }
        return PreferenciasResponse.from(preferenciaRepository.saveAndFlush(preferencia));
    }

    @Override
    @Transactional
    public PerfilResponse actualizarAvatar(UsuarioPrincipal principal, AvatarRequest request) {
        Usuario usuario = cargarUsuario(principal);
        String imagen = request.imagen().trim();
        Matcher coincidencia = PATRON_AVATAR.matcher(imagen);
        if (!coincidencia.matches()) {
            throw new DatosInvalidosException(
                    "La imagen debe ser una data URL en formato PNG, JPEG o WEBP codificada en base64");
        }
        byte[] contenido;
        try {
            contenido = Base64.getDecoder().decode(coincidencia.group(2));
        } catch (IllegalArgumentException ex) {
            throw new DatosInvalidosException("El contenido base64 de la imagen no es válido");
        }
        if (contenido.length > TAMANO_MAXIMO_AVATAR_BYTES) {
            throw new DatosInvalidosException("La imagen no debe superar 300 KB");
        }
        usuario.setAvatar(imagen);
        log.info("Avatar actualizado para {} ({} bytes)", usuario.getId(), contenido.length);
        return construir(usuario, principal);
    }

    @Override
    @Transactional
    public void eliminarAvatar(UsuarioPrincipal principal) {
        cargarUsuario(principal).setAvatar(null);
    }

    private Usuario cargarUsuario(UsuarioPrincipal principal) {
        return usuarioRepository
                .findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + principal.getId()));
    }

    private PerfilResponse construir(Usuario usuario, UsuarioPrincipal principal) {
        Paciente paciente =
                pacienteRepository.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        Medico medico = medicoRepository.findByUsuarioEmail(usuario.getEmail()).orElse(null);
        boolean mfaActivo = mfaUsuarioRepository
                .findByUsuarioId(usuario.getId())
                .map(MfaUsuario::isActivo)
                .orElse(false);
        return PerfilResponse.from(usuario, principal.getRoles(), paciente, medico, mfaActivo);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
