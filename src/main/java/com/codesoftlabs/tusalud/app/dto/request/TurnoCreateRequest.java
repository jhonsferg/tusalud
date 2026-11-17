package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Turno de atención sin cita previa")
public record TurnoCreateRequest(
        @Schema(example = "3") @NotNull(message = "El paciente es obligatorio")
        Long pacienteId,

        @Schema(example = "1", description = "Médico que atenderá el turno; es opcional")
        Long medicoId,

        @Schema(example = "NORMAL") @NotNull(message = "La prioridad es obligatoria")
        PrioridadTurno prioridad,

        @Schema(example = "Dolor de garganta desde ayer")
        @Size(max = 300, message = "El motivo no debe superar 300 caracteres")
        String motivo) {}
