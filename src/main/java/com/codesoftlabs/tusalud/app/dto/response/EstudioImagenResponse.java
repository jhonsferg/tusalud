package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.EstudioImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Estudio del catálogo de imágenes diagnósticas")
public record EstudioImagenResponse(
        UUID id,
        String codigo,
        String nombre,
        ModalidadImagen modalidad,
        String region,
        String preparacion,
        BigDecimal precio,
        int duracionMinutos,
        boolean activo) {

    public static EstudioImagenResponse from(EstudioImagen estudio) {
        return new EstudioImagenResponse(
                estudio.getId(),
                estudio.getCodigo(),
                estudio.getNombre(),
                estudio.getModalidad(),
                estudio.getRegion(),
                estudio.getPreparacion(),
                estudio.getPrecio(),
                estudio.getDuracionMinutos(),
                estudio.isActivo());
    }
}
