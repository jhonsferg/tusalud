package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Registro de un paciente que llega a emergencia")
public record EmergenciaRequest(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @NotNull
        UUID pacienteId,

        @NotNull @Valid TriajeRequest triaje) {}
