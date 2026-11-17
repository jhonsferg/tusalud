package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionResponse;
import com.codesoftlabs.tusalud.app.security.JwtAuthenticationFilter;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/perfil/sesiones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Perfil - Sesiones", description = "Sesiones activas del usuario autenticado")
public class SesionController {

    private final SesionService sesionService;

    @GetMapping
    @Operation(
            summary = "Listar mis sesiones activas",
            description =
                    "Devuelve las sesiones no revocadas y no vencidas del usuario. El campo actual indica la sesión de la petición.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesiones activas"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SesionResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(hidden = true)
                    @RequestAttribute(value = JwtAuthenticationFilter.TOKEN_ID_ATTRIBUTE, required = false)
                    UUID tokenActual) {
        return ResponseEntity.ok(sesionService.listarActivas(principal, tokenActual));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Cerrar una sesión", description = "Revoca una sesión propia por su identificador.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sesión revocada"),
        @ApiResponse(
                responseCode = "404",
                description = "La sesión no existe o no pertenece al usuario",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> revocar(
            @Parameter(description = "ID de la sesión", example = "3") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        sesionService.revocar(principal, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(
            summary = "Cerrar todas las demás sesiones",
            description = "Revoca todas las sesiones del usuario excepto la sesión que realiza la petición.")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Sesiones revocadas")})
    public ResponseEntity<Void> revocarTodasExceptoActual(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(hidden = true)
                    @RequestAttribute(value = JwtAuthenticationFilter.TOKEN_ID_ATTRIBUTE, required = false)
                    UUID tokenActual) {
        sesionService.revocarTodasExceptoActual(principal, tokenActual);
        return ResponseEntity.noContent().build();
    }
}
