package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Segundo paso del inicio de sesión con verificación en dos pasos")
public record MfaVerificarRequest(
        @Schema(description = "Token temporal devuelto por el inicio de sesión")
        @NotBlank(message = "El token de verificación es obligatorio")
        String mfaToken,

        @Schema(example = "123456", description = "Código TOTP o código de recuperación")
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 20, message = "El código no debe superar 20 caracteres")
        String codigo) {
    @Override
    public String toString() {
        return "MfaVerificarRequest[REDACTADO]";
    }
}
