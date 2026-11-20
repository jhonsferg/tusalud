package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ActualizarCuentaContableRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.AsientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaContableRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.MotivoContableRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.PeriodoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.PlantillaAsientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoPendienteResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResumen;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ContabilidadAuditoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaContableResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PeriodoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PlantillaAsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ResumenContabilidad;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.contabilidad.AsientoService;
import com.codesoftlabs.tusalud.app.services.contabilidad.AsientosPendientesService;
import com.codesoftlabs.tusalud.app.services.contabilidad.ContabilidadAuditor;
import com.codesoftlabs.tusalud.app.services.contabilidad.PeriodoContableService;
import com.codesoftlabs.tusalud.app.services.contabilidad.PlanContableService;
import com.codesoftlabs.tusalud.app.services.contabilidad.PlantillaAsientoService;
import com.codesoftlabs.tusalud.app.services.contabilidad.ResumenContabilidadService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/contabilidad")
@RequiredArgsConstructor
@Validated
@Tag(name = "Contabilidad", description = "Plan contable, periodos, asientos, plantillas y auditoría")
public class ContabilidadController {

    private static final String FINANZAS = "hasRole('FINANZAS')";
    private static final String CONSULTA = "hasAnyRole('FINANZAS', 'GERENCIA')";
    private static final String CIERRE = "hasAnyRole('FINANZAS', 'GERENCIA')";
    private static final String GERENCIA = "hasRole('GERENCIA')";

    private final PlanContableService plan;
    private final PeriodoContableService periodos;
    private final AsientoService asientos;
    private final PlantillaAsientoService plantillas;
    private final AsientosPendientesService pendientes;
    private final ResumenContabilidadService resumen;
    private final ContabilidadAuditor auditor;

    @GetMapping("/resumen")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<ResumenContabilidad> resumen() {
        return ResponseEntity.ok(resumen.resumen());
    }

    @GetMapping("/cuentas")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<CuentaContableResponse>> cuentas(
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        return ResponseEntity.ok(plan.listar(soloActivas));
    }

    @PostMapping("/cuentas")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<CuentaContableResponse> crearCuenta(
            @Valid @RequestBody CuentaContableRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plan.crear(actor, request));
    }

    @PutMapping("/cuentas/{id}")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<CuentaContableResponse> actualizarCuenta(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarCuentaContableRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(plan.actualizar(actor, id, request));
    }

    @GetMapping("/periodos")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<PeriodoResponse>> periodos() {
        return ResponseEntity.ok(periodos.listar());
    }

    @PostMapping("/periodos")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<PeriodoResponse> abrirPeriodo(
            @Valid @RequestBody PeriodoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(periodos.abrir(actor, request.anio(), request.mes()));
    }

    @PostMapping("/periodos/{id}/cerrar")
    @PreAuthorize(CIERRE)
    public ResponseEntity<PeriodoResponse> cerrarPeriodo(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(periodos.cerrar(actor, id));
    }

    @PostMapping("/periodos/{id}/reabrir")
    @PreAuthorize(GERENCIA)
    public ResponseEntity<PeriodoResponse> reabrirPeriodo(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoContableRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(periodos.reabrir(actor, id, request.motivo()));
    }

    @GetMapping("/asientos")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<AsientoResumen>> asientos(
            @RequestParam(required = false) UUID periodoId,
            @RequestParam(required = false) EstadoAsiento estado,
            @RequestParam(required = false) OrigenAsiento origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(asientos.listar(periodoId, estado, origen, desde, hasta, q));
    }

    @GetMapping("/asientos/{id}")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<AsientoResponse> asiento(@PathVariable UUID id) {
        return ResponseEntity.ok(asientos.obtener(id));
    }

    @PostMapping("/asientos")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<AsientoResponse> crearAsiento(
            @Valid @RequestBody AsientoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asientos.crear(actor, request));
    }

    @PutMapping("/asientos/{id}")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<AsientoResponse> actualizarAsiento(
            @PathVariable UUID id,
            @Valid @RequestBody AsientoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(asientos.actualizar(actor, id, request));
    }

    @DeleteMapping("/asientos/{id}")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<Void> eliminarAsiento(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        asientos.eliminar(actor, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/asientos/{id}/contabilizar")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<AsientoResponse> contabilizar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(asientos.contabilizar(actor, id));
    }

    @PostMapping("/asientos/{id}/reversar")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<AsientoResponse> reversar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoContableRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(asientos.reversar(actor, id, request.motivo()));
    }

    @GetMapping("/plantillas")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<PlantillaAsientoResponse>> plantillas() {
        return ResponseEntity.ok(plantillas.listar());
    }

    @PutMapping("/plantillas/{id}")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<PlantillaAsientoResponse> actualizarPlantilla(
            @PathVariable UUID id,
            @Valid @RequestBody PlantillaAsientoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(plantillas.actualizar(actor, id, request));
    }

    @GetMapping("/pendientes")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<AsientoPendienteResponse>> pendientes(
            @RequestParam(defaultValue = "true") boolean soloAbiertos) {
        return ResponseEntity.ok(pendientes.listar(soloAbiertos));
    }

    @PostMapping("/pendientes/{id}/reprocesar")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<AsientoPendienteResponse> reprocesar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(pendientes.reprocesar(actor, id));
    }

    @GetMapping("/auditoria")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ContabilidadAuditoriaResponse>> auditoria(
            @RequestParam(required = false) String entidad, @RequestParam(required = false) UUID entidadId) {
        return ResponseEntity.ok(entidadId == null ? auditor.recientes(entidad) : auditor.deEntidad(entidadId));
    }
}
