package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.EvolucionHospitalaria;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Nota de evolución de un ingreso")
public record EvolucionHospResponse(
        UUID id,
        @Schema(example = "MEDICA") TipoEvolucion tipo,
        String nota,
        String presionArterial,
        Integer frecuenciaCardiaca,
        BigDecimal temperatura,
        Integer saturacionO2,
        String autorNombre,
        LocalDateTime fecha) {

    private static final int LIMITE_RESUMEN = 200;

    public static EvolucionHospResponse from(EvolucionHospitalaria evolucion, boolean resumida) {
        String nota = evolucion.getNota();
        if (resumida && nota.length() > LIMITE_RESUMEN) {
            nota = nota.substring(0, LIMITE_RESUMEN).trim() + "…";
        }
        return new EvolucionHospResponse(
                evolucion.getId(),
                evolucion.getTipo(),
                nota,
                resumida ? null : evolucion.getPresionArterial(),
                resumida ? null : evolucion.getFrecuenciaCardiaca(),
                resumida ? null : evolucion.getTemperatura(),
                resumida ? null : evolucion.getSaturacionO2(),
                evolucion.getAutor().getNombreCompleto(),
                evolucion.getFecha());
    }
}
