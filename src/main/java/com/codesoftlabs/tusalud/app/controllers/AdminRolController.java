package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.RolResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.UsuarioAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Indicadores y gestión reservados para administradores")
public class AdminRolController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar roles",
            description =
                    "Devuelve los roles del sistema con su descripción y la cantidad de usuarios que los tienen. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de roles"),
        @ApiResponse(
                responseCode = "403",
                description = "Se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RolResponse>> listar() {
        return ResponseEntity.ok(usuarioAdminService.listarRoles());
    }
}
