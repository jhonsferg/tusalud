package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/citas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Atención médica", description = "Registro de la atención clínica de una cita")
public class AtencionController {

    private final AtencionService atencionService;

    @PostMapping("/{id:\\d+}/atencion")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Registrar atención",
            description =
                    "Registra el diagnóstico, receta y signos vitales de una cita. Marca la cita como ATENDIDA y notifica al paciente. Solo el médico de la cita.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Atención registrada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Solo el médico de la cita puede registrar la atención",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La cita ya tiene una atención registrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AtencionResponse> registrar(
            @Parameter(description = "ID de la cita", example = "10") @PathVariable Long id,
            @Valid @RequestBody AtencionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atencionService.registrar(id, request, principal));
    }

    @PutMapping("/{id:\\d+}/atencion")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Editar atención",
            description = "Actualiza la atención registrada. Solo el médico de la cita.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atención actualizada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Solo el médico de la cita puede editar la atención",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Atención no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AtencionResponse> actualizar(
            @Parameter(description = "ID de la cita", example = "10") @PathVariable Long id,
            @Valid @RequestBody AtencionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(atencionService.actualizar(id, request, principal));
    }

    @GetMapping("/{id:\\d+}/atencion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Obtener atención",
            description = "Visible para el paciente dueño de la cita, el médico de la cita o un ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Atención encontrada"),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para ver la atención",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita o atención no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AtencionResponse> obtener(
            @Parameter(description = "ID de la cita", example = "10") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(atencionService.obtener(id, principal));
    }
}
