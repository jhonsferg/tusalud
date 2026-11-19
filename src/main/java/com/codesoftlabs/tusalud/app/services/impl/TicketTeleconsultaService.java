package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketTeleconsultaService {

    public record Datos(UUID teleconsultaId, UUID usuarioId, Instant expira) {}

    private static final int BYTES = 32;

    private final TelemedicinaProperties propiedades;
    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Datos> vigentes = new ConcurrentHashMap<>();

    public String emitir(UUID teleconsultaId, UUID usuarioId) {
        purgar();
        byte[] bytes = new byte[BYTES];
        azar.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        vigentes.put(
                ticket, new Datos(teleconsultaId, usuarioId, Instant.now().plusSeconds(propiedades.ticketTtlSeg())));
        return ticket;
    }

    public Optional<Datos> consumir(String ticket) {
        if (ticket == null || ticket.isBlank() || ticket.length() > 100) {
            return Optional.empty();
        }
        Datos datos = vigentes.remove(ticket);
        if (datos == null || datos.expira().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(datos);
    }

    private void purgar() {
        Instant ahora = Instant.now();
        vigentes.values().removeIf(datos -> datos.expira().isBefore(ahora));
    }
}
