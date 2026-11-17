package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IngresoItemRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @NotBlank(message = "El número de lote es obligatorio")
        @Size(max = 40, message = "El número de lote no puede superar 40 caracteres")
        String numeroLote,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        @Future(message = "La fecha de vencimiento debe ser futura")
        LocalDate fechaVencimiento,

        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @NotNull(message = "El costo unitario es obligatorio")
        @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        @Size(max = 60, message = "La ubicación no puede superar 60 caracteres")
        String ubicacion) {}
