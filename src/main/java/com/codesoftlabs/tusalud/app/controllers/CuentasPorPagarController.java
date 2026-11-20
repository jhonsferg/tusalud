package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.ComprobanteProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.MotivoCompraRequest;
import com.codesoftlabs.tusalud.app.dto.request.ComprasRequests.PagoProveedorRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprasAuditoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprobanteProveedorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ReporteCompraProveedor;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ResumenCompras;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ResumenCuentasPorPagar;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.compras.ComprasAuditor;
import com.codesoftlabs.tusalud.app.services.compras.CuentasPorPagarService;
import com.codesoftlabs.tusalud.app.services.compras.ReportesComprasService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Validated
@Tag(name = "Compras - Cuentas por pagar", description = "Comprobantes de proveedores, pagos, reportes y auditoría")
public class CuentasPorPagarController {

    private static final String FINANZAS = "hasRole('FINANZAS')";
    private static final String CONSULTA = "hasAnyRole('FINANZAS', 'GERENCIA', 'ADMIN')";

    private final CuentasPorPagarService cuentas;
    private final ReportesComprasService reportes;
    private final ComprasAuditor auditor;

    @GetMapping("/comprobantes")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ComprobanteProveedorResponse>> listar(
            @RequestParam(required = false) EstadoComprobanteProveedor estado,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "false") boolean soloVencidos) {
        return ResponseEntity.ok(cuentas.listar(estado, proveedorId, desde, hasta, soloVencidos));
    }

    @GetMapping("/comprobantes/{id}")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<ComprobanteProveedorResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentas.obtener(id));
    }

    @PostMapping("/comprobantes")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ComprobanteProveedorResponse> registrar(
            @Valid @RequestBody ComprobanteProveedorRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentas.registrar(actor, request));
    }

    @PostMapping("/comprobantes/{id}/pagos")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ComprobanteProveedorResponse> pagar(
            @PathVariable UUID id,
            @Valid @RequestBody PagoProveedorRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentas.pagar(actor, id, request));
    }

    @PostMapping("/comprobantes/{id}/anular")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ComprobanteProveedorResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCompraRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cuentas.anular(actor, id, request.motivo()));
    }

    @GetMapping("/cuentas-por-pagar/resumen")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<ResumenCuentasPorPagar> resumenCuentas() {
        return ResponseEntity.ok(cuentas.resumen());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ALMACEN', 'FINANZAS', 'GERENCIA', 'ADMIN')")
    public ResponseEntity<ResumenCompras> resumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        ResumenCompras completo = reportes.resumen();
        boolean financiero = actor.getRoles().stream()
                .anyMatch(r -> r.equals("ROLE_FINANZAS") || r.equals("ROLE_GERENCIA") || r.equals("ROLE_ADMIN"));
        if (financiero) {
            return ResponseEntity.ok(completo);
        }
        return ResponseEntity.ok(new ResumenCompras(
                completo.requisicionesAbiertas(),
                completo.ordenesPorAprobar(),
                completo.ordenesEnTransito(),
                null,
                null,
                completo.limiteFinanzas()));
    }

    @GetMapping("/reportes/por-proveedor")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ReporteCompraProveedor>> porProveedor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID proveedorId) {
        return ResponseEntity.ok(reportes.porProveedor(desde, hasta, proveedorId));
    }

    @GetMapping("/auditoria")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ComprasAuditoriaResponse>> auditoria(@RequestParam(required = false) String entidad) {
        return ResponseEntity.ok(auditor.recientes(entidad));
    }
}
