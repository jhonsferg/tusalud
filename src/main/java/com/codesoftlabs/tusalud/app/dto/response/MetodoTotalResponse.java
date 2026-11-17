package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import java.math.BigDecimal;

public record MetodoTotalResponse(MetodoPagoCaja metodo, BigDecimal total, long comprobantes) {}
