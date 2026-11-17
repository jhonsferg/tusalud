package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CambioEstadoUsuarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.RolesUsuarioRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioAdminResponse;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.UsuarioAdminService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequiredArgsConstructor
@Validated
@Tag(name = "Administración", description = "Indicadores y gestión reservados para administradores")
public class AdminUsuarioController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar usuarios",
            description =
                    "Lista las cuentas con su rol, estado y último acceso. Se puede filtrar por rol y buscar por nombre, apellido o email. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de usuarios"),
        @ApiResponse(
                responseCode = "403",
                description = "Se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UsuarioAdminResponse>> listar(
            @Parameter(description = "Filtra por rol", example = "ROLE_MEDICO") @RequestParam(required = false)
                    RolNombre rol,
            @Parameter(description = "Texto a buscar en nombre, apellido o email", example = "garcia")
                    @RequestParam(required = false)
                    String q) {
        return ResponseEntity.ok(usuarioAdminService.listar(rol, q));
    }

    @PutMapping("/{id:\\d+}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Asignar roles a un usuario",
            description =
                    "Reemplaza los roles de una cuenta. Exige un perfil de médico o de paciente para los roles correspondientes, no permite que un administrador se quite su propio rol ni dejar el sistema sin administradores, y cierra las sesiones del usuario. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles actualizados"),
        @ApiResponse(
                responseCode = "404",
                description = "Usuario no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Combinación de roles no permitida",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UsuarioAdminResponse> asignarRoles(
            @Parameter(description = "ID del usuario", example = "4") @PathVariable @Positive Long id,
            @Valid @RequestBody RolesUsuarioRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioAdminService.asignarRoles(id, request.roles(), principal));
    }

    @PutMapping("/{id:\\d+}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Activar o desactivar un usuario",
            description =
                    "Cambia el estado de una cuenta. Al desactivarla se cierran todas sus sesiones. Un administrador no puede desactivarse a sí mismo. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(
                responseCode = "404",
                description = "Usuario no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Un administrador no puede desactivar su propia cuenta",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UsuarioAdminResponse> cambiarEstado(
            @Parameter(description = "ID del usuario", example = "4") @PathVariable @Positive Long id,
            @Valid @RequestBody CambioEstadoUsuarioRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioAdminService.cambiarEstado(id, request.estado(), principal));
    }
}
