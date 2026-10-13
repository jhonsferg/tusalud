package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Citas", description = "Registro, consulta y cambios de estado de citas médicas")
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Registrar cita",
            description =
                    "Registra una cita en estado PENDIENTE para el paciente autenticado. Valida que el médico atienda en ese horario y que no existan solapamientos con otras citas del médico o del paciente. Requiere rol PACIENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cita registrada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos o fuera del horario de atención del médico",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El médico o el paciente ya tienen una cita en ese horario",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> crear(
            @Valid @RequestBody CitaCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        CitaResponse creada = citaService.crearCita(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/mis-citas")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Mis citas",
            description =
                    "Devuelve las citas del paciente autenticado entre dos fechas (inclusive). Por defecto, desde hoy hasta dentro de 30 días. Requiere rol PACIENTE.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de citas del paciente"),
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
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CitaResponse>> misCitas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Fecha inicial (yyyy-MM-dd); por defecto hoy", example = "2026-10-01")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Fecha final (yyyy-MM-dd); por defecto hoy + 30 días", example = "2026-10-31")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin) {
        return ResponseEntity.ok(citaService.listarMisCitas(principal, inicio, fin));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Obtener cita",
            description =
                    "Devuelve el detalle de una cita. Visible para el paciente dueño, el médico de la cita o un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita encontrada"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "La cita no pertenece al usuario autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> obtener(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(citaService.obtenerCita(id, principal));
    }

    @PutMapping("/{id:\\d+}/cancelar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN')")
    @Operation(
            summary = "Cancelar cita",
            description =
                    "Cambia el estado de una cita PENDIENTE o CONFIRMADA a CANCELADA. Permitido al paciente dueño o a un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita cancelada"),
        @ApiResponse(
                responseCode = "400",
                description = "La cita no se puede cancelar en su estado actual",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para cancelar esta cita",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> cancelar(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(citaService.cancelarCita(id, principal));
    }

    @PutMapping("/{id:\\d+}/confirmar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(
            summary = "Confirmar cita",
            description =
                    "Cambia el estado de una cita PENDIENTE a CONFIRMADA. Permitido al médico de la cita o a un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita confirmada"),
        @ApiResponse(
                responseCode = "400",
                description = "La cita no se puede confirmar en su estado actual",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para confirmar esta cita",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> confirmar(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(citaService.confirmarCita(id, principal));
    }

    @PutMapping("/{id:\\d+}/atender")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Marcar cita como atendida",
            description = "Cambia el estado de una cita PENDIENTE o CONFIRMADA a ATENDIDA. Solo el médico de la cita.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita atendida"),
        @ApiResponse(
                responseCode = "400",
                description = "La cita no se puede atender en su estado actual",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Solo el médico de la cita puede atenderla",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> atender(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(citaService.atenderCita(id, principal));
    }
}
