package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Paciente con citas del médico autenticado")
public record PacienteMedicoResponse(
        @Schema(example = "1") Long pacienteId,
        @Schema(example = "Juan Pérez") String nombreCompleto,
        @Schema(example = "12345678") String dni,
        @Schema(example = "HC-000001") String historialClinicoNumero,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime ultimaCita,

        @Schema(example = "3") long totalCitas,

        @Schema(
                example = "Ricardo Salazar",
                description = "Médico que compartió al paciente; null si lo atiende directamente")
        String compartidoPor) {}
