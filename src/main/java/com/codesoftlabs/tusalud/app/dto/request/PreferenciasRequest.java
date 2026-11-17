package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Preferencias de notificaciones y recordatorios")
public record PreferenciasRequest(
        @Schema(example = "true") @NotNull(message = "Indique si los recordatorios están activos")
        Boolean recordatoriosActivos,

        @Schema(example = "1440", description = "Minutos de antelación para el recordatorio (15 a 4320)")
        @NotNull(message = "Indique los minutos de antelación del recordatorio")
        @Min(value = 15, message = "La antelación mínima del recordatorio es 15 minutos")
        @Max(value = 4320, message = "La antelación máxima del recordatorio es 4320 minutos")
        Integer minutosAntelacionRecordatorio,

        @Schema(example = "true") @NotNull(message = "Indique si desea alertas de inicio de sesión")
        Boolean alertasInicioSesion,

        @Schema(example = "true", description = "Si se omite, no cambia")
        Boolean tourCompletado) {}
