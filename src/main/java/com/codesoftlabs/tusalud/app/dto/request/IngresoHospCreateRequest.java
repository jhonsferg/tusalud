package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Ingreso programado de un paciente a una cama")
public record IngresoHospCreateRequest(
        @Schema(example = "3") @NotNull @Positive Long pacienteId,
        @Schema(example = "4") @NotNull @Positive Long camaId,

        @Schema(example = "Descompensación de insuficiencia cardiaca") @NotBlank @Size(max = 300)
        String motivo,

        @Schema(example = "Insuficiencia cardiaca congestiva") @Size(max = 300)
        String diagnosticoIngreso,

        @Schema(example = "1", description = "Solo lo usa el administrador; un médico ingresa a su nombre")
        Long medicoId) {}
