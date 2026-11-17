package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Nota de evolución médica o de enfermería")
public record EvolucionHospRequest(
        @Schema(example = "MEDICA") @NotNull TipoEvolucion tipo,

        @Schema(example = "Paciente estable, sin disnea en reposo") @NotBlank @Size(max = 1000)
        String nota,

        @Schema(example = "120/80") @Size(max = 20) String presionArterial,
        @Schema(example = "78") @Min(20) @Max(300) Integer frecuenciaCardiaca,

        @Schema(example = "36.8") @DecimalMin("30.0") @DecimalMax("45.0")
        BigDecimal temperatura,

        @Schema(example = "97") @Min(0) @Max(100) Integer saturacionO2) {}
