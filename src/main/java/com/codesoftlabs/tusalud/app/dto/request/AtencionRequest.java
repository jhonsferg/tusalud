package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Registro de la atención médica de una cita")
public record AtencionRequest(
        @Schema(example = "Hipertensión arterial leve")
        @NotBlank(message = "El diagnóstico es obligatorio")
        @Size(max = 1000, message = "El diagnóstico no debe superar 1000 caracteres")
        String diagnostico,

        @Schema(example = "Control con dieta baja en sal")
        @Size(max = 1000, message = "El tratamiento no debe superar 1000 caracteres")
        String tratamiento,

        @Schema(example = "Reposo relativo y control en 30 días")
        @Size(max = 1000, message = "Las indicaciones no deben superar 1000 caracteres")
        String indicaciones,

        @Schema(example = "Paciente refiere mejoría")
        @Size(max = 1000, message = "Las observaciones no deben superar 1000 caracteres")
        String observaciones,

        @Schema(example = "72.5")
        @DecimalMin(value = "0.5", message = "El peso no es válido")
        @DecimalMax(value = "500", message = "El peso no es válido")
        BigDecimal pesoKg,

        @Schema(example = "170.0")
        @DecimalMin(value = "30", message = "La talla no es válida")
        @DecimalMax(value = "250", message = "La talla no es válida")
        BigDecimal tallaCm,

        @Schema(example = "120/80")
        @Pattern(regexp = "^\\d{2,3}/\\d{2,3}$", message = "La presión arterial debe tener el formato 120/80")
        String presionArterial,

        @Schema(example = "36.5")
        @DecimalMin(value = "30", message = "La temperatura no es válida")
        @DecimalMax(value = "45", message = "La temperatura no es válida")
        BigDecimal temperaturaC,

        @Schema(example = "72")
        @Min(value = 20, message = "La frecuencia cardíaca no es válida")
        @Max(value = 250, message = "La frecuencia cardíaca no es válida")
        Integer frecuenciaCardiaca,

        @Valid List<RecetaItemRequest> receta) {}
