package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.entities.RefreshToken;
import java.util.UUID;

public interface RefreshTokenService {

    String emitirParaSesion(UUID tokenIdSesion);

    RefreshToken buscarPorToken(String token);

    String rotar(RefreshToken actual);

    void revocarFamilia(UUID familia);

    void revocarDeSesion(UUID sesionId);

    void revocarDeUsuario(UUID usuarioId);

    void revocarDeUsuarioExceptoSesion(UUID usuarioId, UUID tokenIdActual);

    long getRefreshExpirationMs();
}
