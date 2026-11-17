package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ValoracionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValoracionesMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ValoracionService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Valoraciones", description = "Opiniones de los pacientes sobre la atención recibida")
public class ValoracionController {

    private final ValoracionService valoracionService;

    @PostMapping("/citas/{id:\\d+}/valoracion")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Valorar una cita atendida",
            description =
                    "El paciente dueño califica de 1 a 5 la atención recibida. Solo se permite una valoración por cita.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Valoración registrada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "La cita no pertenece al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La cita no está atendida o ya fue valorada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ValoracionResponse> valorar(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Valid @RequestBody ValoracionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        ValoracionResponse creada = valoracionService.valorar(id, request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/citas/{id:\\d+}/valoracion")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO', 'ADMIN')")
    @Operation(
            summary = "Obtener la valoración de una cita",
            description = "Permitido al paciente dueño, al médico de la cita y a un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Valoración de la cita"),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para ver esta valoración",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "La cita no existe o no tiene valoración",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ValoracionResponse> obtener(
            @Parameter(description = "ID de la cita", example = "1") @PathVariable @Positive Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(valoracionService.obtenerDeCita(id, principal));
    }

    @GetMapping("/medicos/{id:\\d+}/valoraciones")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar las valoraciones de un médico",
            description = "Devuelve el promedio redondeado a un decimal, el total y el detalle de cada valoración.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Valoraciones del médico"),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ValoracionesMedicoResponse> listar(
            @Parameter(description = "ID del médico", example = "1") @PathVariable @Positive Long id) {
        return ResponseEntity.ok(valoracionService.listarDeMedico(id));
    }
}
