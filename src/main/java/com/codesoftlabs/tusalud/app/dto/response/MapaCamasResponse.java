package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Camas agrupadas por área")
public record MapaCamasResponse(List<AreaMapa> areas) {

    @Schema(description = "Camas de un área con sus totales")
    public record AreaMapa(AreaCama area, int total, int libres, int ocupadas, List<CamaResponse> camas) {}
}
