package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Registro de un paciente que llega a emergencia")
public record EmergenciaRequest(
        @Schema(example = "3") @NotNull @Positive Long pacienteId,
        @NotNull @Valid TriajeRequest triaje) {}
