package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Ingresos cobrados en un mes")
public record IngresoMesResponse(
        @Schema(example = "2026-10") String mes,
        @Schema(example = "1250.00") BigDecimal total) {}
