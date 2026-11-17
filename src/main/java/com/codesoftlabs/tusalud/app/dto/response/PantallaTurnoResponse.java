package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Turno mostrado en la pantalla de la sala de espera")
public record PantallaTurnoResponse(
        @Schema(example = "A-003") String numero,
        @Schema(example = "Consultorio 101") String consultorio,
        @Schema(example = "LLAMADO") EstadoTurno estado) {}
