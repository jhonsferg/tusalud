package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;

public record ResumenAlmacenResponse(
        long totalProductos,
        long productosBajoStock,
        long lotesPorVencer,
        long lotesVencidos,
        BigDecimal valorInventario) {}
