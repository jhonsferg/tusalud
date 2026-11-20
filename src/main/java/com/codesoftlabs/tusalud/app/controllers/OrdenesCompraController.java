package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.AplicarCreditoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.DevolucionRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.MotivoCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.OrdenCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.RecepcionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprasAuditoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ConfiguracionCompras;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.DevolucionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionResumen;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.compras.ComprasAuditor;
import com.codesoftlabs.tusalud.app.services.compras.OrdenCompraService;
import com.codesoftlabs.tusalud.app.services.compras.RecepcionCompraService;
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
@Tag(name = "Compras - Órdenes y recepciones", description = "Órdenes de compra, aprobación, recepción y devoluciones")
public class OrdenesCompraController {

    private static final String LECTURA = "hasAnyRole('ALMACEN', 'FINANZAS', 'GERENCIA', 'ADMIN')";
    private static final String APROBADORES = "hasAnyRole('FINANZAS', 'GERENCIA')";
    private static final String AUDITORES = "hasAnyRole('FINANZAS', 'GERENCIA', 'ADMIN')";

    private final OrdenCompraService ordenes;
    private final RecepcionCompraService recepciones;
    private final ComprasAuditor auditor;

    @GetMapping("/configuracion")
    @PreAuthorize(LECTURA)
    public ResponseEntity<ConfiguracionCompras> configuracion() {
        return ResponseEntity.ok(ordenes.configuracion());
    }

    @GetMapping("/ordenes")
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<OrdenCompraResumen>> listar(
            @RequestParam(required = false) EstadoOrdenCompra estado,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ordenes.listar(estado, proveedorId, desde, hasta));
    }

    @GetMapping("/ordenes/{id}")
    @PreAuthorize(LECTURA)
    public ResponseEntity<OrdenCompraDetalle> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ordenes.obtener(id));
    }

    @GetMapping("/ordenes/{id}/auditoria")
    @PreAuthorize(AUDITORES)
    public ResponseEntity<List<ComprasAuditoriaResponse>> auditoria(@PathVariable UUID id) {
        return ResponseEntity.ok(auditor.deEntidad(id));
    }

    @PostMapping("/ordenes")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<OrdenCompraDetalle> crear(
            @Valid @RequestBody OrdenCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenes.crear(actor, request));
    }

    @PostMapping("/ordenes/desde-cotizacion/{cotizacionId}")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<OrdenCompraDetalle> desdeCotizacion(
            @PathVariable UUID cotizacionId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ordenes.desdeCotizacion(actor, cotizacionId));
    }

    @PutMapping("/ordenes/{id}")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<OrdenCompraDetalle> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody OrdenCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.actualizar(actor, id, request));
    }

    @PostMapping("/ordenes/{id}/enviar-aprobacion")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<OrdenCompraDetalle> enviarAprobacion(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.enviarAprobacion(actor, id));
    }

    @PostMapping("/ordenes/{id}/aprobar")
    @PreAuthorize(APROBADORES)
    public ResponseEntity<OrdenCompraDetalle> aprobar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.aprobar(actor, id));
    }

    @PostMapping("/ordenes/{id}/rechazar")
    @PreAuthorize(APROBADORES)
    public ResponseEntity<OrdenCompraDetalle> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.rechazar(actor, id, request.motivo()));
    }

    @PostMapping("/ordenes/{id}/anular")
    @PreAuthorize("hasAnyRole('ALMACEN', 'FINANZAS', 'GERENCIA')")
    public ResponseEntity<OrdenCompraDetalle> anular(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.anular(actor, id, request.motivo()));
    }

    @PostMapping("/ordenes/{id}/cerrar")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<OrdenCompraDetalle> cerrar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(ordenes.cerrar(actor, id, request.motivo()));
    }

    @PostMapping("/ordenes/{id}/recepciones")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<RecepcionDetalle> recibir(
            @PathVariable UUID id,
            @Valid @RequestBody RecepcionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recepciones.registrar(actor, id, request));
    }

    @GetMapping("/recepciones")
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<RecepcionResumen>> recepciones(
            @RequestParam(required = false) UUID ordenId,
            @RequestParam(defaultValue = "false") boolean soloDiscrepancias,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(recepciones.listar(ordenId, soloDiscrepancias, desde, hasta));
    }

    @GetMapping("/recepciones/{id}")
    @PreAuthorize(LECTURA)
    public ResponseEntity<RecepcionDetalle> recepcion(@PathVariable UUID id) {
        return ResponseEntity.ok(recepciones.obtener(id));
    }

    @GetMapping("/devoluciones")
    @PreAuthorize(LECTURA)
    public ResponseEntity<List<DevolucionResponse>> devoluciones(
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(recepciones.devoluciones(proveedorId, desde, hasta));
    }

    @PostMapping("/devoluciones/{id}/aplicar-credito")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<DevolucionResponse> aplicarCredito(
            @PathVariable UUID id,
            @Valid @RequestBody AplicarCreditoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(recepciones.aplicarCredito(actor, id, request.comprobanteId()));
    }

    @PostMapping("/devoluciones")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<DevolucionResponse> devolver(
            @Valid @RequestBody DevolucionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recepciones.devolver(actor, request));
    }
}
