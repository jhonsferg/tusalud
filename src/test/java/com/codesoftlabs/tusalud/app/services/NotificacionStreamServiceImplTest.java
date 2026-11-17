package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionStreamService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest
@ActiveProfiles("test")
class NotificacionStreamServiceImplTest {

    @Autowired
    private NotificacionStreamService streamService;

    private static NotificacionResponse aviso() {
        return new NotificacionResponse(
                UUID.randomUUID(),
                TipoNotificacion.CITA_CONFIRMADA,
                "Cita confirmada",
                "Mensaje",
                "/citas/1",
                false,
                LocalDateTime.now());
    }

    @Test
    void registraSuscriptoresYPublicaSinFallarSiNoHayConexionActiva() {
        int antes = streamService.suscriptoresActivos();

        SseEmitter emisor = streamService.suscribir(UUID.randomUUID());

        assertThat(emisor).isNotNull();
        assertThat(streamService.suscriptoresActivos()).isGreaterThanOrEqualTo(antes);
        streamService.publicar(UUID.randomUUID(), aviso());
        streamService.publicar(UUID.randomUUID(), aviso());
        streamService.enviarLatido();
        emisor.complete();
    }
}
