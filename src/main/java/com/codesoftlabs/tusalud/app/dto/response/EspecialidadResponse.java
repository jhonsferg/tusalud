package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Especialidad médica con el precio de su consulta")
public record EspecialidadResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "Cardiología") String nombre,

        @Schema(example = "Diagnóstico y tratamiento de enfermedades del corazón")
        String descripcion,

        @Schema(example = "150.00") BigDecimal precioConsulta) {

    public static EspecialidadResponse from(Especialidad especialidad) {
        return new EspecialidadResponse(
                especialidad.getId(),
                especialidad.getNombre(),
                especialidad.getDescripcion(),
                especialidad.getPrecioConsulta());
    }
}
