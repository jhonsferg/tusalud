package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Evaluación de triaje de una emergencia")
public record TriajeRequest(
        @Schema(example = "2", description = "1 es el más grave y 5 el menos urgente") @NotNull @Min(1) @Max(5)
        Integer nivel,

        @Schema(example = "150/95") @Size(max = 20) String presionArterial,
        @Schema(example = "108") @Min(20) @Max(300) Integer frecuenciaCardiaca,
        @Schema(example = "22") @Min(4) @Max(80) Integer frecuenciaRespiratoria,

        @Schema(example = "37.2") @DecimalMin("30.0") @DecimalMax("45.0")
        BigDecimal temperatura,

        @Schema(example = "94") @Min(0) @Max(100) Integer saturacionO2,
        @Schema(example = "15") @Min(3) @Max(15) Integer glasgow,

        @Schema(example = "Dolor torácico opresivo de inicio súbito") @NotBlank @Size(max = 300)
        String motivoConsulta) {}
