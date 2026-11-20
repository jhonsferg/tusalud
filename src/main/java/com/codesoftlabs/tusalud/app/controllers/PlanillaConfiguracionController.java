package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ContratoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.EmpleadoConceptoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.FinalizarContratoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ParametroRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ConceptoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ContratoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.EmpleadoConceptoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ParametroResponse;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoContrato;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.planilla.ConceptosPlanillaService;
import com.codesoftlabs.tusalud.app.services.planilla.ContratosLaboralesService;
import com.codesoftlabs.tusalud.app.services.planilla.ParametrosPlanillaService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planilla")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Planilla - Configuración",
        description = "Contratos laborales, conceptos y parámetros del régimen peruano por vigencia")
public class PlanillaConfiguracionController {

    private final ContratosLaboralesService contratos;
    private final ConceptosPlanillaService conceptos;
    private final ParametrosPlanillaService parametros;

    @GetMapping("/parametros")
    @PreAuthorize("hasAnyRole('RRHH', 'FINANZAS', 'GERENCIA')")
    @Operation(
            summary = "Parámetros de planilla",
            description =
                    "Con vigentes=true solo los vigentes hoy, de lo contrario el historial. Todos están por validar con el contador.")
    public ResponseEntity<List<ParametroResponse>> listarParametros(
            @RequestParam(required = false) String codigo, @RequestParam(defaultValue = "true") boolean vigentes) {
        return ResponseEntity.ok(parametros.listar(codigo, vigentes));
    }

    @PostMapping("/parametros")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Crear una nueva versión de un parámetro desde el primer día de un mes")
    public ResponseEntity<ParametroResponse> nuevoParametro(
            @Valid @RequestBody ParametroRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parametros.nuevaVersion(principal, request));
    }

    @PostMapping("/parametros/{id}/validar")
    @PreAuthorize("hasRole('FINANZAS')")
    @Operation(summary = "Marcar un parámetro como validado por el contador")
    public ResponseEntity<ParametroResponse> validarParametro(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(parametros.validar(principal, id));
    }

    @GetMapping("/conceptos")
    @PreAuthorize("hasAnyRole('RRHH', 'FINANZAS')")
    @Operation(summary = "Conceptos de planilla")
    public ResponseEntity<List<ConceptoResponse>> listarConceptos() {
        return ResponseEntity.ok(conceptos.listar());
    }

    @PostMapping("/conceptos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Crear un concepto de ingreso o descuento")
    public ResponseEntity<ConceptoResponse> crearConcepto(
            @Valid @RequestBody ConceptoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conceptos.crear(principal, request));
    }

    @PutMapping("/conceptos/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Actualizar un concepto configurable")
    public ResponseEntity<ConceptoResponse> actualizarConcepto(
            @PathVariable UUID id,
            @Valid @RequestBody ConceptoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(conceptos.actualizar(principal, id, request));
    }

    @GetMapping("/contratos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Contratos laborales")
    public ResponseEntity<List<ContratoResponse>> listarContratos(
            @RequestParam(required = false) UUID empleadoId, @RequestParam(required = false) EstadoContrato estado) {
        return ResponseEntity.ok(contratos.listar(empleadoId, estado));
    }

    @GetMapping("/contratos/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Detalle de un contrato")
    public ResponseEntity<ContratoResponse> obtenerContrato(@PathVariable UUID id) {
        return ResponseEntity.ok(contratos.obtener(id));
    }

    @PostMapping("/contratos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Registrar un contrato laboral")
    public ResponseEntity<ContratoResponse> crearContrato(
            @Valid @RequestBody ContratoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratos.crear(principal, request));
    }

    @PutMapping("/contratos/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Actualizar un contrato vigente")
    public ResponseEntity<ContratoResponse> actualizarContrato(
            @PathVariable UUID id,
            @Valid @RequestBody ContratoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(contratos.actualizar(principal, id, request));
    }

    @PostMapping("/contratos/{id}/finalizar")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Finalizar un contrato")
    public ResponseEntity<ContratoResponse> finalizarContrato(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizarContratoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(contratos.finalizar(principal, id, request));
    }

    @GetMapping("/empleados/{empleadoId}/conceptos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Conceptos recurrentes asignados a un empleado")
    public ResponseEntity<List<EmpleadoConceptoResponse>> conceptosDeEmpleado(@PathVariable UUID empleadoId) {
        return ResponseEntity.ok(conceptos.deEmpleado(empleadoId));
    }

    @PostMapping("/empleados/{empleadoId}/conceptos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Asignar un concepto recurrente a un empleado")
    public ResponseEntity<EmpleadoConceptoResponse> asignarConcepto(
            @PathVariable UUID empleadoId,
            @Valid @RequestBody EmpleadoConceptoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conceptos.asignar(principal, empleadoId, request));
    }

    @DeleteMapping("/empleados/{empleadoId}/conceptos/{asignacionId}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Quitar un concepto recurrente de un empleado")
    public ResponseEntity<Void> quitarConcepto(
            @PathVariable UUID empleadoId,
            @PathVariable UUID asignacionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        conceptos.quitar(principal, empleadoId, asignacionId);
        return ResponseEntity.noContent().build();
    }
}
