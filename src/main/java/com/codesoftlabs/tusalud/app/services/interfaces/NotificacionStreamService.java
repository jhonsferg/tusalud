package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificacionStreamService {

    SseEmitter suscribir(Long usuarioId);

    void publicar(Long usuarioId, NotificacionResponse notificacion);

    void enviarLatido();

    int suscriptoresActivos();
}
