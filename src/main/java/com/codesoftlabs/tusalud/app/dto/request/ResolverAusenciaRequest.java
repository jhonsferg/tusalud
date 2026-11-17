package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo de la resolución de una ausencia")
public record ResolverAusenciaRequest(
        @Schema(example = "Cobertura insuficiente ese día")
        @Size(max = 300, message = "El motivo no debe superar 300 caracteres")
        String motivo) {}
