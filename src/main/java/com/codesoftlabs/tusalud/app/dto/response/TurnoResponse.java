package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Turno;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Turno de atención en la sala de espera")
public record TurnoResponse(
        @Schema(example = "12") Long id,
        @Schema(example = "A-003") String numero,
        @Schema(type = "string", example = "2026-10-19") LocalDate fecha,
        @Schema(example = "3") Long pacienteId,
        @Schema(example = "Juan Pérez") String pacienteNombre,
        @Schema(example = "12345678") String dni,
        @Schema(example = "10") Long citaId,
        @Schema(example = "1") Long medicoId,
        @Schema(example = "Luis García") String medicoNombre,
        @Schema(example = "NORMAL") PrioridadTurno prioridad,
        @Schema(example = "EN_ESPERA") EstadoTurno estado,
        @Schema(example = "Control de presión arterial") String motivo,
        @Schema(example = "Consultorio 101") String consultorio,
        @Schema(type = "string") LocalDateTime creadoEn,
        @Schema(type = "string") LocalDateTime llamadoEn,
        @Schema(type = "string") LocalDateTime inicioAtencionEn,
        @Schema(type = "string") LocalDateTime finAtencionEn,

        @Schema(example = "7", description = "Minutos de espera hasta ser llamado o hasta ahora")
        long esperaMinutos) {

    public static TurnoResponse from(Turno turno, LocalDateTime ahora) {
        var paciente = turno.getPaciente();
        var medico = turno.getMedico();
        var cita = turno.getCita();
        return new TurnoResponse(
                turno.getId(),
                turno.getNumero(),
                turno.getFecha(),
                paciente.getId(),
                paciente.getUsuario().getNombreCompleto(),
                paciente.getDni(),
                cita == null ? null : cita.getId(),
                medico == null ? null : medico.getId(),
                medico == null ? null : medico.getUsuario().getNombreCompleto(),
                turno.getPrioridad(),
                turno.getEstado(),
                turno.getMotivo(),
                turno.getConsultorio(),
                turno.getCreadoEn(),
                turno.getLlamadoEn(),
                turno.getInicioAtencionEn(),
                turno.getFinAtencionEn(),
                espera(turno, ahora));
    }

    private static long espera(Turno turno, LocalDateTime ahora) {
        LocalDateTime fin = turno.getEstado() == EstadoTurno.EN_ESPERA
                ? ahora
                : turno.getLlamadoEn() != null
                        ? turno.getLlamadoEn()
                        : turno.getInicioAtencionEn() != null ? turno.getInicioAtencionEn() : ahora;
        return Math.max(0, Duration.between(turno.getCreadoEn(), fin).toMinutes());
    }
}
