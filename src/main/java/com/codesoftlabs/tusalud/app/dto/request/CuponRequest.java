package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos de un cupón de descuento")
public record CuponRequest(
        @Schema(example = "BIENVENIDA10")
        @NotBlank(message = "El código del cupón es obligatorio")
        @Size(min = 3, max = 30, message = "El código debe tener entre 3 y 30 caracteres")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "El código solo puede contener letras, números, guiones y guiones bajos")
        String codigo,

        @Schema(example = "10% de descuento para nuevos pacientes")
        @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
        String descripcion,

        @Schema(example = "PORCENTAJE") @NotNull(message = "El tipo de cupón es obligatorio")
        TipoCupon tipo,

        @Schema(example = "10.00", description = "Porcentaje (1 a 100) o monto fijo según el tipo")
        @NotNull(message = "El valor del cupón es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor del cupón debe ser mayor a cero")
        BigDecimal valor,

        @Schema(type = "string", example = "2026-01-01") @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @Schema(type = "string", example = "2026-12-31") @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        @Schema(example = "100", description = "Usos máximos; vacío para ilimitado")
        @Positive(message = "Los usos máximos deben ser un número positivo")
        Integer usosMaximos,

        @Schema(example = "true", description = "Por defecto true")
        Boolean activo) {}
