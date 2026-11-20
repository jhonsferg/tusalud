package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.CotizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.MotivoCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RequisicionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComparativaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.CotizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.SugerenciaRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.compras.CotizacionService;
import com.codesoftlabs.tusalud.app.services.compras.RequisicionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Validated
@Tag(name = "Compras - Requisiciones", description = "Requisiciones internas y cotizaciones comparativas")
public class RequisicionesCompraController {

    private static final String LECTURA = "hasAnyRole('ALMACEN', 'FARMACIA', 'FINANZAS', 'GERENCIA', 'ADMIN')";
    private static final String COMPRADORES = "hasAnyRole('ALMACEN', 'FINANZAS', 'GERENCIA', 'ADMIN')";
    private static final String SOLICITANTES = "hasAnyRole('ALMACEN', 'FARMACIA')";

    private final RequisicionService requisiciones;
    private final CotizacionService cotizaciones;

    @GetMapping("/requisiciones")
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<RequisicionResumen>> listar(
            @RequestParam(required = false) EstadoRequisicion estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.listar(actor, estado, desde, hasta));
    }

    @GetMapping("/requisiciones/sugeridas")
    @PreAuthorize(SOLICITANTES)
    public ResponseEntity<List<SugerenciaRequisicion>> sugeridas() {
        return ResponseEntity.ok(requisiciones.sugeridas());
    }

    @GetMapping("/requisiciones/{id}")
    @PreAuthorize(LECTURA)
    public ResponseEntity<RequisicionDetalle> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.obtener(actor, id));
    }

    @PostMapping("/requisiciones")
    @PreAuthorize(SOLICITANTES)
    public ResponseEntity<RequisicionDetalle> crear(
            @Valid @RequestBody RequisicionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requisiciones.crear(actor, request));
    }

    @PutMapping("/requisiciones/{id}")
    @PreAuthorize(SOLICITANTES)
    public ResponseEntity<RequisicionDetalle> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody RequisicionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.actualizar(actor, id, request));
    }

    @PostMapping("/requisiciones/{id}/enviar")
    @PreAuthorize(SOLICITANTES)
    public ResponseEntity<RequisicionDetalle> enviar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.enviar(actor, id));
    }

    @PostMapping("/requisiciones/{id}/rechazar")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<RequisicionDetalle> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.rechazar(actor, id, request.motivo()));
    }

    @PostMapping("/requisiciones/{id}/anular")
    @PreAuthorize(SOLICITANTES)
    public ResponseEntity<RequisicionDetalle> anular(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(requisiciones.anular(actor, id, request.motivo()));
    }

    @GetMapping("/requisiciones/{id}/cotizaciones")
    @PreAuthorize(COMPRADORES)
    public ResponseEntity<List<CotizacionResponse>> cotizaciones(@PathVariable UUID id) {
        return ResponseEntity.ok(cotizaciones.listar(id));
    }

    @GetMapping("/requisiciones/{id}/comparativa")
    @PreAuthorize(COMPRADORES)
    public ResponseEntity<ComparativaResponse> comparativa(@PathVariable UUID id) {
        return ResponseEntity.ok(cotizaciones.comparativa(id));
    }

    @PostMapping("/requisiciones/{id}/cotizaciones")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<CotizacionResponse> registrarCotizacion(
            @PathVariable UUID id,
            @Valid @RequestBody CotizacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cotizaciones.registrar(actor, id, request));
    }

    @PostMapping("/cotizaciones/{id}/seleccionar")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<CotizacionResponse> seleccionar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cotizaciones.seleccionar(actor, id));
    }
}
