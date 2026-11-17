package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReporteCalidadResponse(
        PeriodoReporte periodo,
        Valoraciones valoraciones,
        List<ValoracionGrupo> porMedico,
        List<ValoracionGrupo> porEspecialidad,
        List<Cancelacion> citasPorEspecialidad,
        Laboratorio laboratorio,
        Anulaciones anulaciones) {

    public record Valoraciones(BigDecimal promedio, long total, List<Long> distribucion) {}

    public record ValoracionGrupo(String nombre, long total, BigDecimal promedio, List<Long> distribucion) {}

    public record Cancelacion(
            String especialidad,
            long total,
            long canceladas,
            long noAsistieron,
            BigDecimal tasaCancelacionPct,
            BigDecimal tasaAusenciaPct) {}

    public record Laboratorio(
            long completadas,
            long canceladas,
            BigDecimal entregaPromedioHoras,
            BigDecimal entregaP90Horas,
            List<PorPrioridad> porPrioridad) {}

    public record PorPrioridad(String prioridad, long cantidad, BigDecimal promedioHoras) {}

    public record Anulaciones(
            long comprobantes,
            long dispensaciones,
            List<Motivo> motivosComprobantes,
            List<Motivo> motivosDispensaciones) {}

    public record Motivo(String motivo, long cantidad) {}
}
