package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indicadores de hospitalización y emergencia")
public record HospResumenResponse(
        @Schema(example = "8") long camasLibres,
        @Schema(example = "5") long camasOcupadas,
        @Schema(example = "1") long camasLimpieza,
        @Schema(example = "4") long ingresosActivos,
        @Schema(example = "2") long emergenciasEnEspera,
        @Schema(example = "38.5") double ocupacionPorcentaje) {}
