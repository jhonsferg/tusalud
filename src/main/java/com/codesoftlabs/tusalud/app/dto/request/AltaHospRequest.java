package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos del alta hospitalaria")
public record AltaHospRequest(
        @Schema(example = "Insuficiencia cardiaca compensada") @NotBlank @Size(max = 300)
        String diagnosticoAlta,

        @Schema(example = "Control en 7 días, dieta hiposódica") @NotBlank @Size(max = 500)
        String indicacionesAlta,

        @Schema(example = "I50.9", description = "Código CIE-10 del diagnóstico de alta") @Size(max = 8)
        String cie10Alta) {}
