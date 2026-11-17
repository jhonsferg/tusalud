package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCajaResponse(
        Long cajaId,
        BigDecimal montoApertura,
        long comprobantes,
        BigDecimal total,
        BigDecimal efectivoEsperado,
        List<MetodoTotalResponse> porMetodo) {}
