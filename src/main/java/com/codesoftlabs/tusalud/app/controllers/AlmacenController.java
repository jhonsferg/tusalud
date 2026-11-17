package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.AjusteRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SalidaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AlertasInventarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoResponse;
import com.codesoftlabs.tusalud.app.dto.response.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenAlmacenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AlmacenService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/almacen")
@RequiredArgsConstructor
@Validated
@Tag(name = "Almacén - Operaciones", description = "Ingresos, salidas, ajustes, kardex y alertas de inventario")
public class AlmacenController {

    private final AlmacenService almacenService;

    @PostMapping("/ingresos")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(
            summary = "Registrar ingreso de mercadería",
            description = "Crea un lote por cada ítem y registra los movimientos de entrada.")
    public ResponseEntity<IngresoResponse> registrarIngreso(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody IngresoRequest request) {
        IngresoResponse creado = almacenService.registrarIngreso(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Listar los últimos ingresos")
    public ResponseEntity<List<IngresoResponse>> listarIngresos() {
        return ResponseEntity.ok(almacenService.listarIngresos());
    }

    @PostMapping("/salidas")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(
            summary = "Registrar salida de stock",
            description =
                    "Descuenta por FEFO, primero los lotes que vencen antes. Responde 409 si no alcanza el stock.")
    public ResponseEntity<List<MovimientoResponse>> registrarSalida(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody SalidaRequest request) {
        return ResponseEntity.status(201).body(almacenService.registrarSalida(principal, request));
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Registrar ajuste positivo de un lote")
    public ResponseEntity<MovimientoResponse> registrarAjuste(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AjusteRequest request) {
        return ResponseEntity.status(201).body(almacenService.registrarAjuste(principal, request));
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Kardex de movimientos", description = "Los más recientes primero, hasta 200.")
    public ResponseEntity<List<MovimientoResponse>> movimientos(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) TipoMovimiento tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(almacenService.movimientos(productoId, tipo, desde, hasta));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN', 'FARMACIA')")
    @Operation(summary = "Alertas de stock bajo y vencimientos")
    public ResponseEntity<AlertasInventarioResponse> alertas() {
        return ResponseEntity.ok(almacenService.alertas());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ALMACEN', 'ADMIN')")
    @Operation(summary = "Indicadores del almacén")
    public ResponseEntity<ResumenAlmacenResponse> resumen() {
        return ResponseEntity.ok(almacenService.resumen());
    }
}
