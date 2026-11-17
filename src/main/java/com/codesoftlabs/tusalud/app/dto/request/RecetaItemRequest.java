package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Medicamento de la receta")
public record RecetaItemRequest(
        @Schema(example = "Losartán 50 mg")
        @NotBlank(message = "El medicamento es obligatorio")
        @Size(max = 150, message = "El medicamento no debe superar 150 caracteres")
        String medicamento,

        @Schema(example = "1 tableta") @Size(max = 100, message = "La dosis no debe superar 100 caracteres")
        String dosis,

        @Schema(example = "Cada 24 horas") @Size(max = 100, message = "La frecuencia no debe superar 100 caracteres")
        String frecuencia,

        @Schema(example = "30 días") @Size(max = 100, message = "La duración no debe superar 100 caracteres")
        String duracion) {}
