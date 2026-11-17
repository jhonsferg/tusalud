package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteEjecutivoResponse(
        PeriodoReporte periodo,
        PeriodoReporte periodoAnterior,
        IndicadorReporte citas,
        IndicadorReporte atendidas,
        IndicadorReporte canceladas,
        IndicadorReporte tasaAusenciaPct,
        IndicadorReporte ingresos,
        IndicadorReporte ticketPromedio,
        IndicadorReporte ocupacionHospitalariaPct,
        IndicadorReporte esperaPromedioMin,
        IndicadorReporte valoracionPromedio,
        List<Punto> serie) {

    public record Punto(LocalDate fecha, long citas, long atendidas, BigDecimal ingresos) {}
}
