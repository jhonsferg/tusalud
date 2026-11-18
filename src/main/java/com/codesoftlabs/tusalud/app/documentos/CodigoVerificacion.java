package com.codesoftlabs.tusalud.app.documentos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class CodigoVerificacion {

    private CodigoVerificacion() {}

    public static String generar(TipoDocumento tipo, String referencia, UUID usuarioId, Instant instante) {
        String base = tipo.codigo() + "|" + referencia + "|" + usuarioId + "|" + instante.getEpochSecond();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash).toUpperCase(Locale.ROOT);
            return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible", ex);
        }
    }
}
