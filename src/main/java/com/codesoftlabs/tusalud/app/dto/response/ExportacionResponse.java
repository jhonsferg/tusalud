package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Exportacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Exportación solicitada por el usuario")
public record ExportacionResponse(
        UUID id,
        String tipo,
        String formato,
        EstadoExportacion estado,
        String nombre,
        Long tamano,
        String error,
        LocalDateTime creadaEn,
        LocalDateTime terminadaEn,
        LocalDateTime expiraEn) {

    public static ExportacionResponse from(Exportacion exportacion) {
        return new ExportacionResponse(
                exportacion.getId(),
                exportacion.getTipo().codigo(),
                exportacion.getFormato().name(),
                exportacion.getEstado(),
                exportacion.getNombre(),
                exportacion.getTamano(),
                exportacion.getError(),
                exportacion.getCreatedAt(),
                exportacion.getTerminadaEn(),
                exportacion.getExpiraEn());
    }
}
