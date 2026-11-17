package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;

public record CatalogoFarmaciaResponse(
        Long id,
        String codigo,
        String nombre,
        String principioActivo,
        String presentacion,
        String unidad,
        BigDecimal precioVenta,
        long stockActual,
        boolean requiereReceta) {}
