package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.CumplirControlRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.EstadoPlanRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CronicosRequests.PlanRequest;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.PlanDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CronicosResponses.PlanResumenResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.cronicos.PlanesTratamientoService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/planes-tratamiento")
@RequiredArgsConstructor
@Validated
@Tag(name = "Planes de tratamiento", description = "Seguimiento de pacientes crónicos con controles y metas")
public class PlanesTratamientoController {

    private final PlanesTratamientoService servicio;

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Crear un plan de tratamiento con metas y controles programados")
    public ResponseEntity<PlanDetalleResponse> crear(
            @Valid @RequestBody PlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(actor, request));
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mis planes de tratamiento")
    public List<PlanResumenResponse> mios(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.mios(actor);
    }

    @GetMapping("/tablero")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Pacientes crónicos del médico ordenados por riesgo")
    public List<PlanResumenResponse> tablero(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.tablero(actor);
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Planes de un paciente con relación asistencial")
    public List<PlanResumenResponse> deUnPaciente(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.deUnPaciente(actor, pacienteId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Detalle de un plan con metas, controles y mediciones")
    public PlanDetalleResponse obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.obtener(actor, id);
    }

    @PostMapping("/{id}/mediciones")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Registrar una medición del paciente o del personal")
    public ResponseEntity<PlanDetalleResponse> medir(
            @PathVariable UUID id,
            @Valid @RequestBody MedicionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarMedicion(actor, id, request));
    }

    @PostMapping("/{id}/controles/{controlId}/cumplir")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Marcar un control como cumplido, con la cita opcional")
    public PlanDetalleResponse cumplir(
            @PathVariable UUID id,
            @PathVariable UUID controlId,
            @RequestBody(required = false) CumplirControlRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.cumplirControl(actor, id, controlId, request);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Completar, suspender o reactivar un plan")
    public PlanDetalleResponse estado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoPlanRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return servicio.cambiarEstado(actor, id, request);
    }
}
