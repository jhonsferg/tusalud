package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CuentaPorCobrarResponse(
        OrigenComprobante origen,
        Long referenciaId,
        Long pacienteId,
        String paciente,
        String dni,
        String descripcion,
        BigDecimal monto,
        LocalDateTime fecha) {}
