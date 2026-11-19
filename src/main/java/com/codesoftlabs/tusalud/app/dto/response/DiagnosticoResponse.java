package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDiagnostico;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Diagnóstico CIE-10 registrado en una atención")
public record DiagnosticoResponse(
        @Schema(example = "I10") String codigo,

        @Schema(example = "Hipertensión esencial (primaria)")
        String descripcion,

        @Schema(example = "DEFINITIVO") TipoDiagnostico tipo,
        boolean principal,
        String observacion) {

    public static DiagnosticoResponse from(AtencionDiagnostico d) {
        return new DiagnosticoResponse(
                d.getCodigo(), d.getDescripcion(), d.getTipo(), d.isPrincipal(), d.getObservacion());
    }
}
