package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.EventoCalendario;
import com.codesoftlabs.tusalud.app.entities.enums.ColorEvento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Evento del calendario personal")
public record EventoCalendarioResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Reunión de equipo") String titulo,
        String descripcion,
        String ubicacion,
        @Schema(example = "REUNION") TipoEvento tipo,

        @Schema(type = "string", example = "2026-11-20T10:00:00")
        LocalDateTime inicio,

        @Schema(type = "string", example = "2026-11-20T11:00:00")
        LocalDateTime fin,

        boolean todoElDia,
        @Schema(example = "AZUL") ColorEvento color,
        boolean completado,
        Integer recordatorioMinutos,

        @Schema(type = "string", example = "2026-11-17T10:00:00")
        LocalDateTime createdAt,

        @Schema(type = "string", example = "2026-11-17T10:00:00")
        LocalDateTime updatedAt) {

    public static EventoCalendarioResponse from(EventoCalendario evento) {
        return new EventoCalendarioResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getUbicacion(),
                evento.getTipo(),
                evento.getInicio(),
                evento.getFin(),
                evento.isTodoElDia(),
                evento.getColor(),
                evento.isCompletado(),
                evento.getRecordatorioMinutos(),
                evento.getCreatedAt(),
                evento.getUpdatedAt());
    }
}
