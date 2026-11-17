package com.codesoftlabs.tusalud.app.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class TotpService {

    public static final String EMISOR = "TuSalud";

    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int BYTES_SECRETO = 20;
    private static final int DIGITOS = 6;
    private static final int PASO_SEGUNDOS = 30;
    private static final int VENTANA = 1;
    private static final int MODULO = 1_000_000;

    private final SecureRandom aleatorio = new SecureRandom();

    public String generarSecreto() {
        byte[] bytes = new byte[BYTES_SECRETO];
        aleatorio.nextBytes(bytes);
        return codificarBase32(bytes);
    }

    public String otpauthUri(String cuenta, String secreto) {
        String etiqueta = urlEncode(EMISOR + ":" + cuenta);
        return "otpauth://totp/" + etiqueta + "?secret=" + secreto + "&issuer=" + urlEncode(EMISOR)
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PASO_SEGUNDOS;
    }

    public String generarCodigo(String secreto, Instant instante) {
        return generarCodigo(secreto, instante.getEpochSecond() / PASO_SEGUNDOS);
    }

    public boolean verificar(String secreto, String codigo, Instant ahora) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return false;
        }
        long paso = ahora.getEpochSecond() / PASO_SEGUNDOS;
        byte[] esperado = codigo.getBytes(StandardCharsets.US_ASCII);
        boolean valido = false;
        for (long desplazamiento = -VENTANA; desplazamiento <= VENTANA; desplazamiento++) {
            byte[] candidato = generarCodigo(secreto, paso + desplazamiento).getBytes(StandardCharsets.US_ASCII);
            valido |= MessageDigest.isEqual(candidato, esperado);
        }
        return valido;
    }

    private String generarCodigo(String secreto, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodificarBase32(secreto), "HmacSHA1"));
            byte[] hash =
                    mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0F;
            int truncado = ((hash[desplazamiento] & 0x7F) << 24)
                    | ((hash[desplazamiento + 1] & 0xFF) << 16)
                    | ((hash[desplazamiento + 2] & 0xFF) << 8)
                    | (hash[desplazamiento + 3] & 0xFF);
            return String.format("%0" + DIGITOS + "d", truncado % MODULO);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular el código TOTP", ex);
        }
    }

    static String codificarBase32(byte[] datos) {
        StringBuilder resultado = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte dato : datos) {
            buffer = (buffer << 8) | (dato & 0xFF);
            bits += 8;
            while (bits >= 5) {
                resultado.append(ALFABETO_BASE32.charAt((buffer >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            resultado.append(ALFABETO_BASE32.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return resultado.toString();
    }

    static byte[] decodificarBase32(String texto) {
        String limpio = texto.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        byte[] salida = new byte[limpio.length() * 5 / 8];
        int buffer = 0;
        int bits = 0;
        int indice = 0;
        for (char caracter : limpio.toCharArray()) {
            int valor = ALFABETO_BASE32.indexOf(caracter);
            if (valor < 0) {
                throw new IllegalArgumentException("Secreto base32 inválido");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                salida[indice++] = (byte) ((buffer >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return salida;
    }

    private static String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
