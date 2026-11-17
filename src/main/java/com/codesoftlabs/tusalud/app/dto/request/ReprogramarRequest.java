package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "Nueva fecha y hora para una cita existente")
public record ReprogramarRequest(
        @Schema(type = "string", example = "2026-10-20T10:30:00", description = "ISO-8601 sin zona horaria")
        @NotNull(message = "La fecha y hora es obligatoria")
        @Future(message = "La fecha y hora de la cita debe ser futura")
        LocalDateTime fechaHora) {}
