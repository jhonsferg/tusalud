package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Especialidad médica con el precio de su consulta")
public record EspecialidadResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

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
