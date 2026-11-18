package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.EventoSeguridad;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoSeguridadResponse(
        UUID id,
        String tipo,
        String etiqueta,
        String severidad,
        UUID usuarioId,
        String usuarioNombre,
        String actorNombre,
        String ip,
        String dispositivo,
        String detalle,
        LocalDateTime creadoEn) {

    public static EventoSeguridadResponse from(EventoSeguridad evento) {
        return new EventoSeguridadResponse(
                evento.getId(),
                evento.getTipo().name(),
                evento.getTipo().getEtiqueta(),
                evento.getSeveridad().name(),
                evento.getUsuario() == null ? null : evento.getUsuario().getId(),
                evento.getUsuario() == null ? null : evento.getUsuario().getNombreCompleto(),
                evento.getActor() == null ? null : evento.getActor().getNombreCompleto(),
                evento.getIpEnmascarada(),
                evento.getDispositivo(),
                evento.getDetalle(),
                evento.getCreadoEn());
    }
}
