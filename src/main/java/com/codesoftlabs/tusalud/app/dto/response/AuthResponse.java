package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "Token JWT y datos básicos del usuario autenticado")
public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzUxMiJ9...") String token,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Vigencia del token en milisegundos", example = "86400000")
        long expiresIn,

        @Schema(example = "1") Long usuarioId,

        @Schema(example = "admin@tusalud.com") String email,

        @Schema(example = "Administrador General") String nombreCompleto,

        @Schema(example = "[\"ROLE_ADMIN\"]") List<String> roles) {}
