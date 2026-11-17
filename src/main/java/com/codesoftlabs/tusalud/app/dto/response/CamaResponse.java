package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cama;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Cama hospitalaria con su ocupante actual")
public record CamaResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "H-201") String codigo,
        @Schema(example = "HOSPITALIZACION") AreaCama area,
        @Schema(example = "2") Integer piso,
        String descripcion,
        @Schema(example = "OCUPADA") EstadoCama estado,
        @Schema(example = "180.00") BigDecimal precioDia,
        boolean activa,
        @Schema(example = "5") Long ingresoActivoId,
        @Schema(example = "Jorge Medina") String pacienteNombre) {

    public static CamaResponse from(Cama cama, Long ingresoActivoId, String pacienteNombre) {
        return new CamaResponse(
                cama.getId(),
                cama.getCodigo(),
                cama.getArea(),
                cama.getPiso(),
                cama.getDescripcion(),
                cama.getEstado(),
                cama.getPrecioDia(),
                cama.isActiva(),
                ingresoActivoId,
                pacienteNombre);
    }
}
