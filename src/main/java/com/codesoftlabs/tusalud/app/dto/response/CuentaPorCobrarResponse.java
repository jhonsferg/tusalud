package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CuentaPorCobrarResponse(
        OrigenComprobante origen,
        UUID referenciaId,
        UUID pacienteId,
        String paciente,
        String dni,
        String descripcion,
        BigDecimal monto,
        LocalDateTime fecha) {}
