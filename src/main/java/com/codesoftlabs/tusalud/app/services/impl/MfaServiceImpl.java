package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.MfaCodigosRecuperacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaConfiguracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaEstadoResponse;
import com.codesoftlabs.tusalud.app.entities.MfaCodigoRecuperacion;
import com.codesoftlabs.tusalud.app.entities.MfaUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MfaCodigoRecuperacionRepository;
import com.codesoftlabs.tusalud.app.repositories.MfaUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.CifradorSecretos;
import com.codesoftlabs.tusalud.app.security.TotpService;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import com.codesoftlabs.tusalud.app.services.interfaces.MfaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MfaServiceImpl implements MfaService {

    private static final int CODIGOS_RECUPERACION = 8;
    private static final int MAX_INTENTOS = 5;
    private static final int MINUTOS_BLOQUEO = 5;
    private static final String ALFABETO_RECUPERACION = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_TOTP = 6;

    private final MfaUsuarioRepository mfaRepository;
    private final MfaCodigoRecuperacionRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final CifradorSecretos cifrador;
    private final NotificacionService notificacionService;
    private final EventoSeguridadService eventoSeguridadService;

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public MfaEstadoResponse estado(UsuarioPrincipal actor) {
        boolean activo = estaActivo(actor.getId());
        return new MfaEstadoResponse(
                activo, activo ? codigoRepository.countByUsuarioIdAndUsadoFalse(actor.getId()) : 0);
    }

    @Override
    public boolean estaActivo(UUID usuarioId) {
        return mfaRepository
                .findByUsuarioId(usuarioId)
                .map(MfaUsuario::isActivo)
                .orElse(false);
    }

    @Override
    @Transactional
    public MfaConfiguracionResponse configurar(UsuarioPrincipal actor) {
        Usuario usuario = cargarUsuario(actor.getId());
        MfaUsuario mfa = mfaRepository.findByUsuarioId(actor.getId()).orElse(null);
        if (mfa != null && mfa.isActivo()) {
            throw new BusinessLogicException("La verificación en dos pasos ya está activada");
        }
        String secreto = totpService.generarSecreto();
        if (mfa == null) {
            mfa = MfaUsuario.builder().usuario(usuario).build();
        }
        mfa.setSecretoCifrado(cifrador.cifrar(secreto));
        mfa.setActivo(false);
        mfa.setIntentosFallidos(0);
        mfa.setBloqueadoHasta(null);
        mfaRepository.save(mfa);
        return new MfaConfiguracionResponse(secreto, totpService.otpauthUri(usuario.getEmail(), secreto));
    }

    @Override
    @Transactional(noRollbackFor = {DatosInvalidosException.class, DemasiadosIntentosException.class})
    public MfaCodigosRecuperacionResponse activar(UsuarioPrincipal actor, String codigo) {
        MfaUsuario mfa = mfaRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new BusinessLogicException("Primero genere el secreto de la verificación"));
        if (mfa.isActivo()) {
            throw new BusinessLogicException("La verificación en dos pasos ya está activada");
        }
        verificarBloqueo(mfa);
        if (!totpService.verificar(cifrador.descifrar(mfa.getSecretoCifrado()), codigo, Instant.now())) {
            registrarFallo(mfa);
            throw new DatosInvalidosException("El código de verificación no es válido");
        }
        mfa.setActivo(true);
        reiniciarIntentos(mfa);
        List<String> codigos = emitirCodigosRecuperacion(mfa.getUsuario());
        notificacionService.crear(
                mfa.getUsuario(),
                TipoNotificacion.MFA_ACTIVADO,
                "Verificación en dos pasos activada",
                "Tu cuenta ahora solicita un código de la aplicación autenticadora al iniciar sesión.",
                "/perfil?tab=seguridad");
        eventoSeguridadService.registrar(
                TipoEventoSeguridad.MFA_ACTIVADO, actor.getId(), actor.getId(), "Verificación en dos pasos activada");
        log.info("Auditoría: MFA activado para el usuario {}", actor.getId());
        return new MfaCodigosRecuperacionResponse(codigos);
    }

    @Override
    @Transactional(noRollbackFor = {DatosInvalidosException.class, DemasiadosIntentosException.class})
    public void desactivar(UsuarioPrincipal actor, String password, String codigo) {
        Usuario usuario = cargarUsuario(actor.getId());
        MfaUsuario mfa = mfaActivo(actor.getId());
        verificarBloqueo(mfa);
        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            registrarFallo(mfa);
            throw new DatosInvalidosException("La contraseña es incorrecta");
        }
        if (!aceptarCodigo(mfa, codigo)) {
            throw new DatosInvalidosException("El código de verificación no es válido");
        }
        codigoRepository.eliminarDeUsuario(actor.getId());
        mfaRepository.delete(mfa);
        notificacionService.crear(
                usuario,
                TipoNotificacion.MFA_DESACTIVADO,
                "Verificación en dos pasos desactivada",
                "Se desactivó la verificación en dos pasos de tu cuenta. Si no fuiste tú, cambia tu contraseña.",
                "/perfil?tab=seguridad");
        eventoSeguridadService.registrar(
                TipoEventoSeguridad.MFA_DESACTIVADO,
                actor.getId(),
                actor.getId(),
                "Verificación en dos pasos desactivada");
        log.info("Auditoría: MFA desactivado para el usuario {}", actor.getId());
    }

    @Override
    @Transactional(noRollbackFor = {DatosInvalidosException.class, DemasiadosIntentosException.class})
    public MfaCodigosRecuperacionResponse regenerarCodigos(UsuarioPrincipal actor, String codigo) {
        MfaUsuario mfa = mfaActivo(actor.getId());
        verificarBloqueo(mfa);
        if (!totpService.verificar(cifrador.descifrar(mfa.getSecretoCifrado()), codigo, Instant.now())) {
            registrarFallo(mfa);
            throw new DatosInvalidosException("El código de verificación no es válido");
        }
        reiniciarIntentos(mfa);
        return new MfaCodigosRecuperacionResponse(emitirCodigosRecuperacion(mfa.getUsuario()));
    }

    @Override
    @Transactional(noRollbackFor = {BadCredentialsException.class, DemasiadosIntentosException.class})
    public void validarSegundoPaso(UUID usuarioId, String codigo) {
        MfaUsuario mfa = mfaRepository
                .findByUsuarioId(usuarioId)
                .filter(MfaUsuario::isActivo)
                .orElseThrow(() -> new BadCredentialsException("Código de verificación inválido"));
        verificarBloqueo(mfa);
        if (!aceptarCodigo(mfa, codigo)) {
            throw new BadCredentialsException("Código de verificación inválido");
        }
    }

    private boolean aceptarCodigo(MfaUsuario mfa, String codigo) {
        boolean valido;
        String limpio = codigo == null ? "" : codigo.trim();
        if (limpio.matches("\\d{" + LONGITUD_TOTP + "}")) {
            valido = totpService.verificar(cifrador.descifrar(mfa.getSecretoCifrado()), limpio, Instant.now());
        } else {
            valido = consumirCodigoRecuperacion(mfa.getUsuario().getId(), limpio);
        }
        if (valido) {
            reiniciarIntentos(mfa);
        } else {
            registrarFallo(mfa);
        }
        return valido;
    }

    private boolean consumirCodigoRecuperacion(UUID usuarioId, String codigo) {
        String hash = hash(codigo);
        for (MfaCodigoRecuperacion candidato : codigoRepository.findByUsuarioIdAndUsadoFalse(usuarioId)) {
            if (MessageDigest.isEqual(
                    candidato.getCodigoHash().getBytes(StandardCharsets.US_ASCII),
                    hash.getBytes(StandardCharsets.US_ASCII))) {
                candidato.setUsado(true);
                return true;
            }
        }
        return false;
    }

    private List<String> emitirCodigosRecuperacion(Usuario usuario) {
        codigoRepository.eliminarDeUsuario(usuario.getId());
        List<String> visibles = new ArrayList<>();
        for (int i = 0; i < CODIGOS_RECUPERACION; i++) {
            String bruto = aleatorioRecuperacion(8);
            visibles.add(bruto.substring(0, 4) + "-" + bruto.substring(4));
            codigoRepository.save(MfaCodigoRecuperacion.builder()
                    .usuario(usuario)
                    .codigoHash(hash(bruto))
                    .usado(false)
                    .build());
        }
        return visibles;
    }

    private String aleatorioRecuperacion(int longitud) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < longitud; i++) {
            sb.append(ALFABETO_RECUPERACION.charAt(aleatorio.nextInt(ALFABETO_RECUPERACION.length())));
        }
        return sb.toString();
    }

    private static String hash(String codigo) {
        String normalizado = codigo.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256").digest(normalizado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void verificarBloqueo(MfaUsuario mfa) {
        if (mfa.getBloqueadoHasta() != null && mfa.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            eventoSeguridadService.registrar(
                    TipoEventoSeguridad.BLOQUEO_INTENTOS,
                    mfa.getUsuario().getId(),
                    null,
                    "Verificación bloqueada por intentos fallidos");
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Intente nuevamente en " + MINUTOS_BLOQUEO + " minutos");
        }
    }

    private void registrarFallo(MfaUsuario mfa) {
        int intentos = mfa.getIntentosFallidos() + 1;
        if (intentos >= MAX_INTENTOS) {
            mfa.setIntentosFallidos(0);
            mfa.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
            log.warn(
                    "Auditoría: MFA bloqueado temporalmente para el usuario {}",
                    mfa.getUsuario().getId());
        } else {
            mfa.setIntentosFallidos(intentos);
        }
    }

    private void reiniciarIntentos(MfaUsuario mfa) {
        mfa.setIntentosFallidos(0);
        mfa.setBloqueadoHasta(null);
    }

    private MfaUsuario mfaActivo(UUID usuarioId) {
        return mfaRepository
                .findByUsuarioId(usuarioId)
                .filter(MfaUsuario::isActivo)
                .orElseThrow(() -> new BusinessLogicException("La verificación en dos pasos no está activada"));
    }

    private Usuario cargarUsuario(UUID id) {
        return usuarioRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id " + id));
    }
}
