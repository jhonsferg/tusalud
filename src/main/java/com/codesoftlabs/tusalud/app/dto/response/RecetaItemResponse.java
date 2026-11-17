package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Medicamento de la receta")
public record RecetaItemResponse(
        @Schema(example = "Losartán 50 mg") String medicamento,
        @Schema(example = "1 tableta") String dosis,
        @Schema(example = "Cada 24 horas") String frecuencia,
        @Schema(example = "30 días") String duracion) {

    public static RecetaItemResponse from(RecetaItem item) {
        return new RecetaItemResponse(item.getMedicamento(), item.getDosis(), item.getFrecuencia(), item.getDuracion());
    }
}
