package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de la toma del estudio")
public record RealizarImagenRequest(
        @Schema(example = "Paciente colaborador, sin incidencias") @Size(max = 300)
        String observaciones) {}
