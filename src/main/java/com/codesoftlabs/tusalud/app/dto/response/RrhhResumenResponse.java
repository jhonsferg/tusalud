package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RrhhResumenResponse(
        long totalEmpleados,
        long activos,
        long enLicencia,
        long cesados,
        long solicitudesPendientes,

        @io.swagger.v3.oas.annotations.media.Schema(description = "Solo para RRHH y GERENCIA")
        BigDecimal masaSalarial,

        List<PorArea> porArea,
        List<ContratoPorVencer> contratosPorVencer,
        long ausentesHoy,
        List<AusenteHoy> ausentesHoyDetalle) {

    public record PorArea(AreaLaboral area, long total) {}

    public record ContratoPorVencer(
            UUID empleadoId, String nombreCompleto, String cargo, LocalDate fechaFin, long diasRestantes) {}

    public record AusenteHoy(
            UUID empleadoId, String nombreCompleto, AreaLaboral area, TipoAusencia tipo, LocalDate hasta) {}
}
