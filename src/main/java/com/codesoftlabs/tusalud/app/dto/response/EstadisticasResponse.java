package com.codesoftlabs.tusalud.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Indicadores generales de la clínica para el panel de administración")
public record EstadisticasResponse(
        long totalPacientes,
        long totalMedicos,
        long totalEspecialidades,
        long citasHoy,
        long citasPendientes,
        BigDecimal ingresosMes,
        BigDecimal ingresosTotales,
        List<TopMedicoResponse> topMedicos,
        List<IngresoMesResponse> ingresosPorMes,
        List<CitasEspecialidadResponse> citasPorEspecialidad) {}
