package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
@Schema(description = "Token JWT y datos básicos del usuario autenticado")
public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzUxMiJ9...") String token,

        @Schema(description = "Token opaco para renovar el acceso sin credenciales; se rota en cada renovación")
        String refreshToken,

        @Schema(description = "Vigencia del refresh token en milisegundos", example = "604800000")
        long refreshExpiresIn,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Vigencia del token en milisegundos", example = "900000")
        long expiresIn,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID usuarioId,

        @Schema(example = "admin@tusalud.com") String email,

        @Schema(example = "Administrador General") String nombreCompleto,

        @Schema(example = "[\"ROLE_ADMIN\"]") List<String> roles,

        @Schema(description = "Si es true, falta el segundo paso en /auth/mfa/verificar", example = "false")
        boolean mfaRequerido,

        @Schema(description = "Token temporal (5 min) para el segundo paso; solo si mfaRequerido")
        String mfaToken) {
    @Override
    public String toString() {
        return "AuthResponse[REDACTADO]";
    }
}
