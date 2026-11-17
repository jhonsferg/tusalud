package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AperturaCajaRequest(
        @NotNull(message = "El monto de apertura es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto de apertura no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal montoApertura) {}
