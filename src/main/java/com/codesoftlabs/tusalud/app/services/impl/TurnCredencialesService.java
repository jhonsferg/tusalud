package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse.IceServer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TurnCredencialesService {

    private static final String ALGORITMO = "HmacSHA1";

    private final TelemedicinaProperties propiedades;

    public IceServersResponse generar(UUID usuarioId) {
        TelemedicinaProperties.Turn turn = propiedades.turn();
        long expira = Instant.now().getEpochSecond() + turn.ttlSeg();
        List<IceServer> servidores = new ArrayList<>();
        if (turn.habilitado()) {
            String base = turn.host() + ":" + turn.puerto();
            String usuario = expira + ":" + usuarioId;
            servidores.add(new IceServer(List.of("stun:" + base), null, null));
            servidores.add(new IceServer(
                    List.of("turn:" + base + "?transport=udp", "turn:" + base + "?transport=tcp"),
                    usuario,
                    firmar(turn.secreto(), usuario)));
        }
        return new IceServersResponse(servidores, expira);
    }

    public static String firmar(String secreto, String usuario) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return Base64.getEncoder().encodeToString(mac.doFinal(usuario.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar la credencial TURN", e);
        }
    }
}
