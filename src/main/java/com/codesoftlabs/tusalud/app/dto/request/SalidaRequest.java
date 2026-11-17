package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.MotivoSalida;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SalidaRequest(
        @NotNull(message = "El producto es obligatorio") UUID productoId,

        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        @NotNull(message = "El motivo es obligatorio") MotivoSalida motivo,

        @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
        String observaciones) {}
