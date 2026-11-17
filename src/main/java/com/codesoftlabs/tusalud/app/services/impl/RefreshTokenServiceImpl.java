package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.entities.RefreshToken;
import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import com.codesoftlabs.tusalud.app.repositories.RefreshTokenRepository;
import com.codesoftlabs.tusalud.app.repositories.SesionUsuarioRepository;
import com.codesoftlabs.tusalud.app.security.JwtUtils;
import com.codesoftlabs.tusalud.app.services.interfaces.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int BYTES_TOKEN = 32;
    private static final Duration RETENCION_EXPIRADOS = Duration.ofDays(1);

    private final RefreshTokenRepository repository;
    private final SesionUsuarioRepository sesionRepository;
    private final JwtUtils jwtUtils;
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    @Transactional
    public String emitirParaSesion(UUID tokenIdSesion) {
        SesionUsuario sesion = sesionRepository
                .findByTokenId(tokenIdSesion)
                .orElseThrow(() -> new IllegalStateException("Sesión inexistente: " + tokenIdSesion));
        return guardar(sesion, UUID.randomUUID());
    }

    @Override
    public RefreshToken buscarPorToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return repository.findByTokenHash(hash(token.trim())).orElse(null);
    }

    @Override
    @Transactional
    public String rotar(RefreshToken actual) {
        actual.setUsado(true);
        return guardar(actual.getSesion(), actual.getFamilia());
    }

    @Override
    @Transactional
    public void revocarFamilia(UUID familia) {
        repository.revocarFamilia(familia);
    }

    @Override
    @Transactional
    public void revocarDeSesion(Long sesionId) {
        repository.revocarDeSesion(sesionId);
    }

    @Override
    @Transactional
    public void revocarDeUsuario(Long usuarioId) {
        repository.revocarDeUsuario(usuarioId);
    }

    @Override
    @Transactional
    public void revocarDeUsuarioExceptoSesion(Long usuarioId, UUID tokenIdActual) {
        if (tokenIdActual == null) {
            repository.revocarDeUsuario(usuarioId);
        } else {
            repository.revocarDeUsuarioExcepto(usuarioId, tokenIdActual);
        }
    }

    @Override
    public long getRefreshExpirationMs() {
        return jwtUtils.getRefreshExpirationMs();
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS, initialDelay = 5)
    @Transactional
    public void purgarExpirados() {
        int eliminados = repository.eliminarExpirados(LocalDateTime.now().minus(RETENCION_EXPIRADOS));
        log.debug("Refresh tokens expirados eliminados: {}", eliminados);
    }

    private String guardar(SesionUsuario sesion, UUID familia) {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.builder()
                .usuario(sesion.getUsuario())
                .sesion(sesion)
                .familia(familia)
                .tokenHash(hash(token))
                .expiraEn(LocalDateTime.now().plus(Duration.ofMillis(jwtUtils.getRefreshExpirationMs())))
                .build());
        return token;
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
