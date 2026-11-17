package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Marca de una tarea como completada o pendiente")
public record CompletarEventoRequest(
        @Schema(example = "true") @NotNull(message = "El estado de la tarea es obligatorio")
        Boolean completado) {}
