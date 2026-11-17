package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.response.CuponResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/admin/cupones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración - Cupones", description = "Gestión de cupones de descuento (requiere rol ADMIN)")
public class AdminCuponController {

    private final CuponService cuponService;

    @GetMapping
    @Operation(summary = "Listar cupones", description = "Devuelve todos los cupones, los más recientes primero.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de cupones"),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CuponResponse>> listar() {
        return ResponseEntity.ok(cuponService.listar());
    }

    @PostMapping
    @Operation(
            summary = "Crear cupón",
            description = "Crea un cupón. El código se guarda en mayúsculas y debe ser único.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cupón creado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Ya existe un cupón con ese código",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CuponResponse> crear(@Valid @RequestBody CuponRequest request) {
        CuponResponse creado = cuponService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Actualizar cupón")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cupón actualizado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cupón no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Ya existe otro cupón con ese código",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CuponResponse> actualizar(
            @Parameter(description = "ID del cupón", example = "1") @PathVariable Long id,
            @Valid @RequestBody CuponRequest request) {
        return ResponseEntity.ok(cuponService.actualizar(id, request));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(
            summary = "Eliminar cupón",
            description = "Solo se puede eliminar un cupón que nunca fue utilizado (usos actuales en cero).")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Cupón eliminado"),
        @ApiResponse(
                responseCode = "404",
                description = "Cupón no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El cupón ya fue utilizado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del cupón", example = "1") @PathVariable Long id) {
        cuponService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
