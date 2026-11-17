package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RecetaFarmaciaResponse(
        UUID atencionId,
        UUID citaId,
        LocalDateTime fecha,
        UUID pacienteId,
        String pacienteNombre,
        String dni,
        String medicoNombre,
        EstadoReceta estado,
        List<RecetaItemFarmaciaResponse> items) {}
