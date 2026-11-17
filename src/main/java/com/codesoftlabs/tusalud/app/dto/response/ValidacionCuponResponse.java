package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resultado de validar un cupón sobre un monto")
public record ValidacionCuponResponse(
        @Schema(example = "SALUD20") String codigo,
        @Schema(example = "true") boolean valido,
        @Schema(example = "Cupón aplicado correctamente") String mensaje,
        @Schema(example = "30.00") BigDecimal descuento,
        @Schema(example = "120.00") BigDecimal total) {}
