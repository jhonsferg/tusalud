package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DispensacionItemRequest(
        Integer recetaItemId,
        @NotNull(message = "El producto es obligatorio") UUID productoId,

        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad) {}
