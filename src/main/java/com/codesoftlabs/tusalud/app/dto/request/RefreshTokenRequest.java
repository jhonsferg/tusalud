package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Datos para renovar el token de acceso")
public record RefreshTokenRequest(
        @Schema(description = "Refresh token entregado al iniciar sesión o en la renovación anterior")
        @NotBlank(message = "El refresh token es obligatorio")
        String refreshToken) {}
