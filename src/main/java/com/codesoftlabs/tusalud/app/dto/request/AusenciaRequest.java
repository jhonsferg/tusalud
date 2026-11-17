package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Solicitud de ausencia, permiso o vacaciones")
public record AusenciaRequest(
        @Schema(example = "VACACIONES") @NotNull(message = "El tipo de ausencia es obligatorio")
        TipoAusencia tipo,

        @Schema(example = "2026-12-01") @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @Schema(example = "2026-12-05") @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin,

        @Schema(example = "Viaje familiar") @Size(max = 300, message = "El motivo no debe superar 300 caracteres")
        String motivo) {}
