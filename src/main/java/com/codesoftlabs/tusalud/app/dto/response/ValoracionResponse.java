package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Valoracion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Valoración de un paciente sobre la atención de un médico")
public record ValoracionResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID citaId,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

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
