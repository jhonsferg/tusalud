package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/historia-clinica")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PACIENTE')")
@Tag(name = "Historia clínica", description = "Historia clínica del paciente autenticado")
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaClinicaService;

    @GetMapping
    @Operation(
            summary = "Mi historia clínica",
            description = "Devuelve los datos del paciente y todas sus atenciones, las más recientes primero.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historia clínica"),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HistoriaClinicaResponse> miHistoria(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.obtenerMia(principal));
    }

    @PutMapping("/datos-medicos")
    @Operation(
            summary = "Actualizar datos médicos",
            description = "Actualiza grupo sanguíneo, alergias y antecedentes del paciente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos médicos actualizados"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos (grupo sanguíneo no permitido o texto demasiado largo)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PacienteHistoriaResponse> actualizarDatosMedicos(
            @Valid @RequestBody DatosMedicosRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.actualizarDatosMedicos(principal, request));
    }
}
