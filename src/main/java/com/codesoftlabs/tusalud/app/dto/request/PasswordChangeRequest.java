package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Cambio de contraseña del usuario autenticado")
public record PasswordChangeRequest(
        @Schema(example = "Paciente123!") @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @Schema(example = "NuevaClave456!")
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String passwordNueva) {}
