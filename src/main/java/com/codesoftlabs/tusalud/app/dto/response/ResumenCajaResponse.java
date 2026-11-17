package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumenCajaResponse(
        UUID cajaId,
        BigDecimal montoApertura,
        long comprobantes,
        BigDecimal total,
        BigDecimal efectivoEsperado,
        List<MetodoTotalResponse> porMetodo) {}
