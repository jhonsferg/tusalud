package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Schema(description = "Bloque de horario de atención")
public record HorarioResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "1") Long medicoId,

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
