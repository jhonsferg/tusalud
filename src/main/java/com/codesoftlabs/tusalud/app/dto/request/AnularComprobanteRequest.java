package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularComprobanteRequest(
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
        String motivo) {}
