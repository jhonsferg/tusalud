package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;

public record IndicadorReporte(
        BigDecimal actual,
        BigDecimal anterior,

        @io.swagger.v3.oas.annotations.media.Schema(description = "Nulo si el periodo anterior fue cero")
        BigDecimal variacionPct) {}
