package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.AjusteAsistenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConsolidarRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionManualRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionTerminalRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.TerminalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.AsistenciaDiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ConsolidacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MarcacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MarcacionTerminalResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ResumenAsistenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.TerminalCreadaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.TerminalResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.planilla.AsistenciaService;
import com.codesoftlabs.tusalud.app.services.planilla.TerminalesAsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/asistencia")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Asistencia del personal",
        description = "Marcaciones manuales y por terminal, consolidación diaria, tardanzas, faltas y horas extra")
public class AsistenciaController {

    private final AsistenciaService asistencia;
    private final TerminalesAsistenciaService terminales;

    @GetMapping("/marcaciones")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Marcaciones del personal en un rango")
    public ResponseEntity<List<MarcacionResponse>> marcaciones(
            @RequestParam(required = false) UUID empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(asistencia.listarMarcaciones(empleadoId, desde, hasta));
    }

    @PostMapping("/marcaciones")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Registrar una marcación manual con su motivo",
            description = "No admite fechas futuras ni meses con planilla aprobada.")
    public ResponseEntity<MarcacionResponse> registrarManual(
            @Valid @RequestBody MarcacionManualRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistencia.registrarManual(principal, request));
    }

    @PostMapping("/terminal/marcaciones")
    @Operation(
            summary = "Marcación desde una terminal de asistencia",
            description =
                    "Se autentica con el token de la terminal en X-Terminal-Token. El código de marcación identifica al empleado y la respuesta es uniforme ante datos desconocidos.")
    public ResponseEntity<MarcacionTerminalResponse> marcarEnTerminal(
            @RequestHeader(value = "X-Terminal-Token", required = false) String token,
            @Valid @RequestBody MarcacionTerminalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asistencia.marcarEnTerminal(token, request));
    }

    @GetMapping("/dias")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Asistencia consolidada por día")
    public ResponseEntity<List<AsistenciaDiaResponse>> dias(
            @RequestParam(required = false) UUID empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(asistencia.dias(empleadoId, desde, hasta));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Resumen de asistencia de un rango")
    public ResponseEntity<ResumenAsistenciaResponse> resumen(
            @RequestParam(required = false) UUID empleadoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(asistencia.resumen(empleadoId, desde, hasta));
    }

    @PostMapping("/consolidar")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Consolidar la asistencia desde turnos, ausencias aprobadas y marcaciones",
            description = "Los días ajustados a mano se conservan.")
    public ResponseEntity<ConsolidacionResponse> consolidar(
            @Valid @RequestBody ConsolidarRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(asistencia.consolidar(principal, request));
    }

    @PutMapping("/dias/{id}/ajuste")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Ajustar un día con su justificación",
            description = "Justificar una falta, corregir tardanza o minutos extra y elegir pago o compensación.")
    public ResponseEntity<AsistenciaDiaResponse> ajustar(
            @PathVariable UUID id,
            @Valid @RequestBody AjusteAsistenciaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(asistencia.ajustar(principal, id, request));
    }

    @GetMapping("/terminales")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Terminales de asistencia")
    public ResponseEntity<List<TerminalResponse>> terminales() {
        return ResponseEntity.ok(terminales.listar());
    }

    @PostMapping("/terminales")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Registrar una terminal", description = "El token se muestra una sola vez.")
    public ResponseEntity<TerminalCreadaResponse> crearTerminal(
            @Valid @RequestBody TerminalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(terminales.crear(principal, request));
    }

    @PatchMapping("/terminales/{id}/estado")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Activar o desactivar una terminal")
    public ResponseEntity<TerminalResponse> estadoTerminal(
            @PathVariable UUID id,
            @RequestParam boolean activo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(terminales.cambiarEstado(principal, id, activo));
    }

    @GetMapping("/mi-asistencia")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mi asistencia en un rango, solo la propia")
    public ResponseEntity<ResumenAsistenciaResponse> miAsistencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(asistencia.miAsistencia(principal, desde, hasta));
    }

    @GetMapping("/mis-marcaciones")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis marcaciones en un rango")
    public ResponseEntity<List<MarcacionResponse>> misMarcaciones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(asistencia.misMarcaciones(principal, desde, hasta));
    }
}
