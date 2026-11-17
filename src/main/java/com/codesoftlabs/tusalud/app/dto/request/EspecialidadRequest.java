package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Datos para crear o actualizar una especialidad")
public record EspecialidadRequest(
        @Schema(example = "Cardiología")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Schema(example = "Diagnóstico y tratamiento de enfermedades del corazón")
        @Size(max = 500, message = "La descripción no debe superar 500 caracteres")
        String descripcion,

        @Schema(example = "150.00")
        @NotNull(message = "El precio de la consulta es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio admite como máximo 8 enteros y 2 decimales")
        BigDecimal precioConsulta) {}
