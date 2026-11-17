package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ProductoRequest;
import com.codesoftlabs.tusalud.app.dto.response.LoteResponse;
import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/almacen/productos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Almacén - Productos", description = "Catálogo de productos y sus lotes")
public class AlmacenProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN', 'FARMACIA', 'LABORATORIO')")
    @Operation(
            summary = "Listar productos",
            description = "Incluye el stock actual de lotes vigentes. El laboratorio solo ve insumos y reactivos.")
    public ResponseEntity<List<ProductoResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TipoProducto tipo,
            @RequestParam(required = false) Boolean bajoStock,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(productoService.listar(principal, q, tipo, bajoStock, activo));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN', 'FARMACIA', 'LABORATORIO')")
    @Operation(summary = "Obtener producto")
    public ResponseEntity<ProductoResponse> obtener(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtener(principal, id));
    }

    @GetMapping("/{id:\\d+}/lotes")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN', 'FARMACIA', 'LABORATORIO')")
    @Operation(summary = "Listar lotes de un producto", description = "Ordenados por fecha de vencimiento.")
    public ResponseEntity<List<LoteResponse>> lotes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(productoService.lotes(principal, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Crear producto", description = "El código es único y se guarda en mayúsculas.")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = productoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Actualizar producto")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }
}
