package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DatosMedicosRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.HistoriaClinicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteHistoriaResponse;
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
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/pacientes/{pacienteId}/historia-clinica")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Historia clínica de un paciente",
            description =
                    "Disponible solo para médicos que atienden al paciente, lo tienen hospitalizado o a quienes otro médico se lo compartió. Cada consulta queda auditada.")
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
            @Parameter(description = "ID del paciente", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID pacienteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.obtenerDePaciente(pacienteId, principal));
    }

    @PutMapping("/pacientes/{pacienteId}/datos-medicos")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Actualizar datos médicos de un paciente",
            description =
                    "Actualiza grupo sanguíneo, alergias y antecedentes. Solo el personal médico con relación asistencial con el paciente puede hacerlo y el cambio queda auditado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos médicos actualizados"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "El médico no tiene relación asistencial con el paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PacienteHistoriaResponse> actualizarDatosMedicos(
            @Parameter(description = "ID del paciente", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID pacienteId,
            @Valid @RequestBody DatosMedicosRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(historiaClinicaService.actualizarDatosMedicos(pacienteId, principal, request));
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
