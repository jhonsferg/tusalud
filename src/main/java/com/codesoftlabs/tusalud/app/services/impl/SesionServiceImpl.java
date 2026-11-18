package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.SesionResponse;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaUsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.DispositivoParser;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RefreshTokenService;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SesionServiceImpl implements SesionService {

    private static final Duration FRECUENCIA_ACTUALIZACION = Duration.ofMinutes(1);

    private final SesionUsuarioRepository sesionRepository;
    private final PreferenciaUsuarioRepository preferenciaRepository;
    private final NotificacionService notificacionService;
    private final EventoSeguridadService eventoSeguridadService;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UUID iniciarSesion(Usuario usuario, ContextoConexion contexto) {
        boolean tieneSesionesPrevias = sesionRepository.existsByUsuarioId(usuario.getId());
        LocalDateTime ahora = LocalDateTime.now();
        String dispositivo = DispositivoParser.describir(contexto.userAgent());
        UUID tokenId = UUID.randomUUID();
        boolean conocido =
                sesionRepository.existsByUsuarioIdAndDispositivoAndIp(usuario.getId(), dispositivo, contexto.ip());

        sesionRepository.save(SesionUsuario.builder()
                .usuario(usuario)
                .tokenId(tokenId)
                .ip(contexto.ip())
                .userAgent(contexto.userAgent())
                .dispositivo(dispositivo)
                .creadaEn(ahora)
                .ultimoAcceso(ahora)
                .expiraEn(ahora.plus(Duration.ofMillis(jwtUtils.getRefreshExpirationMs())))
                .revocada(false)
                .build());

        if (tieneSesionesPrevias && !conocido) {
            eventoSeguridadService.registrar(
                    TipoEventoSeguridad.DISPOSITIVO_NUEVO,
                    usuario.getId(),
                    null,
                    "Inicio de sesión desde un dispositivo o una dirección no usados antes",
                    contexto);
        }
        if (tieneSesionesPrevias && alertasActivas(usuario.getId())) {
            notificacionService.crear(
                    usuario,
                    TipoNotificacion.INICIO_SESION,
                    "Nuevo inicio de sesión",
                    "Se inició sesión desde " + dispositivo + " con la IP "
                            + (contexto.ip() == null ? "desconocida" : contexto.ip()) + ".",
                    "/perfil");
        }
        log.info("Sesión iniciada para usuario {} desde {}", usuario.getId(), dispositivo);
        return tokenId;
    }

    @Override
    @Transactional
    public boolean sesionActiva(UUID tokenId) {
        LocalDateTime ahora = LocalDateTime.now();
        return sesionRepository
                .findByTokenId(tokenId)
                .filter(sesion -> sesion.estaActiva(ahora))
                .map(sesion -> {
                    if (sesion.getUltimoAcceso().isBefore(ahora.minus(FRECUENCIA_ACTUALIZACION))) {
                        sesion.setUltimoAcceso(ahora);
                    }
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<SesionResponse> listarActivas(UsuarioPrincipal actor, UUID tokenActual) {
        return sesionRepository
                .findByUsuarioIdAndRevocadaFalseAndExpiraEnAfterOrderByUltimoAccesoDesc(
                        actor.getId(), LocalDateTime.now())
                .stream()
                .map(sesion -> SesionResponse.from(sesion, tokenActual))
                .toList();
    }

    @Override
    @Transactional
    public void revocar(UsuarioPrincipal actor, UUID sesionId) {
        SesionUsuario sesion = sesionRepository
                .findByIdAndUsuarioId(sesionId, actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión", "id", sesionId));
        sesion.setRevocada(true);
        refreshTokenService.revocarDeSesion(sesion.getId());
    }

    @Override
    @Transactional
    public void revocarTodasExceptoActual(UsuarioPrincipal actor, UUID tokenActual) {
        if (tokenActual == null) {
            sesionRepository.revocarTodasDelUsuario(actor.getId());
            refreshTokenService.revocarDeUsuario(actor.getId());
        } else {
            sesionRepository.revocarTodasExcepto(actor.getId(), tokenActual);
            refreshTokenService.revocarDeUsuarioExceptoSesion(actor.getId(), tokenActual);
        }
    }

    @Override
    @Transactional
    public void cerrarSesion(UUID tokenId) {
        if (tokenId != null) {
            sesionRepository.findByTokenId(tokenId).ifPresent(sesion -> {
                sesion.setRevocada(true);
                refreshTokenService.revocarDeSesion(sesion.getId());
            });
        }
    }

    @Override
    @Transactional
    public void revocarTodasDelUsuario(UUID usuarioId) {
        sesionRepository.revocarTodasDelUsuario(usuarioId);
        refreshTokenService.revocarDeUsuario(usuarioId);
    }

    private boolean alertasActivas(UUID usuarioId) {
        return preferenciaRepository
                .findByUsuarioId(usuarioId)
                .map(preferencia -> preferencia.isAlertasInicioSesion())
                .orElse(true);
    }
}
