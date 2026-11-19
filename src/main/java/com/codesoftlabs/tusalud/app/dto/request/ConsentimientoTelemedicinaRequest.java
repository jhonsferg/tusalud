package com.codesoftlabs.tusalud.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsentimientoTelemedicinaRequest(
        @NotBlank(message = "La versión del consentimiento es obligatoria")
        @Size(max = 20, message = "La versión no debe superar 20 caracteres")
        String version) {}
