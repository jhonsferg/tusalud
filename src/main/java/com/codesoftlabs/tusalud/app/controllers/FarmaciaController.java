package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DispensacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CatalogoFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenFarmaciaResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoReceta;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.FarmaciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/farmacia")
@RequiredArgsConstructor
@Validated
@Tag(name = "Farmacia", description = "Catálogo, recetas pendientes y dispensación de medicamentos")
public class FarmaciaController {

    private final FarmaciaService farmaciaService;

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyRole('FARMACIA', 'MEDICO', 'ADMIN', 'ALMACEN')")
    @Operation(summary = "Catálogo de medicamentos", description = "Medicamentos activos con precio y stock actual.")
    public ResponseEntity<List<CatalogoFarmaciaResponse>> catalogo(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(farmaciaService.catalogo(q));
    }

    @GetMapping("/recetas")
    @PreAuthorize("hasRole('FARMACIA')")
    @Operation(
            summary = "Recetas para dispensar",
            description = "Recetas de los últimos 90 días. Por defecto devuelve las pendientes y parciales.")
    public ResponseEntity<List<RecetaFarmaciaResponse>> recetas(
            @RequestParam(required = false) EstadoReceta estado, @RequestParam(required = false) String q) {
        return ResponseEntity.ok(farmaciaService.recetas(estado, q));
    }

    @PostMapping("/dispensaciones")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(
            summary = "Dispensar medicamentos",
            description =
                    "Descuenta el stock por FEFO, marca los ítems de receta como dispensados y genera el comprobante.")
    public ResponseEntity<DispensacionResponse> dispensar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody DispensacionRequest request) {
        DispensacionResponse creada = farmaciaService.dispensar(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/dispensaciones")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(summary = "Historial de dispensaciones", description = "Las últimas 200 del rango, 30 días por defecto.")
    public ResponseEntity<List<DispensacionResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(farmaciaService.listar(desde, hasta, q));
    }

    @GetMapping("/dispensaciones/{id}")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(summary = "Detalle de una dispensación")
    public ResponseEntity<DispensacionResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(farmaciaService.obtener(id));
    }

    @PutMapping("/dispensaciones/{id}/anular")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(
            summary = "Anular una dispensación",
            description =
                    "Devuelve el stock a sus lotes y reabre los ítems de receta. Responde 409 si ya está anulada.")
    public ResponseEntity<DispensacionResponse> anular(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(farmaciaService.anular(principal, id));
    }

    @GetMapping("/mis-dispensaciones")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mis medicamentos dispensados")
    public ResponseEntity<List<DispensacionResponse>> misDispensaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(farmaciaService.misDispensaciones(principal));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('FARMACIA', 'ADMIN')")
    @Operation(summary = "Indicadores de la farmacia")
    public ResponseEntity<ResumenFarmaciaResponse> resumen() {
        return ResponseEntity.ok(farmaciaService.resumen());
    }
}
