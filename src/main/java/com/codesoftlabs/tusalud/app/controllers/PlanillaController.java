package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.CalcularPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MotivoPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.PagarPlanillaRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ArchivoPlanillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.BoletaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MiBoletaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaAuditoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ResumenPlanillaResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.planilla.PlanillaAuditor;
import com.codesoftlabs.tusalud.app.services.planilla.PlanillaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planilla")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Planilla",
        description = "Cálculo mensual de planilla, boletas de pago y archivos de pago con segregación de funciones")
public class PlanillaController {

    private static final String CONSULTA = "hasAnyRole('RRHH', 'FINANZAS', 'GERENCIA')";

    private final PlanillaService planillaService;
    private final PlanillaAuditor auditor;

    @GetMapping("/resumen")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Resumen de la planilla", description = "Totales agregados, sin datos individuales")
    public ResponseEntity<ResumenPlanillaResponse> resumen() {
        return ResponseEntity.ok(planillaService.resumen());
    }

    @GetMapping("/planillas")
    @PreAuthorize(CONSULTA)
    @Operation(summary = "Listar las planillas mensuales")
    public ResponseEntity<List<PlanillaResponse>> listar() {
        return ResponseEntity.ok(planillaService.listar());
    }

    @GetMapping("/planillas/{id}")
    @PreAuthorize(CONSULTA)
    @Operation(
            summary = "Detalle de una planilla",
            description = "Las boletas individuales solo llegan a RRHH y finanzas, gerencia ve los totales.")
    public ResponseEntity<PlanillaDetalleResponse> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.obtener(principal, id));
    }

    @PostMapping("/planillas/calcular")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Calcular o recalcular la planilla de un mes",
            description =
                    "Consolida la asistencia, aplica contratos, conceptos y parámetros vigentes y genera las boletas.")
    public ResponseEntity<PlanillaResponse> calcular(
            @Valid @RequestBody CalcularPlanillaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planillaService.calcular(principal, request));
    }

    @PostMapping("/planillas/{id}/devolver")
    @PreAuthorize("hasRole('FINANZAS')")
    @Operation(summary = "Devolver una planilla calculada a borrador con su motivo")
    public ResponseEntity<PlanillaResponse> devolver(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoPlanillaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.rechazar(principal, id, request.motivo()));
    }

    @PostMapping("/planillas/{id}/aprobar")
    @PreAuthorize("hasRole('FINANZAS')")
    @Operation(
            summary = "Aprobar la planilla",
            description = "Quien calculó no puede aprobar. Genera el asiento contable de la planilla.")
    public ResponseEntity<PlanillaResponse> aprobar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.aprobar(principal, id));
    }

    @PostMapping("/planillas/{id}/pagar")
    @PreAuthorize("hasRole('FINANZAS')")
    @Operation(
            summary = "Registrar el pago de la planilla",
            description =
                    "Quien aprobó no puede pagar. Genera el asiento y el movimiento de tesorería y avisa a cada empleado.")
    public ResponseEntity<PlanillaResponse> pagar(
            @PathVariable UUID id,
            @Valid @RequestBody PagarPlanillaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.pagar(principal, id, request));
    }

    @PostMapping("/planillas/{id}/cerrar")
    @PreAuthorize("hasAnyRole('RRHH', 'GERENCIA')")
    @Operation(summary = "Cerrar una planilla pagada")
    public ResponseEntity<PlanillaResponse> cerrar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.cerrar(principal, id));
    }

    @GetMapping("/planillas/{id}/archivo-bancario")
    @PreAuthorize("hasRole('FINANZAS')")
    @Operation(summary = "Archivo CSV de abonos para el banco")
    public ResponseEntity<ArchivoPlanillaResponse> archivoBancario(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.archivoBancario(principal, id));
    }

    @GetMapping("/planillas/{id}/plame")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Archivo de texto para la declaración PLAME",
            description = "El formato está marcado por validar con el contador.")
    public ResponseEntity<ArchivoPlanillaResponse> plame(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.plame(principal, id));
    }

    @GetMapping("/boletas/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Boleta de pago",
            description = "RRHH y finanzas ven cualquiera, el empleado solo las suyas y ya pagadas.")
    public ResponseEntity<BoletaResponse> boleta(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.boleta(principal, id));
    }

    @GetMapping("/mis-boletas")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis boletas de pago ya pagadas")
    public ResponseEntity<List<MiBoletaResumenResponse>> misBoletas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(planillaService.misBoletas(principal));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('RRHH', 'FINANZAS')")
    @Operation(summary = "Bitácora de planilla, contratos, conceptos y asistencia")
    public ResponseEntity<List<PlanillaAuditoriaResponse>> auditoria(@RequestParam(required = false) String entidad) {
        return ResponseEntity.ok(auditor.recientes(entidad));
    }

    @GetMapping("/auditoria/{entidadId}")
    @PreAuthorize("hasAnyRole('RRHH', 'FINANZAS')")
    @Operation(summary = "Bitácora de un registro")
    public ResponseEntity<List<PlanillaAuditoriaResponse>> auditoriaDe(@PathVariable UUID entidadId) {
        return ResponseEntity.ok(auditor.deEntidad(entidadId));
    }
}
