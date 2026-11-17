package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record IngresoRequest(
        Long proveedorId,

        @Size(max = 40, message = "El documento no puede superar 40 caracteres")
        String documento,

        @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
        String observaciones,

        @NotEmpty(message = "Debe registrar al menos un producto") @Valid
        List<IngresoItemRequest> items) {}
