package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Historia clínica - Médicos y pacientes",
        description = "Consulta de historias clínicas por parte del personal")
public class PacienteHistoriaController {

    private final HistoriaClinicaService historiaClinicaService;

    @GetMapping("/pacientes/{pacienteId:\\d+}/historia-clinica")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(
            summary = "Historia clínica de un paciente",
            description =
                    "Disponible para ADMIN y para médicos que atienden al paciente o a quienes otro médico se lo compartió.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historia clínica"),
        @ApiResponse(
                responseCode = "403",
                description = "El médico no tiene citas con el paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Paciente no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HistoriaClinicaResponse> historiaDePaciente(
            @Parameter(description = "ID del paciente", example = "1") @PathVariable Long pacienteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.obtenerDePaciente(pacienteId, principal));
    }

    @GetMapping("/medicos/me/pacientes")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Mis pacientes",
            description =
                    "Pacientes que tienen citas con el médico autenticado, con su última cita y el total de citas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de pacientes"),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol MEDICO",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PacienteMedicoResponse>> misPacientes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.listarPacientesDeMedico(principal));
    }
}
