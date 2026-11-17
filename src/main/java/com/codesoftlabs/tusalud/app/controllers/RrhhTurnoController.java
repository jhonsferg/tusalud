package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.TurnoPersonalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.SemanaTurnosResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoPersonalResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.security.Autorizaciones;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.TurnoPersonalService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rrhh")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recursos humanos - Turnos", description = "Plantillas de turno y asignación de turnos del personal")
public class RrhhTurnoController {

    private final TurnoPersonalService turnoService;

    @GetMapping("/plantillas-turno")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN')")
    @Operation(summary = "Plantillas de turno")
    public ResponseEntity<List<PlantillaTurnoResponse>> plantillas() {
        return ResponseEntity.ok(turnoService.plantillas());
    }

    @GetMapping("/turnos")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN')")
    @Operation(summary = "Turnos asignados", description = "Rango máximo de 62 días.")
    public ResponseEntity<List<TurnoPersonalResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) AreaLaboral area,
            @RequestParam(required = false) UUID empleadoId) {
        return ResponseEntity.ok(turnoService.listar(desde, hasta, area, empleadoId));
    }

    @GetMapping("/turnos/semana")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN')")
    @Operation(
            summary = "Vista semanal por área",
            description = "De lunes a domingo, con las ausencias aprobadas de cada día.")
    public ResponseEntity<SemanaTurnosResponse> semana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) AreaLaboral area) {
        return ResponseEntity.ok(turnoService.semana(fecha, area));
    }

    @PostMapping("/turnos")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Asignar un turno",
            description =
                    "Responde 409 si el turno se cruza con otro, supera 48 horas semanales o cae sobre una ausencia.")
    public ResponseEntity<TurnoPersonalResponse> asignar(
            @Valid @RequestBody TurnoPersonalRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.asignar(request, principal));
    }

    @DeleteMapping("/turnos/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Quitar un turno futuro")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        turnoService.eliminar(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-turnos")
    @PreAuthorize(Autorizaciones.PERSONAL)
    @Operation(summary = "Mis turnos", description = "Los de los próximos 14 días si no se indica rango.")
    public ResponseEntity<List<TurnoPersonalResponse>> misTurnos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(turnoService.misTurnos(principal, desde, hasta));
    }
}
