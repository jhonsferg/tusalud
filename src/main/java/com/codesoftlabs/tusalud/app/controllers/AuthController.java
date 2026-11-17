package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.RefreshTokenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RegisterRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.security.ContextoConexion;
import com.codesoftlabs.tusalud.app.security.JwtAuthenticationFilter;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Autenticación", description = "Inicio de sesión y registro de pacientes (endpoints públicos)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements()
    @Operation(
            summary = "Iniciar sesión",
            description =
                    "Autentica al usuario con email y contraseña y devuelve un token JWT para usar en el encabezado Authorization: Bearer <token>.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Credenciales inválidas o usuario deshabilitado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, ContextoConexion.desde(httpRequest)));
    }

    @PostMapping("/register")
    @SecurityRequirements()
    @Operation(
            summary = "Registrar paciente",
            description =
                    "Crea una cuenta de paciente (ROLE_PACIENTE) y devuelve su token JWT para iniciar sesión de inmediato.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Paciente registrado correctamente"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El email o el documento ya se encuentran registrados",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarPaciente(request, ContextoConexion.desde(httpRequest)));
    }

    @PostMapping("/refresh")
    @SecurityRequirements()
    @Operation(
            summary = "Renovar el token de acceso",
            description =
                    "Intercambia un refresh token vigente por un nuevo token de acceso y un nuevo refresh token (rotación). Reutilizar un refresh token ya rotado cierra la sesión.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Refresh token inexistente, expirado, reutilizado o sesión cerrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refrescar(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca la sesión asociada al token con el que se realiza la petición.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sesión cerrada"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true)
                    @RequestAttribute(value = JwtAuthenticationFilter.TOKEN_ID_ATTRIBUTE, required = false)
                    UUID tokenActual) {
        authService.cerrarSesion(tokenActual);
        return ResponseEntity.noContent().build();
    }
}
