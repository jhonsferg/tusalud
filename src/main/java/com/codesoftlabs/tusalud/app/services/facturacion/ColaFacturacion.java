package com.codesoftlabs.tusalud.app.services.facturacion;

import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ColaFacturacion {

    private static final int LOTE = 25;

    private final FacturacionService facturacion;
    private final ConfiguracionFiscalService configuracion;

    @Value("${app.facturacion.cola-habilitada:true}")
    private boolean habilitada;

    @Scheduled(fixedDelayString = "${app.facturacion.cola.intervalo-ms:10000}", initialDelay = 20000)
    public void procesar() {
        if (!habilitada || configuracion.vigente().isEmpty()) {
            return;
        }
        procesarAhora();
    }

    public int procesarAhora() {
        int procesados = 0;
        for (UUID id : facturacion.pendientesDeEnvio(LOTE)) {
            try {
                facturacion.enviar(id);
                procesados++;
            } catch (RuntimeException ex) {
                log.warn("Falló el envío del comprobante {}", id, ex);
            }
        }
        for (UUID id : facturacion.resumenesPendientes(LOTE)) {
            try {
                facturacion.procesarResumen(id);
                procesados++;
            } catch (RuntimeException ex) {
                log.warn("Falló el proceso del resumen {}", id, ex);
            }
        }
        return procesados;
    }

    @Scheduled(cron = "${app.facturacion.cron-resumen:0 30 0 * * *}")
    public void resumenDelDiaAnterior() {
        if (!habilitada) {
            return;
        }
        var vigente = configuracion.vigente();
        if (vigente.isEmpty() || !vigente.get().isBoletasPorResumen()) {
            return;
        }
        LocalDate ayer = LocalDate.now().minusDays(1);
        try {
            facturacion.generarResumenDiario(null, ayer);
        } catch (RuntimeException ex) {
            log.info("Sin resumen diario para {}: {}", ayer, ex.getMessage());
        }
    }
}
