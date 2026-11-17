package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Reserva de una cita en el mostrador a nombre de un paciente")
public record CitaMostradorRequest(
        @Schema(example = "3") @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId,

        @Schema(example = "1") @NotNull(message = "El médico es obligatorio")
        UUID medicoId,

        @Schema(example = "1") @NotNull(message = "La especialidad es obligatoria")
        UUID especialidadId,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        @NotNull(message = "La fecha y hora es obligatoria")
        @Future(message = "La fecha y hora de la cita debe ser futura")
        LocalDateTime fechaHora,

        @Schema(example = "Control de rutina")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no debe superar 500 caracteres")
        String motivo) {}
