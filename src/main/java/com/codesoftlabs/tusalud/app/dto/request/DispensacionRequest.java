package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record DispensacionRequest(
        UUID atencionId,
        UUID pacienteId,

        @NotEmpty(message = "Debe indicar al menos un producto") @Valid
        List<DispensacionItemRequest> items,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPagoFarmacia metodoPago,

        @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
        String observaciones) {}
