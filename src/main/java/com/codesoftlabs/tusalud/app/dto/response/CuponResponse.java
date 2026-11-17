package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cupon;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Cupón de descuento")
public record CuponResponse(
        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(example = "BIENVENIDA10") String codigo,

        @Schema(example = "10% de descuento para nuevos pacientes")
        String descripcion,

        @Schema(example = "PORCENTAJE") TipoCupon tipo,
        @Schema(example = "10.00") BigDecimal valor,
        @Schema(type = "string", example = "2026-01-01") LocalDate fechaInicio,
        @Schema(type = "string", example = "2026-12-31") LocalDate fechaFin,
        @Schema(example = "100") Integer usosMaximos,
        @Schema(example = "4") int usosActuales,
        @Schema(example = "true") boolean activo,

        @Schema(example = "true", description = "Activo, dentro de fechas y con usos disponibles")
        boolean vigente,

        @Schema(type = "string", example = "2026-10-12T21:00:00")
        LocalDateTime createdAt) {

    public static CuponResponse from(Cupon cupon) {
        return new CuponResponse(
                cupon.getId(),
                cupon.getCodigo(),
                cupon.getDescripcion(),
                cupon.getTipo(),
                cupon.getValor(),
                cupon.getFechaInicio(),
                cupon.getFechaFin(),
                cupon.getUsosMaximos(),
                cupon.getUsosActuales(),
                cupon.isActivo(),
                cupon.esVigente(LocalDate.now()),
                cupon.getCreatedAt());
    }
}
