package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.SesionCaja;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SesionCajaResponse(
        UUID id,
        UUID cajeroId,
        String cajeroNombre,
        LocalDateTime apertura,
        BigDecimal montoApertura,
        LocalDateTime cierre,
        BigDecimal montoContado,
        BigDecimal montoEsperado,
        BigDecimal diferencia,
        EstadoCaja estado,
        String observaciones) {

    public static SesionCajaResponse from(SesionCaja sesion) {
        return new SesionCajaResponse(
                sesion.getId(),
                sesion.getCajero().getId(),
                sesion.getCajero().getNombreCompleto(),
                sesion.getApertura(),
                sesion.getMontoApertura(),
                sesion.getCierre(),
                sesion.getMontoContado(),
                sesion.getMontoEsperado(),
                sesion.getDiferencia(),
                sesion.getEstado(),
                sesion.getObservaciones());
    }
}
