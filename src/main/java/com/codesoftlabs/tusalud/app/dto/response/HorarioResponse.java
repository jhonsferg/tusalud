package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Schema(description = "Bloque de horario de atención")
public record HorarioResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

        @Schema(example = "MONDAY") DayOfWeek diaSemana,

        @Schema(type = "string", example = "08:00") @JsonFormat(pattern = "HH:mm")
        LocalTime horaInicio,

        @Schema(type = "string", example = "13:00") @JsonFormat(pattern = "HH:mm")
        LocalTime horaFin,

        @Schema(example = "30") Integer duracionCitaMinutos) {

    public static HorarioResponse from(HorarioAtencion horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getMedico().getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getDuracionCitaMinutos());
    }
}
