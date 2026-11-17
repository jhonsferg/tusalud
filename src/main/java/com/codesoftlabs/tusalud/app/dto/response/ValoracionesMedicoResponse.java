package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resumen de valoraciones de un médico")
public record ValoracionesMedicoResponse(
        @Schema(example = "4.6") double promedio,
        @Schema(example = "12") int total,
        List<ValoracionResponse> valoraciones) {}
