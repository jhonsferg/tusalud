package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Informe del estudio, o su corrección con motivo")
public record InformeImagenRequest(
        @NotBlank(message = "Los hallazgos son obligatorios") @Size(max = 4000)
        String hallazgos,

        @NotBlank(message = "La conclusión es obligatoria") @Size(max = 2000)
        String conclusion,

        boolean hallazgoCritico,

        @Schema(example = "CMP-45210")
        @NotBlank(message = "La colegiatura es obligatoria")
        @Size(min = 3, max = 30, message = "La colegiatura debe tener entre 3 y 30 caracteres")
        String firmaColegiatura,

        @Schema(description = "Obligatorio al corregir un informe ya emitido") @Size(max = 300)
        String motivoCorreccion) {}
