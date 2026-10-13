package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
@Schema(description = "Respuesta de error estandarizada")
public record ErrorResponse(
        @Schema(description = "Fecha y hora del error", example = "2026-10-12T20:45:00")
        LocalDateTime timestamp,

        @Schema(description = "Código de estado HTTP", example = "409")
        int status,

        @Schema(description = "Descripción del estado HTTP", example = "Conflict")
        String error,

        @Schema(
                description = "Mensaje descriptivo del error",
                example = "El médico ya tiene una cita registrada en ese horario")
        String message,

        @Schema(description = "Ruta de la petición", example = "/api/v1/citas")
        String path,

        @Schema(description = "Errores de validación por campo (solo en errores 400 de validación)")
        Map<String, String> validationErrors) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(
            HttpStatus status, String message, String path, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }
}
