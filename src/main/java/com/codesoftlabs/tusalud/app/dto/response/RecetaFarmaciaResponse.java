package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import java.time.LocalDateTime;
import java.util.List;

public record RecetaFarmaciaResponse(
        Long atencionId,
        Long citaId,
        LocalDateTime fecha,
        Long pacienteId,
        String pacienteNombre,
        String dni,
        String medicoNombre,
        EstadoReceta estado,
        List<RecetaItemFarmaciaResponse> items) {}
