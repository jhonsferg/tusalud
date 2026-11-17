package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjusteRequest(
        @NotNull(message = "El lote es obligatorio") Long loteId,

        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
        String observaciones) {}
