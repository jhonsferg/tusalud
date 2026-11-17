package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.AnularComprobanteRequest;
import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CierreCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.CuentaPorCobrarResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteIngresosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionCajaResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
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
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Caja y facturación",
        description = "Apertura y cierre de caja, cobros, comprobantes y reportes de ingresos")
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/aperturas")
    @PreAuthorize("hasRole('CAJA')")
    @Operation(summary = "Abrir caja", description = "Abre la caja del cajero autenticado con un monto inicial.")
    public ResponseEntity<SesionCajaResponse> abrir(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AperturaCajaRequest request) {
        SesionCajaResponse creada = cajaService.abrir(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/caja/actual")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/actual")
    @PreAuthorize("hasRole('CAJA')")
    @Operation(summary = "Caja abierta", description = "Devuelve la caja abierta del cajero o 404 si no tiene una.")
    public ResponseEntity<SesionCajaResponse> actual(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(cajaService.actual(principal));
    }

    @PostMapping("/cierre")
    @PreAuthorize("hasRole('CAJA')")
    @Operation(
            summary = "Cerrar caja",
            description =
                    "Calcula el efectivo esperado, registra la diferencia y avisa a los administradores si existe.")
    public ResponseEntity<SesionCajaResponse> cerrar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CierreCajaRequest request) {
        return ResponseEntity.ok(cajaService.cerrar(principal, request));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(summary = "Historial de cajas", description = "Últimas 60 sesiones. El administrador ve las de todos.")
    public ResponseEntity<List<SesionCajaResponse>> historial(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(cajaService.historial(principal));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasRole('CAJA')")
    @Operation(summary = "Resumen de la caja actual", description = "Totales por método de pago de la caja abierta.")
    public ResponseEntity<ResumenCajaResponse> resumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(cajaService.resumen(principal));
    }

    @PostMapping("/cobros")
    @PreAuthorize("hasRole('CAJA')")
    @Operation(
            summary = "Registrar un cobro",
            description =
                    "Emite una boleta o factura. Si cobra una cita registra su pago y si cobra una orden de laboratorio la marca como cobrada.")
    public ResponseEntity<ComprobanteResponse> cobrar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CobroRequest request) {
        ComprobanteResponse creado = cajaService.cobrar(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/caja/comprobantes/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(summary = "Cuentas por cobrar", description = "Citas y órdenes de laboratorio aún sin cobrar.")
    public ResponseEntity<List<CuentaPorCobrarResponse>> pendientes(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(cajaService.pendientes(q));
    }

    @GetMapping("/comprobantes")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(
            summary = "Listar comprobantes",
            description = "Hasta 200 comprobantes, por defecto de los últimos 30 días.")
    public ResponseEntity<List<ComprobanteResponse>> comprobantes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoComprobante estado) {
        return ResponseEntity.ok(cajaService.comprobantes(desde, hasta, q, estado));
    }

    @GetMapping("/comprobantes/{id}")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(summary = "Detalle de un comprobante")
    public ResponseEntity<ComprobanteResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cajaService.obtener(id));
    }

    @PutMapping("/comprobantes/{id}/anular")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(
            summary = "Anular un comprobante",
            description = "Revierte el pago de la cita o el cobro de la orden de laboratorio de origen.")
    public ResponseEntity<ComprobanteResponse> anular(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AnularComprobanteRequest request) {
        return ResponseEntity.ok(cajaService.anular(principal, id, request.motivo()));
    }

    @GetMapping("/reportes/ingresos")
    @PreAuthorize("hasAnyRole('CAJA', 'ADMIN')")
    @Operation(
            summary = "Reporte de ingresos",
            description = "Totales por día, método de pago y origen. Máximo 366 días.")
    public ResponseEntity<ReporteIngresosResponse> reporteIngresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(cajaService.reporteIngresos(desde, hasta));
    }
}
