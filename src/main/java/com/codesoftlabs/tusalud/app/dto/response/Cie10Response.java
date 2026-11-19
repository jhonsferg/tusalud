package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Diagnóstico del catálogo CIE-10")
public record Cie10Response(
        @Schema(example = "I10") String codigo,

        @Schema(example = "Hipertensión esencial (primaria)")
        String descripcion,

        String capitulo,
        String grupo,
        SexoPaciente sexo,
        Integer edadMinima,
        Integer edadMaxima,
        boolean muestra) {

    public static Cie10Response from(Cie10Catalogo c) {
        return new Cie10Response(
                c.getCodigo(),
                c.getDescripcion(),
                c.getCapitulo(),
                c.getGrupo(),
                c.getSexo(),
                c.getEdadMinima(),
                c.getEdadMaxima(),
                c.isMuestra());
    }
}
