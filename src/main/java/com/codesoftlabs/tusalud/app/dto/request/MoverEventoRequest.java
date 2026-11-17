package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Nuevo horario de un evento, para arrastrarlo o redimensionarlo")
public record MoverEventoRequest(
        @Schema(type = "string", example = "2026-11-20T10:00:00") @NotNull(message = "El inicio es obligatorio")
        LocalDateTime inicio,

        @Schema(type = "string", example = "2026-11-20T11:00:00") @NotNull(message = "El fin es obligatorio")
        LocalDateTime fin,

        @Schema(example = "false") Boolean todoElDia) {}
