package com.codesoftlabs.tusalud.app.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Bloque de horario de atención de un médico")
public record HorarioRequest(
        @Schema(example = "MONDAY") @NotNull(message = "El día de la semana es obligatorio")
        DayOfWeek diaSemana,

        @Schema(type = "string", example = "08:00")
        @JsonFormat(pattern = "HH:mm")
        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @Schema(type = "string", example = "13:00")
        @JsonFormat(pattern = "HH:mm")
        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @Schema(example = "30")
        @NotNull(message = "La duración de la cita es obligatoria")
        @Min(value = 10, message = "La duración mínima es 10 minutos")
        @Max(value = 240, message = "La duración máxima es 240 minutos")
        Integer duracionCitaMinutos) {}
