package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.PlantillaDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Versión del historial de una plantilla")
public record PlantillaVersionResponse(
        int version, boolean activa, boolean restauracion, String motivo, String creadaPor, LocalDateTime creadaEn) {

    public static PlantillaVersionResponse from(PlantillaDocumento plantilla) {
        return new PlantillaVersionResponse(
                plantilla.getVersion(),
                plantilla.isActiva(),
                plantilla.isRestauracion(),
                plantilla.getMotivo(),
                plantilla.getCreadoPor().getNombreCompleto(),
                plantilla.getCreatedAt());
    }
}
