package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidad de citas agrupadas por estado (auditoría)")
public record CitasPorEstadoResponse(
        @Schema(example = "PENDIENTE") EstadoCita estado,

        @Schema(example = "12") Long cantidad) {}
