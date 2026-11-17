package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Valoración de una cita atendida")
public record ValoracionRequest(
        @Schema(example = "5")
        @Min(value = 1, message = "La puntuación mínima es 1")
        @Max(value = 5, message = "La puntuación máxima es 5")
        int puntuacion,

        @Schema(example = "Muy buena atención y explicaciones claras")
        @Size(max = 500, message = "El comentario no debe superar 500 caracteres")
        String comentario) {}
