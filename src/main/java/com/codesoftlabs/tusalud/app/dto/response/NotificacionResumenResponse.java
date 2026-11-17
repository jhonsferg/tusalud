package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidad de notificaciones no leídas")
public record NotificacionResumenResponse(
        @Schema(example = "3") long noLeidas) {}
