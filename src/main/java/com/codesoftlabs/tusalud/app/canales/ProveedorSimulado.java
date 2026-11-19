package com.codesoftlabs.tusalud.app.canales;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class ProveedorSimulado implements ProveedorMensajeria {

    private static final int CAPACIDAD = 200;
    private static final String SUFIJO_TRANSITORIO = "0000";
    private static final String SUFIJO_PERMANENTE = "9999";

    public record MensajeSimulado(
            String id, Canal canal, String destino, String asunto, String texto, LocalDateTime fecha) {}

    private final ConcurrentLinkedDeque<MensajeSimulado> bandeja = new ConcurrentLinkedDeque<>();

    @Override
    public String enviar(MensajeSaliente mensaje) {
        String destino = mensaje.destino();
        boolean verificacion = mensaje.claveIdempotencia() != null
                && mensaje.claveIdempotencia().startsWith("verificacion:");
        if (!verificacion && destino.endsWith(SUFIJO_TRANSITORIO)) {
            throw new EnvioException("El proveedor simulado rechazó temporalmente el envío", false);
        }
        if (!verificacion && destino.endsWith(SUFIJO_PERMANENTE)) {
            throw new EnvioException("El proveedor simulado rechazó el destino", true);
        }
        String id = "sim-" + UUID.randomUUID();
        bandeja.addFirst(new MensajeSimulado(
                id, mensaje.canal(), destino, mensaje.asunto(), mensaje.texto(), LocalDateTime.now()));
        while (bandeja.size() > CAPACIDAD) {
            bandeja.pollLast();
        }
        return id;
    }

    public List<MensajeSimulado> ultimos(int limite) {
        return new ArrayList<>(bandeja.stream().limit(limite).toList());
    }

    public void vaciar() {
        bandeja.clear();
    }
}
