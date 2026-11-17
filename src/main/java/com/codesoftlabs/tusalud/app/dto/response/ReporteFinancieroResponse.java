package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteFinancieroResponse(
        PeriodoReporte periodo,
        BigDecimal totalCobrado,
        long transacciones,
        BigDecimal ticketPromedio,
        List<Dia> porDia,
        List<Metodo> porMetodo,
        List<Origen> porOrigen,
        List<Item> porEspecialidad,
        List<Item> porMedico,
        Comprobantes comprobantes,
        Cajas cajas,
        PorCobrar porCobrar) {

    public record Dia(LocalDate fecha, BigDecimal total, long transacciones) {}

    public record Metodo(String metodo, BigDecimal total, long transacciones) {}

    public record Origen(
            String origen,
            BigDecimal total,

            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "Verdadero si es un cargo devengado y no cobrado en el periodo")
            boolean devengado) {}

    public record Item(String nombre, long cantidad, BigDecimal total) {}

    public record Comprobantes(long emitidos, long anulados, BigDecimal tasaAnulacionPct) {}

    public record Cajas(
            long sesionesCerradas, BigDecimal faltante, BigDecimal sobrante, BigDecimal neto, List<Cajero> porCajero) {}

    public record Cajero(String nombre, long sesiones, BigDecimal faltante, BigDecimal sobrante, BigDecimal neto) {}

    public record PorCobrar(Monto citas, Monto laboratorio, BigDecimal total) {}

    public record Monto(long cantidad, BigDecimal monto) {}
}
