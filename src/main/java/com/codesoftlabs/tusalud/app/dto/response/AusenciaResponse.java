package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AusenciaResponse(
        UUID id,
        UUID empleadoId,
        String empleadoNombre,
        AreaLaboral area,
        TipoAusencia tipo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int dias,
        String motivo,
        EstadoAusencia estado,
        String resueltoPor,
        LocalDateTime resueltoEn,
        String motivoResolucion,
        LocalDateTime createdAt) {

    public static AusenciaResponse from(Ausencia a) {
        return new AusenciaResponse(
                a.getId(),
                a.getEmpleado().getId(),
                a.getEmpleado().getUsuario().getNombreCompleto(),
                a.getEmpleado().getArea(),
                a.getTipo(),
                a.getFechaInicio(),
                a.getFechaFin(),
                a.getDias(),
                a.getMotivo(),
                a.getEstado(),
                a.getResueltoPor() == null ? null : a.getResueltoPor().getNombreCompleto(),
                a.getResueltoEn(),
                a.getMotivoResolucion(),
                a.getCreatedAt());
    }

    public AusenciaResponse sinMotivos() {
        return new AusenciaResponse(
                id,
                empleadoId,
                empleadoNombre,
                area,
                tipo,
                fechaInicio,
                fechaFin,
                dias,
                null,
                estado,
                resueltoPor,
                resueltoEn,
                null,
                createdAt);
    }
}
