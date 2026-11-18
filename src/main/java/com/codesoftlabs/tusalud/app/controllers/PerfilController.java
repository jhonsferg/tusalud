package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.PerfilUpdateRequest;
import com.codesoftlabs.tusalud.app.dto.request.PreferenciasRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PerfilResponse;
import com.codesoftlabs.tusalud.app.dto.response.PreferenciasResponse;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.security.JwtAuthenticationFilter;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Perfil", description = "Datos personales y contraseña del usuario autenticado")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    @Operation(
            summary = "Obtener mi perfil",
            description =
                    "Devuelve los datos personales del usuario autenticado y los de su ficha de paciente o médico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil del usuario"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PerfilResponse> obtener(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.obtener(principal));
    }

    @PutMapping
    @Operation(
            summary = "Actualizar mi perfil",
            description =
                    "Modifica nombre, apellido y teléfono. Los pacientes pueden cambiar su fecha de nacimiento y los médicos su consultorio. El email, el DNI y la colegiatura no se modifican.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PerfilResponse> actualizar(
            @Valid @RequestBody PerfilUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizar(principal, request));
    }

    @PutMapping("/password")
    @Operation(
            summary = "Cambiar mi contraseña",
            description =
                    "Cambia la contraseña del usuario autenticado después de verificar la actual. El token vigente sigue siendo válido.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La contraseña actual no es correcta o la nueva es igual a la actual",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(hidden = true)
                    @RequestAttribute(value = JwtAuthenticationFilter.TOKEN_ID_ATTRIBUTE, required = false)
                    UUID tokenActual) {
        perfilService.cambiarPassword(principal, request, tokenActual);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferencias")
    @Operation(
            summary = "Obtener mis preferencias",
            description =
                    "Devuelve las preferencias de recordatorios y alertas. Si nunca se guardaron, devuelve los valores por defecto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencias del usuario"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PreferenciasResponse> obtenerPreferencias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.obtenerPreferencias(principal));
    }

    @PutMapping("/preferencias")
    @Operation(
            summary = "Actualizar mis preferencias",
            description =
                    "Guarda las preferencias de recordatorios (activos y horas de antelación entre 1 y 72) y alertas de inicio de sesión.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencias actualizadas"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PreferenciasResponse> actualizarPreferencias(
            @Valid @RequestBody PreferenciasRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(perfilService.actualizarPreferencias(principal, request));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Actualizar mi avatar",
            description =
                    "Sube la imagen de perfil como archivo multipart y la guarda en el almacén de objetos. Formatos permitidos: PNG, JPEG y WEBP validados por su contenido, hasta 2 MB.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avatar actualizado; devuelve el perfil"),
        @ApiResponse(
                responseCode = "400",
                description = "Imagen con formato, tipo o tamaño no permitido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PerfilResponse> actualizarAvatar(
            @RequestParam("archivo") MultipartFile archivo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (java.io.IOException ex) {
            throw new DatosInvalidosException("No se pudo leer el archivo cargado");
        }
        return ResponseEntity.ok(perfilService.actualizarAvatar(principal, archivo.getOriginalFilename(), contenido));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Eliminar mi avatar", description = "Quita la imagen de perfil del usuario autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Avatar eliminado"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminarAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        perfilService.eliminarAvatar(principal);
        return ResponseEntity.noContent().build();
    }
}
