package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.CambioPiezaRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ConsentimientoItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ItemPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.MotivoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PagoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.ProcedimientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.OdontologiaRequests.SesionRequest;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.CambioOdontogramaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.OdontogramaResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PlanResponse;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.PlanResumen;
import com.codesoftlabs.tusalud.app.dto.response.OdontologiaResponses.ProcedimientoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.odontologia.OdontogramaService;
import com.codesoftlabs.tusalud.app.services.odontologia.PlanesOdontologicosService;
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
@RequestMapping("/api/v1/odontologia")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Odontología",
        description = "Odontograma con historial inmutable, planes de tratamiento, presupuesto, sesiones y cobros")
public class OdontologiaController {

    private final OdontogramaService odontograma;
    private final PlanesOdontologicosService planes;

    @GetMapping("/procedimientos")
    @PreAuthorize("hasAnyRole('MEDICO', 'CAJA', 'ADMIN')")
    public ResponseEntity<List<ProcedimientoResponse>> procedimientos(
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(odontograma.procedimientos(soloActivos));
    }

    @PostMapping("/procedimientos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProcedimientoResponse> crearProcedimiento(@Valid @RequestBody ProcedimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontograma.crearProcedimiento(request));
    }

    @PutMapping("/procedimientos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProcedimientoResponse> actualizarProcedimiento(
            @PathVariable UUID id, @Valid @RequestBody ProcedimientoRequest request) {
        return ResponseEntity.ok(odontograma.actualizarProcedimiento(id, request));
    }

    @GetMapping("/pacientes/{pacienteId}/odontograma")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<OdontogramaResponse> obtener(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(odontograma.obtener(actor, pacienteId));
    }

    @GetMapping("/mio/odontograma")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<OdontogramaResponse> mio(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(odontograma.mio(actor));
    }

    @GetMapping("/pacientes/{pacienteId}/odontograma/historial")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<List<CambioOdontogramaResponse>> historial(
            @PathVariable UUID pacienteId,
            @RequestParam(required = false) Integer pieza,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(odontograma.historial(actor, pacienteId, pieza));
    }

    @PostMapping("/pacientes/{pacienteId}/odontograma/cambios")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CambioOdontogramaResponse> cambio(
            @PathVariable UUID pacienteId,
            @Valid @RequestBody CambioPiezaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(odontograma.registrarCambio(actor, pacienteId, request));
    }

    @GetMapping("/pacientes/{pacienteId}/planes")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<List<PlanResumen>> planesDelPaciente(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.delPaciente(actor, pacienteId));
    }

    @GetMapping("/mios/planes")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<PlanResumen>> planesMios(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.mios(actor));
    }

    @GetMapping("/planes/por-cobrar")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<List<PlanResumen>> porCobrar() {
        return ResponseEntity.ok(planes.porCobrar());
    }

    @PostMapping("/planes")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> crearPlan(
            @Valid @RequestBody PlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planes.crear(actor, request));
    }

    @GetMapping("/planes/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<PlanResponse> plan(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.obtener(actor, id));
    }

    @PostMapping("/planes/{id}/items")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> agregarItem(
            @PathVariable UUID id,
            @Valid @RequestBody ItemPlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planes.agregarItem(actor, id, request));
    }

    @DeleteMapping("/planes/{id}/items/{itemId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> quitarItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.quitarItem(actor, id, itemId));
    }

    @PutMapping("/planes/{id}/items/{itemId}/consentimiento")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> vincularConsentimiento(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody ConsentimientoItemRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.vincularConsentimiento(actor, id, itemId, request.consentimientoId()));
    }

    @PostMapping("/planes/{id}/presentar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> presentar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.presentar(actor, id));
    }

    @PostMapping("/planes/{id}/aceptar")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PlanResponse> aceptar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.responder(actor, id, true, null));
    }

    @PostMapping("/planes/{id}/rechazar")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PlanResponse> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoPlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.responder(actor, id, false, request.motivo()));
    }

    @PostMapping("/planes/{id}/cancelar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoPlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(planes.cancelar(actor, id, request.motivo()));
    }

    @PostMapping("/planes/{id}/sesiones")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PlanResponse> sesion(
            @PathVariable UUID id,
            @Valid @RequestBody SesionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planes.registrarSesion(actor, id, request));
    }

    @PostMapping("/planes/{id}/pagos")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<PlanResponse> pago(
            @PathVariable UUID id,
            @Valid @RequestBody PagoPlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planes.registrarPago(actor, id, request));
    }
}
