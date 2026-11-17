package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.ColorEvento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Datos de un evento del calendario personal")
public record EventoCalendarioRequest(
        @Schema(example = "Reunión de equipo") @NotBlank(message = "El título es obligatorio") @Size(max = 120)
        String titulo,

        @Schema(example = "Revisión semanal de pacientes") @Size(max = 500)
        String descripcion,

        @Schema(example = "Sala 2") @Size(max = 120) String ubicacion,

        @Schema(example = "REUNION") @NotNull(message = "El tipo es obligatorio")
        TipoEvento tipo,

        @Schema(type = "string", example = "2026-11-20T10:00:00") @NotNull(message = "El inicio es obligatorio")
        LocalDateTime inicio,

        @Schema(type = "string", example = "2026-11-20T11:00:00") @NotNull(message = "El fin es obligatorio")
        LocalDateTime fin,

        @Schema(example = "false") Boolean todoElDia,
        @Schema(example = "AZUL") ColorEvento color,
        @Schema(example = "false") Boolean completado,
        @Schema(example = "30") @Min(0) @Max(10080) Integer recordatorioMinutos) {}
