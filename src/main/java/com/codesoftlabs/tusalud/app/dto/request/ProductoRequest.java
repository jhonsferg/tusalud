package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Datos de un producto del almacén")
public record ProductoRequest(
        @Schema(example = "MED-0001")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede superar 30 caracteres")
        String codigo,

        @Schema(example = "Paracetamol 500 mg")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El tipo es obligatorio") TipoProducto tipo,

        @Size(max = 150, message = "El principio activo no puede superar 150 caracteres")
        String principioActivo,

        @Size(max = 100, message = "La presentación no puede superar 100 caracteres")
        String presentacion,

        @Schema(example = "tableta")
        @NotBlank(message = "La unidad es obligatoria")
        @Size(max = 30, message = "La unidad no puede superar 30 caracteres")
        String unidad,

        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        BigDecimal precioVenta,

        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo,

        Boolean requiereReceta,
        Boolean activo) {}
