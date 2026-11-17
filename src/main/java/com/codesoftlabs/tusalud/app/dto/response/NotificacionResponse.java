package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Notificacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Notificación dirigida al usuario autenticado")
public record NotificacionResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "CITA_CREADA") TipoNotificacion tipo,

        @Schema(example = "Nueva cita registrada") String titulo,

        @Schema(example = "Su cita fue registrada y está pendiente de confirmación.")
        String mensaje,

        @Schema(example = "/citas/12") String enlace,

        @Schema(example = "false") boolean leida,

        @Schema(type = "string", example = "2026-10-12T21:00:00")
        LocalDateTime createdAt) {

    public static NotificacionResponse from(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getEnlace(),
                notificacion.isLeida(),
                notificacion.getCreatedAt());
    }
}
