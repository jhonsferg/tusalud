package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de una cama")
public record CamaEstadoRequest(
        @Schema(example = "LIBRE") @NotNull EstadoCama estado) {}
