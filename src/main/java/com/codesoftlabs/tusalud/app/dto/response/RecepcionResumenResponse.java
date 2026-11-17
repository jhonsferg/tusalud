package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores de la recepción en el día")
public record RecepcionResumenResponse(
        @Schema(example = "4") long enEspera,
        @Schema(example = "1") long llamados,
        @Schema(example = "12") long atendidosHoy,
        @Schema(example = "2") long ausentesHoy,
        @Schema(example = "9") long esperaPromedioMinutos,
        @Schema(example = "6") long citasPendientesDeLlegada) {}
