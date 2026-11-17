package com.codesoftlabs.tusalud.app.dto.response;

public record RecetaItemFarmaciaResponse(
        int id,
        String medicamento,
        String dosis,
        String frecuencia,
        String duracion,
        String indicaciones,
        boolean dispensado) {}
