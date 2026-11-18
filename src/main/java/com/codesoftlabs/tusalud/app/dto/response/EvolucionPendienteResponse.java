package com.codesoftlabs.tusalud.app.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record EvolucionPendienteResponse(
        UUID ingresoId,
        String codigo,
        String cama,
        String area,
        String paciente,
        LocalDateTime ingresadoEn,
        LocalDateTime ultimaEvolucion,
        long horasSinRegistro) {}
