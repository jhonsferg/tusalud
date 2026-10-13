package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auditoria")
@RequiredArgsConstructor
@Validated
@Tag(name = "Administración - Auditoría", description = "Reportes de auditoría de citas (requiere rol ADMIN)")
public class AdminController {

    private final AuditoriaService auditoriaService;

    @GetMapping("/citas-por-estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Citas por estado",
            description =
                    "Devuelve la cantidad de citas agrupadas por estado (PENDIENTE, CONFIRMADA, CANCELADA, ATENDIDA).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Conteo de citas por estado"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CitasPorEstadoResponse>> citasPorEstado() {
        return ResponseEntity.ok(auditoriaService.contarCitasPorEstado());
    }

    @GetMapping("/citas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar citas por rango de fechas",
            description =
                    "Devuelve todas las citas entre dos fechas (inclusive). Por defecto, desde hace 30 días hasta dentro de 30 días.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de citas"),
        @ApiResponse(
                responseCode = "400",
                description = "Fechas con formato inválido (yyyy-MM-dd) o rango incorrecto",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CitaResponse>> listarCitas(
            @Parameter(description = "Fecha inicial (yyyy-MM-dd); por defecto hoy - 30 días", example = "2026-10-01")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Fecha final (yyyy-MM-dd); por defecto hoy + 30 días", example = "2026-10-31")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin) {
        return ResponseEntity.ok(auditoriaService.listarCitas(inicio, fin));
    }
}
