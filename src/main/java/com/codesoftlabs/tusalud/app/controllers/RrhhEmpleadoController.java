package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.EmpleadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.EmpleadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RrhhResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsuarioDisponibleResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.security.Autorizaciones;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EmpleadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rrhh")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recursos humanos - Empleados", description = "Fichas laborales del personal y resumen de la plantilla")
public class RrhhEmpleadoController {

    private final EmpleadoService empleadoService;

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('RRHH', 'GERENCIA', 'ADMIN')")
    @Operation(
            summary = "Resumen de la plantilla",
            description =
                    "Conteos por estado y área, contratos por vencer y ausentes de hoy. El detalle de ausentes es de RRHH y administración, la masa salarial de RRHH y gerencia.")
    public ResponseEntity<RrhhResumenResponse> resumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(empleadoService.resumen(principal));
    }

    @GetMapping("/empleados")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN', 'GERENCIA')")
    @Operation(
            summary = "Buscar empleados",
            description =
                    "El sueldo solo llega a RRHH y gerencia y el contacto de emergencia no llega al administrador.")
    public ResponseEntity<List<EmpleadoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AreaLaboral area,
            @RequestParam(required = false) EstadoEmpleado estado,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(empleadoService.listar(q, area, estado, principal));
    }

    @GetMapping("/empleados/{id}")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN', 'GERENCIA')")
    @Operation(summary = "Ficha de un empleado")
    public ResponseEntity<EmpleadoResponse> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(empleadoService.obtener(id, principal));
    }

    @PostMapping("/empleados")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Crear una ficha laboral",
            description = "Vincula una ficha a un usuario del personal que aún no la tiene.")
    public ResponseEntity<EmpleadoResponse> crear(
            @Valid @RequestBody EmpleadoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crear(request, principal));
    }

    @PutMapping("/empleados/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(
            summary = "Actualizar una ficha laboral",
            description = "Registra en la auditoría de RR. HH. quién cambió qué, sin guardar los montos.")
    public ResponseEntity<EmpleadoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody EmpleadoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(empleadoService.actualizar(id, request, principal));
    }

    @GetMapping("/usuarios-disponibles")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Usuarios del personal sin ficha laboral")
    public ResponseEntity<List<UsuarioDisponibleResponse>> usuariosDisponibles() {
        return ResponseEntity.ok(empleadoService.usuariosDisponibles());
    }

    @GetMapping("/mi-ficha")
    @PreAuthorize(Autorizaciones.PERSONAL)
    @Operation(summary = "Mi ficha laboral", description = "No incluye el sueldo.")
    public ResponseEntity<EmpleadoResponse> miFicha(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(empleadoService.miFicha(principal));
    }
}
