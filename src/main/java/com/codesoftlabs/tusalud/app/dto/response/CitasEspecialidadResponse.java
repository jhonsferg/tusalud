package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cantidad de citas de una especialidad")
public record CitasEspecialidadResponse(
        @Schema(example = "Cardiología") String especialidad,
        @Schema(example = "8") long cantidad) {}
