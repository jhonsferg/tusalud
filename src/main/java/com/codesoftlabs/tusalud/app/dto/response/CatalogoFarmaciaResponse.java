package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogoFarmaciaResponse(
        UUID id,
        String codigo,
        String nombre,
        String principioActivo,
        String presentacion,
        String unidad,
        BigDecimal precioVenta,
        long stockActual,
        boolean requiereReceta) {}
