package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud de una orden de laboratorio")
public record OrdenLabCreateRequest(
        @Schema(example = "1") @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId,

        @Schema(example = "10", description = "Atención de la que nace la orden, opcional")
        UUID atencionId,

        @Schema(example = "NORMAL") @NotNull(message = "La prioridad es obligatoria")
        PrioridadOrden prioridad,

        @Schema(example = "Paciente en ayunas") @Size(max = 500)
        String indicaciones,

        @Schema(example = "[1, 2]") @NotEmpty(message = "Debe elegir al menos un examen")
        List<@NotNull(message = "El examen es obligatorio") UUID> examenIds) {}
