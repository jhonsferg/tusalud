package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Datos de una cama hospitalaria")
public record CamaRequest(
        @Schema(example = "H-201") @NotBlank @Size(max = 20) String codigo,
        @Schema(example = "HOSPITALIZACION") @NotNull AreaCama area,
        @Schema(example = "2") @Min(0) @Max(50) Integer piso,

        @Schema(example = "Habitación individual con baño") @Size(max = 200)
        String descripcion,

        @Schema(example = "180.00") @NotNull @DecimalMin("0.00")
        BigDecimal precioDia,

        @Schema(example = "true") Boolean activa) {}
