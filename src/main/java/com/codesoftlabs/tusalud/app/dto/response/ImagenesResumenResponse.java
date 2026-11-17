package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores de la cola de imágenes diagnósticas")
public record ImagenesResumenResponse(
        long solicitadas, long programadas, long porInformar, long informadasHoy, long urgentes) {}
