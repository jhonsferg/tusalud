package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo opcional de la cancelación de una orden de imagen")
public record CancelarOrdenImagenRequest(
        @Schema(example = "Solicitada por error") @Size(max = 300)
        String motivo) {}
