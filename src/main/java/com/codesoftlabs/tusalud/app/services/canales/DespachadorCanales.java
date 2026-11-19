package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.EnmascaradorContacto;
import com.codesoftlabs.tusalud.app.canales.PoliticaCanales;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionCanales;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.PreferenciaCanal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.ConfiguracionCanalesRepository;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaCanalRepository;
import com.codesoftlabs.tusalud.app.services.events.NotificacionCreadaEvent;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DespachadorCanales {

    private final ContactoCanalRepository contactos;
    private final PreferenciaCanalRepository preferencias;
    private final ConfiguracionCanalesRepository configuraciones;
    private final EntregaNotificacionRepository entregas;
    private final CanalesProperties propiedades;

    @EventListener
    public void alCrearNotificacion(NotificacionCreadaEvent evento) {
        try {
            despachar(evento.usuarioId(), evento.notificacion(), LocalDateTime.now());
        } catch (RuntimeException ex) {
            log.warn(
                    "No se pudo programar la entrega externa de la notificación {}",
                    evento.notificacion().id(),
                    ex);
        }
    }

    public int despachar(UUID usuarioId, NotificacionResponse notificacion, LocalDateTime ahora) {
        var politica = PoliticaCanales.de(notificacion.tipo()).orElse(null);
        if (politica == null) {
            return 0;
        }
        List<ContactoCanal> destinos = contactos.findByUsuarioId(usuarioId);
        if (destinos.isEmpty()) {
            return 0;
        }
        List<PreferenciaCanal> guardadas = preferencias.findByUsuarioId(usuarioId);
        ConfiguracionCanales configuracion =
                configuraciones.findByUsuarioId(usuarioId).orElse(null);
        int creadas = 0;
        for (ContactoCanal contacto : destinos) {
            if (!contacto.puedeRecibir(politica.obligatoria())) {
                continue;
            }
            boolean activo = politica.obligatoria()
                    || guardadas.stream()
                            .filter(p -> p.getTipo() == notificacion.tipo() && p.getCanal() == contacto.getCanal())
                            .findFirst()
                            .map(PreferenciaCanal::isActivo)
                            .orElse(PoliticaCanales.activoPorDefecto(notificacion.tipo(), contacto.getCanal()));
            if (!activo) {
                continue;
            }
            String clave = notificacion.id() + ":" + contacto.getCanal();
            if (entregas.existsByClaveIdempotencia(clave)) {
                continue;
            }
            EntregaNotificacion.EntregaNotificacionBuilder entrega = EntregaNotificacion.builder()
                    .notificacionId(notificacion.id())
                    .usuarioId(usuarioId)
                    .canal(contacto.getCanal())
                    .tipo(notificacion.tipo())
                    .destinoMascara(EnmascaradorContacto.enmascarar(contacto.getCanal(), contacto.getDestino()))
                    .claveIdempotencia(clave)
                    .urgente(politica.urgente())
                    .creadoEn(ahora);
            long enLaHora = entregas.countByUsuarioIdAndCanalAndTipoNotAndCreadoEnAfter(
                    usuarioId, contacto.getCanal(), TipoNotificacion.SISTEMA, ahora.minusHours(1));
            if (!politica.obligatoria() && enLaHora >= propiedades.limitePorHora()) {
                entregas.save(entrega.estado(EstadoEntrega.FALLIDO)
                        .motivo("Límite de mensajes por hora superado")
                        .build());
                continue;
            }
            LocalDateTime programada = ahora;
            if (!politica.urgente() && configuracion != null && configuracion.isSilencioActivo()) {
                programada =
                        despuesDelSilencio(ahora, configuracion.getSilencioDesde(), configuracion.getSilencioHasta());
            } else if (!politica.urgente() && configuracion == null) {
                programada = despuesDelSilencio(
                        ahora, ConfiguracionCanales.SILENCIO_DESDE, ConfiguracionCanales.SILENCIO_HASTA);
            }
            entregas.save(entrega.proximoIntento(programada).build());
            creadas++;
        }
        return creadas;
    }

    public static LocalDateTime despuesDelSilencio(LocalDateTime ahora, LocalTime desde, LocalTime hasta) {
        LocalTime hora = ahora.toLocalTime();
        boolean nocturna = desde.isAfter(hasta);
        boolean enSilencio = nocturna
                ? !hora.isBefore(desde) || hora.isBefore(hasta)
                : !hora.isBefore(desde) && hora.isBefore(hasta);
        if (!enSilencio) {
            return ahora;
        }
        if (nocturna && !hora.isBefore(desde)) {
            return ahora.toLocalDate().plusDays(1).atTime(hasta);
        }
        return ahora.toLocalDate().atTime(hasta);
    }
}
