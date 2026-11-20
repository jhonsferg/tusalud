package com.codesoftlabs.tusalud.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CalidadResponses {

    private CalidadResponses() {}

    public record CausaResponse(int posicion, String categoria, String texto) {}

    public record AccionResponse(
            UUID id,
            String descripcion,
            UUID responsableId,
            String responsable,
            LocalDate fechaLimite,
            String estado,
            boolean vencida,
            LocalDateTime completadaEn) {}

    public record EventoResumenResponse(
            UUID id,
            String codigo,
            String tipo,
            String severidad,
            String area,
            String estado,
            boolean anonimo,
            String notificante,
            LocalDateTime ocurridoEn,
            LocalDateTime notificadoEn,
            int accionesPendientes) {}

    public record EventoDetalleResponse(
            UUID id,
            String codigo,
            String tipo,
            String severidad,
            String area,
            String estado,
            boolean anonimo,
            String notificante,
            String descripcion,
            String paciente,
            UUID cirugiaId,
            UUID dispensacionId,
            String clasificacion,
            String analisisMetodo,
            String analisisConclusion,
            List<CausaResponse> causas,
            List<AccionResponse> acciones,
            LocalDateTime ocurridoEn,
            LocalDateTime notificadoEn,
            LocalDateTime cerradoEn,
            String cerradoPor,
            String verificacion,
            Boolean accionEficaz) {}

    public record TableroResponse(
            int total,
            Map<String, Long> porEstado,
            Map<String, Long> porSeveridad,
            Map<String, Long> porArea,
            Map<String, Long> porMes,
            long accionesVencidas,
            double diasMedioCierre,
            long casiIncidentes,
            long eventosAdversos) {}

    public record GuiaResumenResponse(
            UUID id,
            String codigo,
            int version,
            String titulo,
            String especialidad,
            String resumen,
            String estado,
            LocalDate vigenteDesde,
            LocalDate vigenteHasta,
            boolean porValidar,
            List<String> codigosCie10) {}

    public record GuiaDetalleResponse(
            GuiaResumenResponse guia, String contenido, List<GuiaResumenResponse> versiones) {}
}
