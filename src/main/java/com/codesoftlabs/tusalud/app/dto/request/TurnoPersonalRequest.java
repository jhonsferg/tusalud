package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Asignación de un turno de trabajo a un empleado")
public record TurnoPersonalRequest(
        @NotNull(message = "El empleado es obligatorio") UUID empleadoId,

        @NotNull(message = "La plantilla de turno es obligatoria")
        UUID plantillaId,

        @Schema(example = "2026-11-24") @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @Size(max = 200, message = "Las notas no deben superar 200 caracteres")
        String notas) {}
