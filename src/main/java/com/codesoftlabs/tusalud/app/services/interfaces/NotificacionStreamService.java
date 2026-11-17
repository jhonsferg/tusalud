package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificacionStreamService {

    SseEmitter suscribir(UUID usuarioId);

    void publicar(UUID usuarioId, NotificacionResponse notificacion);

    void enviarLatido();

    int suscriptoresActivos();
}
