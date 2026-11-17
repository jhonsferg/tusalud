package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ProveedorResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/almacen/proveedores")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
@Tag(name = "Almacén - Proveedores", description = "Gestión de proveedores (requiere rol ALMACEN o ADMIN)")
public class AlmacenProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @Operation(
            summary = "Listar proveedores",
            description = "Permite buscar por razón social o RUC y filtrar por estado.")
    public ResponseEntity<List<ProveedorResponse>> listar(
            @RequestParam(required = false) String q, @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(proveedorService.listar(q, activo));
    }

    @PostMapping
    @Operation(summary = "Crear proveedor", description = "El RUC debe tener 11 dígitos y ser único.")
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse creado = proveedorService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Actualizar proveedor")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request));
    }
}
