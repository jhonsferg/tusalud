package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo opcional de la cancelación de una orden")
public record CancelarOrdenLabRequest(
        @Schema(example = "Solicitada por error") @Size(max = 300)
        String motivo) {}
