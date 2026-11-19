package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Diagnóstico codificado con CIE-10 de una atención")
public record DiagnosticoRequest(
        @Schema(example = "I10")
        @NotBlank(message = "El código CIE-10 es obligatorio")
        @Pattern(
                regexp = "^[A-Za-z][0-9]{2}(\\.[0-9A-Za-z]{1,4})?$",
                message = "El código CIE-10 no tiene un formato válido")
        String codigo,

        @Schema(example = "DEFINITIVO") @NotNull(message = "El tipo de diagnóstico es obligatorio")
        TipoDiagnostico tipo,

        @Schema(description = "Debe haber exactamente un diagnóstico principal")
        boolean principal,

        @Schema(example = "En control con tratamiento")
        @Size(max = 200, message = "La observación no debe superar 200 caracteres")
        String observacion) {}
