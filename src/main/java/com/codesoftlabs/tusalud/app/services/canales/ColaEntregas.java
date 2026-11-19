package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ColaEntregas {

    private static final int LOTE = 50;

    private final EntregaNotificacionRepository entregas;
    private final EntregaProcesador procesador;
    private final CanalesProperties propiedades;

    @Scheduled(fixedDelayString = "${app.canales.intervalo-cola-ms:10000}", initialDelay = 15000)
    public void ejecutarProgramado() {
        if (!propiedades.workerHabilitado()) {
            return;
        }
        int procesadas = procesarPendientes(LocalDateTime.now());
        if (procesadas > 0) {
            log.debug("Entregas externas procesadas: {}", procesadas);
        }
    }

    public int procesarPendientes(LocalDateTime ahora) {
        List<UUID> ids = entregas.idsVencidos(EstadoEntrega.PENDIENTE, ahora, PageRequest.of(0, LOTE));
        int procesadas = 0;
        for (UUID id : ids) {
            try {
                procesador.procesar(id, ahora);
                procesadas++;
            } catch (RuntimeException ex) {
                log.warn("No se pudo procesar la entrega {}", id, ex);
            }
        }
        return procesadas;
    }
}
