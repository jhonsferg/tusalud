package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Fecha, hora y sala en que se realizará el estudio")
public record ProgramarImagenRequest(
        @Schema(type = "string", example = "2026-11-20T09:30:00") @NotNull(message = "La fecha y hora son obligatorias")
        LocalDateTime programadaPara,

        @Schema(example = "Sala de rayos X 1") @NotBlank(message = "La sala es obligatoria") @Size(max = 40)
        String sala) {}
