package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Morbilidad por diagnóstico principal CIE-10, solo agregados y sin datos de pacientes")
public record ReporteMorbilidadResponse(
        PeriodoReporte periodo,
        long casos,
        long diagnosticosDistintos,

        @Schema(description = "Las celdas por edad y sexo con menos casos que este umbral se ocultan")
        int umbralSupresion,

        List<Diagnostico> top,
        List<Capitulo> porCapitulo,
        List<PorEspecialidad> porEspecialidad,
        List<GrupoEtario> porGrupoEtario,
        List<Punto> serie,
        List<Tendencia> tendencia) {

    public record Diagnostico(String codigo, String descripcion, String capitulo, long casos, BigDecimal porcentaje) {}

    public record Capitulo(String capitulo, long casos, BigDecimal porcentaje) {}

    public record DiagnosticoCorto(String codigo, String descripcion, long casos) {}

    public record PorEspecialidad(String especialidad, long casos, List<DiagnosticoCorto> top) {}

    public record GrupoEtario(
            String grupo, Long masculino, Long femenino, Long sinDato, Long total, boolean suprimido) {}

    public record Punto(LocalDate fecha, long casos) {}

    public record Tendencia(String codigo, String descripcion, List<Punto> serie) {}
}
