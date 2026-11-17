package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.AusenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResolverAusenciaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AusenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.MisAusenciasResponse;
import com.codesoftlabs.tusalud.app.dto.response.SaldoVacacionesResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.security.Autorizaciones;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AusenciaService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rrhh")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recursos humanos - Ausencias", description = "Solicitudes de ausencia, permisos, vacaciones y su saldo")
public class RrhhAusenciaController {

    private final AusenciaService ausenciaService;

    @GetMapping("/ausencias")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN')")
    @Operation(
            summary = "Calendario de ausencias",
            description = "Los motivos solo llegan a RRHH. Rango máximo de 366 días.")
    public ResponseEntity<List<AusenciaResponse>> listar(
            @RequestParam(required = false) EstadoAusencia estado,
            @RequestParam(required = false) AreaLaboral area,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.listar(estado, area, desde, hasta, principal));
    }

    @PutMapping("/ausencias/{id}/aprobar")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Aprobar una solicitud", description = "No se puede resolver la propia solicitud.")
    public ResponseEntity<AusenciaResponse> aprobar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ResolverAusenciaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.aprobar(id, request == null ? null : request.motivo(), principal));
    }

    @PutMapping("/ausencias/{id}/rechazar")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Rechazar una solicitud", description = "Exige el motivo del rechazo.")
    public ResponseEntity<AusenciaResponse> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ResolverAusenciaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.rechazar(id, request == null ? null : request.motivo(), principal));
    }

    @PutMapping("/ausencias/{id}/cancelar")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Cancelar una ausencia solicitada o aprobada que aún no terminó")
    public ResponseEntity<AusenciaResponse> cancelar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.cancelar(id, principal));
    }

    @GetMapping("/empleados/{id}/saldo-vacaciones")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Saldo de vacaciones de un empleado")
    public ResponseEntity<SaldoVacacionesResponse> saldo(@PathVariable UUID id) {
        return ResponseEntity.ok(ausenciaService.saldo(id));
    }

    @GetMapping("/mis-ausencias")
    @PreAuthorize(Autorizaciones.PERSONAL)
    @Operation(summary = "Mis ausencias y mi saldo de vacaciones")
    public ResponseEntity<MisAusenciasResponse> misAusencias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.misAusencias(principal));
    }

    @PostMapping("/mis-ausencias")
    @PreAuthorize(Autorizaciones.PERSONAL)
    @Operation(
            summary = "Solicitar una ausencia",
            description =
                    "Requiere ficha laboral. Responde 409 si se cruza con otra ausencia o si las vacaciones superan el saldo.")
    public ResponseEntity<AusenciaResponse> solicitar(
            @Valid @RequestBody AusenciaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ausenciaService.solicitar(principal, request));
    }

    @PutMapping("/mis-ausencias/{id}/cancelar")
    @PreAuthorize(Autorizaciones.PERSONAL)
    @Operation(summary = "Cancelar mi solicitud pendiente")
    public ResponseEntity<AusenciaResponse> cancelarPropia(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(ausenciaService.cancelarPropia(principal, id));
    }
}
