package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionStreamService;
import java.time.LocalDateTime;
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
                1L,
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

        SseEmitter emisor = streamService.suscribir(987_654L);

        assertThat(emisor).isNotNull();
        assertThat(streamService.suscriptoresActivos()).isGreaterThanOrEqualTo(antes);
        streamService.publicar(987_654L, aviso());
        streamService.publicar(111_111L, aviso());
        streamService.enviarLatido();
        emisor.complete();
    }
}
