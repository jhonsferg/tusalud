package com.codesoftlabs.tusalud.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Horarios libres de un médico en una fecha")
public record DisponibilidadResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

        @Schema(example = "Luis García") String medicoNombre,

        @Schema(type = "string", example = "2026-10-19") LocalDate fecha,

        @Schema(description = "Horas de inicio libres (formato HH:mm)", example = "[\"08:00\", \"08:30\", \"09:30\"]")
        @JsonFormat(pattern = "HH:mm")
        List<LocalTime> horariosDisponibles) {}
