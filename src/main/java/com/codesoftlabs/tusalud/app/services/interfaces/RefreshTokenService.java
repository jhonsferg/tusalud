package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.entities.RefreshToken;
import java.util.UUID;

public interface RefreshTokenService {

    String emitirParaSesion(UUID tokenIdSesion);

    RefreshToken buscarPorToken(String token);

    String rotar(RefreshToken actual);

    void revocarFamilia(UUID familia);

    void revocarDeSesion(Long sesionId);

    void revocarDeUsuario(Long usuarioId);

    void revocarDeUsuarioExceptoSesion(Long usuarioId, UUID tokenIdActual);

    long getRefreshExpirationMs();
}
