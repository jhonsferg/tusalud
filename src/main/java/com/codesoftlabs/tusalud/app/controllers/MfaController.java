package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.MfaCodigoRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaDesactivarRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaVerificarRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaCodigosRecuperacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaConfiguracionResponse;
import com.codesoftlabs.tusalud.app.dto.response.MfaEstadoResponse;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import com.codesoftlabs.tusalud.app.services.interfaces.MfaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(
        name = "Verificación en dos pasos",
        description = "Activación y uso de una aplicación autenticadora TOTP para todos los usuarios")
public class MfaController {

    private final MfaService mfaService;
    private final AuthService authService;

    @GetMapping("/estado")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Estado de la verificación en dos pasos",
            description = "Indica si está activa y cuántos códigos de recuperación quedan sin usar.")
    public ResponseEntity<MfaEstadoResponse> estado(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(mfaService.estado(actor));
    }

    @PostMapping("/configurar")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Generar el secreto",
            description =
                    "Genera un secreto TOTP y la URI otpauth para escanear como código QR. Aún no activa la verificación.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Secreto generado"),
        @ApiResponse(
                responseCode = "409",
                description = "La verificación ya está activada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MfaConfiguracionResponse> configurar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(mfaService.configurar(actor));
    }

    @PostMapping("/activar")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Activar la verificación",
            description =
                    "Confirma el primer código de la aplicación y devuelve los códigos de recuperación, que se muestran una sola vez.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verificación activada"),
        @ApiResponse(
                responseCode = "400",
                description = "Código inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "429",
                description = "Demasiados intentos fallidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MfaCodigosRecuperacionResponse> activar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody MfaCodigoRequest request) {
        return ResponseEntity.ok(mfaService.activar(actor, request.codigo()));
    }

    @PostMapping("/desactivar")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Desactivar la verificación",
            description = "Requiere la contraseña y un código vigente o de recuperación.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Verificación desactivada"),
        @ApiResponse(
                responseCode = "400",
                description = "Contraseña o código inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "429",
                description = "Demasiados intentos fallidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> desactivar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody MfaDesactivarRequest request) {
        mfaService.desactivar(actor, request.password(), request.codigo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/codigos-recuperacion/regenerar")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Regenerar códigos de recuperación",
            description = "Invalida los códigos anteriores y emite ocho nuevos tras confirmar un código vigente.")
    public ResponseEntity<MfaCodigosRecuperacionResponse> regenerar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor,
            @Valid @RequestBody MfaCodigoRequest request) {
        return ResponseEntity.ok(mfaService.regenerarCodigos(actor, request.codigo()));
    }

    @PostMapping("/verificar")
    @SecurityRequirements()
    @Operation(
            summary = "Completar el inicio de sesión",
            description =
                    "Segundo paso del inicio de sesión: valida el código TOTP o de recuperación y devuelve el token de acceso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
        @ApiResponse(
                responseCode = "401",
                description = "Token temporal o código inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "429",
                description = "Demasiados intentos fallidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> verificar(
            @Valid @RequestBody MfaVerificarRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.verificarMfa(request, ContextoConexion.desde(httpRequest)));
    }
}
