package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Valoracion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Valoración de un paciente sobre la atención de un médico")
public record ValoracionResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "10") Long citaId,
        @Schema(example = "1") Long medicoId,
        @Schema(example = "Juan Pérez") String pacienteNombre,
        @Schema(example = "5") int puntuacion,
        @Schema(example = "Muy buena atención") String comentario,

        @Schema(type = "string", example = "2026-10-19T10:15:30")
        LocalDateTime createdAt) {

    public static ValoracionResponse from(Valoracion valoracion) {
        return new ValoracionResponse(
                valoracion.getId(),
                valoracion.getCita().getId(),
                valoracion.getMedico().getId(),
                valoracion.getPaciente().getUsuario().getNombreCompleto(),
                valoracion.getPuntuacion(),
                valoracion.getComentario(),
                valoracion.getCreatedAt());
    }
}
