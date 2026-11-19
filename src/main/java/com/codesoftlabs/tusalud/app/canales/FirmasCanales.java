package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class FirmasCanales {

    private static final String ALGORITMO = "HmacSHA256";

    public record TokenBaja(UUID usuarioId, Canal canal) {}

    private FirmasCanales() {}

    public static String hmacHex(String secreto, byte[] datos) {
        return HexFormat.of().formatHex(hmac(secreto, datos));
    }

    public static boolean iguales(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String crearTokenBaja(String secreto, UUID usuarioId, Canal canal) {
        String carga = usuarioId + ":" + canal.name();
        Base64.Encoder codificador = Base64.getUrlEncoder().withoutPadding();
        return codificador.encodeToString(carga.getBytes(StandardCharsets.UTF_8))
                + "."
                + codificador.encodeToString(hmac(secreto, carga.getBytes(StandardCharsets.UTF_8)));
    }

    public static Optional<TokenBaja> leerTokenBaja(String secreto, String token) {
        if (token == null || token.length() > 300) {
            return Optional.empty();
        }
        String[] partes = token.split("\\.");
        if (partes.length != 2) {
            return Optional.empty();
        }
        try {
            byte[] cargaBytes = Base64.getUrlDecoder().decode(partes[0]);
            byte[] firma = Base64.getUrlDecoder().decode(partes[1]);
            if (!MessageDigest.isEqual(firma, hmac(secreto, cargaBytes))) {
                return Optional.empty();
            }
            String[] carga = new String(cargaBytes, StandardCharsets.UTF_8).split(":");
            if (carga.length != 2) {
                return Optional.empty();
            }
            return Optional.of(new TokenBaja(UUID.fromString(carga[0]), Canal.valueOf(carga[1])));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static byte[] hmac(String secreto, byte[] datos) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(datos);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma", ex);
        }
    }
}
