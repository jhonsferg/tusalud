package com.codesoftlabs.tusalud.app.services.events;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import java.util.UUID;

public record NotificacionCreadaEvent(UUID usuarioId, NotificacionResponse notificacion) {}
