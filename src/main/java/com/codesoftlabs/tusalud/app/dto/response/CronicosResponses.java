package com.codesoftlabs.tusalud.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CronicosResponses {

    private CronicosResponses() {}

    public record MetaResponse(String parametro, BigDecimal valorMin, BigDecimal valorMax, String unidad) {}

    public record ControlResponse(
            UUID id, int numero, LocalDate fechaProgramada, String estado, boolean vencido, UUID citaId) {}

    public record MedicionResponse(
            UUID id,
            int numero,
            String parametro,
            BigDecimal valor,
            LocalDateTime medidoEn,
            String origen,
            boolean fueraDeRango) {}

    public record PlanResumenResponse(
            UUID id,
            String paciente,
            String programa,
            String estado,
            LocalDate inicio,
            int adherencia,
            int controlesVencidos,
            int medicionesFueraDeRango,
            String riesgo) {}

    public record PlanDetalleResponse(
            UUID id,
            UUID pacienteId,
            String paciente,
            String medico,
            String programa,
            String diagnosticoCodigo,
            String objetivos,
            String intervenciones,
            String medicacion,
            UUID recetaId,
            String estado,
            LocalDate inicio,
            LocalDate fin,
            int frecuenciaControlDias,
            int adherencia,
            String riesgo,
            List<MetaResponse> metas,
            List<ControlResponse> controles,
            List<MedicionResponse> mediciones) {}
}
