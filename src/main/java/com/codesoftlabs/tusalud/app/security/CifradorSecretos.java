package com.codesoftlabs.tusalud.app.security;

import com.codesoftlabs.tusalud.app.config.MfaProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CifradorSecretos {

    private static final int BYTES_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private final SecretKey clave;
    private final SecureRandom aleatorio = new SecureRandom();

    public CifradorSecretos(MfaProperties propiedades) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(propiedades.claveCifrado().getBytes(StandardCharsets.UTF_8));
            this.clave = new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo derivar la clave de cifrado", ex);
        }
    }

    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[BYTES_IV];
            aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder()
                    .encodeToString(ByteBuffer.allocate(iv.length + cifrado.length)
                            .put(iv)
                            .put(cifrado)
                            .array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo cifrar el secreto", ex);
        }
    }

    public String descifrar(String cifrado) {
        try {
            byte[] datos = Base64.getDecoder().decode(cifrado);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, datos, 0, BYTES_IV));
            return new String(cipher.doFinal(datos, BYTES_IV, datos.length - BYTES_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo descifrar el secreto", ex);
        }
    }
}
