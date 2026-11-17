package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;

public record ResumenFarmaciaResponse(BigDecimal ventasHoy, long dispensacionesHoy, long recetasPendientes) {}
