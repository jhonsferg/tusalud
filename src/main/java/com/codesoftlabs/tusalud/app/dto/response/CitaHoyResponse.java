package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Cita del día en el mostrador de recepción")
public record CitaHoyResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID citaId,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime fechaHora,

        @Schema(example = "CONFIRMADA") EstadoCita estado,
        @Schema(example = "Control de presión arterial") String motivo,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID pacienteId,

        @Schema(example = "Juan Pérez") String pacienteNombre,
        @Schema(example = "12345678") String dni,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

        @Schema(example = "Luis García") String medicoNombre,
        @Schema(example = "Cardiología") String especialidad,
        @Schema(example = "Consultorio 101") String consultorio,
        @Schema(example = "true") boolean llego,
        @Schema(example = "A-003") String turnoNumero) {

    public static CitaHoyResponse from(Cita cita, String turnoNumero) {
        return new CitaHoyResponse(
                cita.getId(),
                cita.getFechaHora(),
                cita.getEstado(),
                null,
                cita.getPaciente().getId(),
                cita.getPaciente().getUsuario().getNombreCompleto(),
                cita.getPaciente().getDni(),
                cita.getMedico().getId(),
                cita.getMedico().getUsuario().getNombreCompleto(),
                cita.getEspecialidad().getNombre(),
                cita.getMedico().getConsultorio(),
                turnoNumero != null,
                turnoNumero);
    }
}
