package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Motivo obligatorio de la acción")
public record CancelarOrdenImagenRequest(
        @Schema(example = "Solicitado por error")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
        String motivo) {}
