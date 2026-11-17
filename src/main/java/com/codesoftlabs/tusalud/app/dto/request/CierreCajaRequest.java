package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CierreCajaRequest(
        @NotNull(message = "El monto contado es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto contado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal montoContado,

        @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
        String observaciones) {}
