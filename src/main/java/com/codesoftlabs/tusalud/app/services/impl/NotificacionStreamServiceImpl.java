package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.services.events.NotificacionCreadaEvent;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionStreamService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class NotificacionStreamServiceImpl implements NotificacionStreamService {

    private static final long SIN_TIEMPO_LIMITE = 0L;

    private final Map<UUID, List<SseEmitter>> suscriptores = new ConcurrentHashMap<>();

    @Override
    public SseEmitter suscribir(UUID usuarioId) {
        SseEmitter emisor = new SseEmitter(SIN_TIEMPO_LIMITE);
        List<SseEmitter> lista = suscriptores.computeIfAbsent(usuarioId, id -> new CopyOnWriteArrayList<>());
        lista.add(emisor);
        Runnable retirar = () -> retirar(usuarioId, emisor);
        emisor.onCompletion(retirar);
        emisor.onTimeout(retirar);
        emisor.onError(error -> retirar.run());
        enviar(usuarioId, emisor, SseEmitter.event().name("conectado").data("ok"));
        return emisor;
    }

    @Override
    public void publicar(UUID usuarioId, NotificacionResponse notificacion) {
        List<SseEmitter> lista = suscriptores.get(usuarioId);
        if (lista == null) {
            log.debug("El usuario {} no tiene suscriptores de notificaciones", usuarioId);
            return;
        }
        log.debug("Publicando una notificación a {} suscriptores del usuario {}", lista.size(), usuarioId);
        lista.forEach(emisor -> enviar(
                usuarioId,
                emisor,
                SseEmitter.event().name("notificacion").data(notificacion, MediaType.APPLICATION_JSON)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCrearNotificacion(NotificacionCreadaEvent evento) {
        publicar(evento.usuarioId(), evento.notificacion());
    }

    @Override
    @Scheduled(fixedRate = 25_000)
    public void enviarLatido() {
        suscriptores.forEach((usuarioId, lista) -> lista.forEach(emisor ->
                enviar(usuarioId, emisor, SseEmitter.event().name("ping").data("1"))));
    }

    @Override
    public int suscriptoresActivos() {
        return suscriptores.values().stream().mapToInt(List::size).sum();
    }

    private void enviar(UUID usuarioId, SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            emisor.send(evento);
        } catch (IOException | IllegalStateException e) {
            log.debug("Se descarta un suscriptor de notificaciones del usuario {}: {}", usuarioId, e.getMessage());
            retirar(usuarioId, emisor);
        }
    }

    private void retirar(UUID usuarioId, SseEmitter emisor) {
        List<SseEmitter> lista = suscriptores.get(usuarioId);
        if (lista == null) {
            return;
        }
        lista.remove(emisor);
        if (lista.isEmpty()) {
            suscriptores.remove(usuarioId, lista);
        }
    }
}
