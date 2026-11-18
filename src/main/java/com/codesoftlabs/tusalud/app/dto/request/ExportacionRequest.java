package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "Solicitud de exportación de un reporte o listado")
public record ExportacionRequest(
        @Schema(example = "reporte-ejecutivo") @NotBlank @Size(max = 40)
        String tipo,

        @Schema(example = "PDF") @Size(max = 10) String formato,

        @Schema(description = "Filtros del reporte, como desde, hasta, agrupacion, q o estado") @Size(max = 12)
        Map<@NotBlank @Size(max = 30) String, @Size(max = 80) String> parametros) {}
