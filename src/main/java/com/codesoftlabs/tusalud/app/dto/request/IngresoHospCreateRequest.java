package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Ingreso programado de un paciente a una cama")
public record IngresoHospCreateRequest(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @NotNull
        UUID pacienteId,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @NotNull
        UUID camaId,

        @Schema(example = "Descompensación de insuficiencia cardiaca") @NotBlank @Size(max = 300)
        String motivo,

        @Schema(example = "Insuficiencia cardiaca congestiva") @Size(max = 300)
        String diagnosticoIngreso,

        @Schema(example = "1", description = "Solo lo usa el administrador; un médico ingresa a su nombre")
        UUID medicoId) {}
