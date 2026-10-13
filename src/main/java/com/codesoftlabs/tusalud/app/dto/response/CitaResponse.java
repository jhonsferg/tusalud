package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Cita médica")
public record CitaResponse(
        @Schema(example = "10") Long id,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime fechaHora,

        @Schema(example = "Dolor de pecho al hacer esfuerzo")
        String motivo,

        @Schema(example = "PENDIENTE") EstadoCita estado,

        @Schema(example = "150.00") BigDecimal monto,

        @Schema(example = "1") Long medicoId,

        @Schema(example = "Luis García") String medicoNombre,

        @Schema(example = "Cardiología") String especialidad,

        @Schema(example = "Consultorio 101") String consultorio,

        @Schema(example = "1") Long pacienteId,

        @Schema(example = "Juan Pérez") String pacienteNombre,

        @Schema(example = "12345678") String pacienteDni,

        @Schema(type = "string", example = "2026-10-12T21:00:00")
        LocalDateTime createdAt) {

    public static CitaResponse from(Cita cita) {
        Medico medico = cita.getMedico();
        Paciente paciente = cita.getPaciente();
        return new CitaResponse(
                cita.getId(),
                cita.getFechaHora(),
                cita.getMotivo(),
                cita.getEstado(),
                cita.getMonto(),
                medico.getId(),
                medico.getUsuario().getNombreCompleto(),
                medico.getEspecialidad().getNombre(),
                medico.getConsultorio(),
                paciente.getId(),
                paciente.getUsuario().getNombreCompleto(),
                paciente.getDni(),
                cita.getCreatedAt());
    }
}
