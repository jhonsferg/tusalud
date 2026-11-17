package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteOperativoResponse(
        PeriodoReporte periodo,
        BigDecimal ocupacionPromedioPct,
        List<AreaOcupacion> camasActuales,
        List<PuntoOcupacion> ocupacionSerie,
        long altas,
        BigDecimal estanciaPromedioDias,
        Esperas esperas,
        List<Emergencia> emergenciasPorNivel,
        List<Demanda> demanda,
        List<AgendaMedico> agenda,
        Inventario inventario) {

    public record AreaOcupacion(
            AreaCama area,
            long total,
            long ocupadas,
            long libres,
            long limpieza,
            long mantenimiento,
            BigDecimal porcentaje) {}

    public record PuntoOcupacion(LocalDate fecha, BigDecimal porcentaje) {}

    public record Esperas(long turnos, BigDecimal promedioMin, BigDecimal p90Min, List<EsperaHora> porHora) {}

    public record EsperaHora(int hora, long turnos, BigDecimal promedioMin) {}

    public record Emergencia(int nivel, String clasificacion, long cantidad) {}

    public record Demanda(
            @io.swagger.v3.oas.annotations.media.Schema(description = "1 lunes a 7 domingo")
            int diaSemana,

            int hora,
            long citas) {}

    public record AgendaMedico(String medico, long citas, long capacidad, BigDecimal utilizacionPct) {}

    public record Inventario(
            List<ProductoCritico> bajoMinimo,
            long lotesPorVencer,
            long unidadesPorVencer,
            long lotesVencidos,
            long unidadesVencidas) {}

    public record ProductoCritico(String codigo, String nombre, long stock, int stockMinimo) {}
}
