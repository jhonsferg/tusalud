package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteIngresosResponse(
        BigDecimal total, List<Dia> porDia, List<Metodo> porMetodo, List<Origen> porOrigen) {

    public record Dia(LocalDate fecha, BigDecimal total, long comprobantes) {}

    public record Metodo(MetodoPagoCaja metodo, BigDecimal total) {}

    public record Origen(OrigenComprobante origen, BigDecimal total) {}
}
