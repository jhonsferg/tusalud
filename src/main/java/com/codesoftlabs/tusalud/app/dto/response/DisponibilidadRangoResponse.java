package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Horas libres de un médico en un día con atención")
public record DisponibilidadRangoResponse(
        @Schema(type = "string", example = "2026-10-19") LocalDate fecha,

        @Schema(description = "Horas de inicio libres", example = "[\"08:00:00\", \"08:30:00\"]")
        List<LocalTime> horariosDisponibles,

        @Schema(example = "2") int totalHoras) {}
