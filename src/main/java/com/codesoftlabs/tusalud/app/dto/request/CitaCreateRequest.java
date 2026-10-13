package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Solicitud de una nueva cita (el paciente se toma del token)")
public record CitaCreateRequest(
        @Schema(example = "1") @NotNull(message = "El médico es obligatorio")
        Long medicoId,

        @Schema(type = "string", example = "2026-10-19T09:00:00", description = "ISO-8601 sin zona horaria")
        @NotNull(message = "La fecha y hora es obligatoria")
        @Future(message = "La fecha y hora de la cita debe ser futura")
        LocalDateTime fechaHora,

        @Schema(example = "Dolor de pecho al hacer esfuerzo")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no debe superar 500 caracteres")
        String motivo) {}
