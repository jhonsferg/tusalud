package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CompartirPacienteRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteCompartidoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PacienteCompartidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pacientes/{pacienteId:\\d+}")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Pacientes compartidos",
        description = "Un médico puede dar acceso a la historia clínica de uno de sus pacientes a otro médico")
public class PacienteCompartidoController {

    private final PacienteCompartidoService compartidoService;

    @PostMapping("/compartir")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Compartir un paciente",
            description =
                    "El médico que atiende al paciente da acceso a otro médico. El destinatario lo verá en sus pacientes y recibirá una notificación.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Paciente compartido"),
        @ApiResponse(
                responseCode = "403",
                description = "El médico no atiende al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Paciente o médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Ya compartido o médico inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PacienteCompartidoResponse> compartir(
            @Parameter(description = "ID del paciente", example = "1") @PathVariable @Positive Long pacienteId,
            @Valid @RequestBody CompartirPacienteRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compartidoService.compartir(pacienteId, request, principal));
    }

    @DeleteMapping("/compartir/{medicoId:\\d+}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(
            summary = "Revocar el acceso compartido",
            description = "Lo puede hacer el médico que compartió al paciente o un administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Acceso revocado"),
        @ApiResponse(
                responseCode = "403",
                description = "No es el médico de origen ni administrador",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No existe ese acceso compartido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> revocar(
            @Parameter(description = "ID del paciente", example = "1") @PathVariable @Positive Long pacienteId,
            @Parameter(description = "ID del médico con acceso", example = "3") @PathVariable @Positive Long medicoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        compartidoService.revocar(pacienteId, medicoId, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/compartidos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(
            summary = "Médicos con acceso compartido",
            description = "Lista los médicos con quienes se compartió el paciente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de accesos compartidos"),
        @ApiResponse(
                responseCode = "403",
                description = "Sin acceso al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Paciente no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PacienteCompartidoResponse>> listar(
            @Parameter(description = "ID del paciente", example = "1") @PathVariable @Positive Long pacienteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(compartidoService.listar(pacienteId, principal));
    }
}
