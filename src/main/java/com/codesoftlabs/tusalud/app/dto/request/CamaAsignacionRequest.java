package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Cama destino para un traslado o una asignación")
public record CamaAsignacionRequest(
        @Schema(example = "4") @NotNull @Positive Long camaId) {}
