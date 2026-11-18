package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Plantilla de documento con su versión vigente")
public record PlantillaResponse(
        String codigo,
        String nombre,
        int version,
        boolean personalizada,
        String actualizadaPor,
        LocalDateTime actualizadaEn,
        String motivo) {}
