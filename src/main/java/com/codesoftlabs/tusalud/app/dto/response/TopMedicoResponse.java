package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Médico con más citas atendidas")
public record TopMedicoResponse(
        @Schema(example = "1") Long medicoId,
        @Schema(example = "Luis García") String nombre,
        @Schema(example = "12") long citasAtendidas) {}
