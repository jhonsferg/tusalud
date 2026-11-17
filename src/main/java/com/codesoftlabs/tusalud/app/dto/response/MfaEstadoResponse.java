package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado de la verificación en dos pasos")
public record MfaEstadoResponse(
        @Schema(example = "true") boolean activo,

        @Schema(example = "8") long codigosRecuperacionRestantes) {}
