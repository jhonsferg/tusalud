package com.codesoftlabs.tusalud.app.services.events;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;

public record NotificacionCreadaEvent(Long usuarioId, NotificacionResponse notificacion) {}
