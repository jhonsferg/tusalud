package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Sesión activa del usuario autenticado")
public record SesionResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "Chrome en Windows") String dispositivo,

        @Schema(example = "190.12.34.56") String ip,

        @Schema(type = "string", example = "2026-10-12T21:00:00")
        LocalDateTime creadaEn,

        @Schema(type = "string", example = "2026-10-12T22:30:00")
        LocalDateTime ultimoAcceso,

        @Schema(type = "string", example = "2026-10-13T21:00:00")
        LocalDateTime expiraEn,

        @Schema(example = "true", description = "Indica si es la sesión que realizó la petición")
        boolean actual) {

    public static SesionResponse from(SesionUsuario sesion, UUID tokenActual) {
        return new SesionResponse(
                sesion.getId(),
                sesion.getDispositivo(),
                sesion.getIp(),
                sesion.getCreadaEn(),
                sesion.getUltimoAcceso(),
                sesion.getExpiraEn(),
                sesion.getTokenId().equals(tokenActual));
    }
}
