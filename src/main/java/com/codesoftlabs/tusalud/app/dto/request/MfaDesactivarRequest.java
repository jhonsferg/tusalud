package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirmación para desactivar la verificación en dos pasos")
public record MfaDesactivarRequest(
        @Schema(example = "Paciente123!") @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @Schema(example = "123456")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no debe superar 20 caracteres")
        String codigo) {
    @Override
    public String toString() {
        return "MfaDesactivarRequest[REDACTADO]";
    }
}
