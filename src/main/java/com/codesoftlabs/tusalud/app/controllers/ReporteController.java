package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ReporteCalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteEjecutivoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteOperativoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.services.interfaces.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Tag(
        name = "Reportes y BI",
        description = "Reportes agregados de solo lectura, sin datos de pacientes ni datos clínicos")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/ejecutivo")
    @PreAuthorize("hasAnyRole('GERENCIA', 'ADMIN')")
    @Operation(
            summary = "Reporte ejecutivo",
            description =
                    "Indicadores del periodo contra el periodo anterior de igual duración y serie por día, semana o mes. Rango máximo de 366 días.")
    public ResponseEntity<ReporteEjecutivoResponse> ejecutivo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AgrupacionReporte agrupacion) {
        return ResponseEntity.ok(reporteService.ejecutivo(desde, hasta, agrupacion));
    }

    @GetMapping("/financiero")
    @PreAuthorize("hasAnyRole('FINANZAS', 'GERENCIA', 'ADMIN')")
    @Operation(
            summary = "Reporte financiero",
            description =
                    "Ingresos por periodo, método, origen, especialidad y médico, comprobantes, diferencias de caja y cuentas por cobrar.")
    public ResponseEntity<ReporteFinancieroResponse> financiero(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AgrupacionReporte agrupacion) {
        return ResponseEntity.ok(reporteService.financiero(desde, hasta, agrupacion));
    }

    @GetMapping("/operativo")
    @PreAuthorize("hasAnyRole('OPERACIONES', 'GERENCIA', 'ADMIN')")
    @Operation(
            summary = "Reporte operativo",
            description =
                    "Ocupación de camas, estancias, esperas de turnos, emergencias, demanda de citas, uso de la agenda e inventario crítico.")
    public ResponseEntity<ReporteOperativoResponse> operativo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AgrupacionReporte agrupacion) {
        return ResponseEntity.ok(reporteService.operativo(desde, hasta, agrupacion));
    }

    @GetMapping("/calidad")
    @PreAuthorize("hasAnyRole('CALIDAD', 'GERENCIA', 'ADMIN')")
    @Operation(
            summary = "Reporte de calidad",
            description =
                    "Valoraciones por médico y especialidad, cancelaciones, ausencias, entrega de resultados de laboratorio y anulaciones.")
    public ResponseEntity<ReporteCalidadResponse> calidad(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AgrupacionReporte agrupacion) {
        return ResponseEntity.ok(reporteService.calidad(desde, hasta, agrupacion));
    }
}
