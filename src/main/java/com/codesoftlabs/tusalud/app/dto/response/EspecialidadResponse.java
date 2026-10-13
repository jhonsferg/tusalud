package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Especialidad médica")
public record EspecialidadResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "Cardiología") String nombre,

        @Schema(example = "Diagnóstico y tratamiento de enfermedades del corazón")
        String descripcion) {

    public static EspecialidadResponse from(Especialidad especialidad) {
        return new EspecialidadResponse(especialidad.getId(), especialidad.getNombre(), especialidad.getDescripcion());
    }
}
