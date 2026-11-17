package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Cama destino para un traslado o una asignación")
public record CamaAsignacionRequest(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @NotNull
        UUID camaId) {}
