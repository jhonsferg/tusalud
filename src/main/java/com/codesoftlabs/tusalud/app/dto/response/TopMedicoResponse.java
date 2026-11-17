package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Médico con más citas atendidas")
public record TopMedicoResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID medicoId,

        @Schema(example = "Luis García") String nombre,
        @Schema(example = "12") long citasAtendidas) {}
