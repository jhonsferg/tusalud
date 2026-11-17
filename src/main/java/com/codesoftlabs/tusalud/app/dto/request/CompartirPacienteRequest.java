package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para compartir un paciente con otro médico")
public record CompartirPacienteRequest(
        @Schema(example = "3") @NotNull(message = "El médico es obligatorio")
        Long medicoId,

        @Schema(example = "Requiere valoración cardiológica complementaria")
        @Size(max = 500, message = "La nota no debe superar 500 caracteres")
        String nota) {}
