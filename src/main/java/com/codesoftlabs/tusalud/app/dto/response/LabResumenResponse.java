package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores de la cola del laboratorio")
public record LabResumenResponse(
        @Schema(example = "4") long pendientes,
        @Schema(example = "3") long enProceso,
        @Schema(example = "6") long completadasHoy,
        @Schema(example = "1") long urgentes) {}
