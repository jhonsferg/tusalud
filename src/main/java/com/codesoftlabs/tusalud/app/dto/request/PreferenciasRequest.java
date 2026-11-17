package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Preferencias de notificaciones y recordatorios")
public record PreferenciasRequest(
        @Schema(example = "true") @NotNull(message = "Indique si los recordatorios están activos")
        Boolean recordatoriosActivos,

        @Schema(example = "24", description = "Horas de antelación para el recordatorio (1 a 72)")
        @NotNull(message = "Indique las horas de antelación del recordatorio")
        @Min(value = 1, message = "La antelación mínima del recordatorio es 1 hora")
        @Max(value = 72, message = "La antelación máxima del recordatorio es 72 horas")
        Integer horasAntelacionRecordatorio,

        @Schema(example = "true") @NotNull(message = "Indique si desea alertas de inicio de sesión")
        Boolean alertasInicioSesion) {}
