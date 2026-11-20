package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ConciliarRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.CuentaTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.ImportarExtractoRequest;
import com.codesoftlabs.tusalud.app.dto.request.ContabilidadRequests.MovimientoTesoreriaRequest;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaTesoreriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ExtractoLineaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ExtractoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.FlujoCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ResumenConciliacion;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.SugerenciaConciliacion;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.contabilidad.ConciliacionService;
import com.codesoftlabs.tusalud.app.services.contabilidad.FlujoCajaService;
import com.codesoftlabs.tusalud.app.services.contabilidad.TesoreriaService;
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
@RequestMapping("/api/v1/contabilidad/tesoreria")
@RequiredArgsConstructor
@Validated
@Tag(name = "Contabilidad - Tesorería", description = "Cuentas, movimientos, conciliación bancaria y flujo de caja")
public class TesoreriaController {

    private static final String FINANZAS = "hasRole('FINANZAS')";
    private static final String CONSULTA = "hasAnyRole('FINANZAS', 'GERENCIA')";

    private final TesoreriaService tesoreria;
    private final ConciliacionService conciliacion;
    private final FlujoCajaService flujo;

    @GetMapping("/cuentas")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<CuentaTesoreriaResponse>> cuentas(
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        return ResponseEntity.ok(tesoreria.cuentas(soloActivas));
    }

    @PostMapping("/cuentas")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<CuentaTesoreriaResponse> crearCuenta(
            @Valid @RequestBody CuentaTesoreriaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreria.crearCuenta(actor, request));
    }

    @PutMapping("/cuentas/{id}")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<CuentaTesoreriaResponse> actualizarCuenta(
            @PathVariable UUID id,
            @Valid @RequestBody CuentaTesoreriaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(tesoreria.actualizarCuenta(actor, id, request));
    }

    @GetMapping("/movimientos")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<MovimientoResponse>> movimientos(
            @RequestParam(required = false) UUID cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean conciliado) {
        return ResponseEntity.ok(tesoreria.listarMovimientos(cuentaId, desde, hasta, conciliado));
    }

    @PostMapping("/movimientos")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<MovimientoResponse> registrar(
            @Valid @RequestBody MovimientoTesoreriaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tesoreria.registrar(actor, request));
    }

    @GetMapping("/flujo-caja")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<FlujoCajaResponse> flujo(@RequestParam(defaultValue = "8") int semanas) {
        return ResponseEntity.ok(flujo.proyectar(semanas));
    }

    @GetMapping("/conciliacion/extractos")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ExtractoResponse>> extractos(@RequestParam(required = false) UUID cuentaId) {
        return ResponseEntity.ok(conciliacion.extractos(cuentaId));
    }

    @PostMapping("/conciliacion/extractos")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ExtractoResponse> importar(
            @Valid @RequestBody ImportarExtractoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conciliacion.importar(actor, request));
    }

    @GetMapping("/conciliacion/{cuentaId}/lineas")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<ExtractoLineaResponse>> lineas(
            @PathVariable UUID cuentaId, @RequestParam(required = false) EstadoLineaExtracto estado) {
        return ResponseEntity.ok(conciliacion.lineas(cuentaId, estado));
    }

    @GetMapping("/conciliacion/{cuentaId}/sugerencias")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<SugerenciaConciliacion>> sugerencias(@PathVariable UUID cuentaId) {
        return ResponseEntity.ok(conciliacion.sugerencias(cuentaId));
    }

    @GetMapping("/conciliacion/{cuentaId}/sin-conciliar")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<List<MovimientoResponse>> sinConciliar(@PathVariable UUID cuentaId) {
        return ResponseEntity.ok(conciliacion.movimientosSinConciliar(cuentaId));
    }

    @GetMapping("/conciliacion/{cuentaId}/resumen")
    @PreAuthorize(CONSULTA)
    public ResponseEntity<ResumenConciliacion> resumen(@PathVariable UUID cuentaId) {
        return ResponseEntity.ok(conciliacion.resumen(cuentaId));
    }

    @PostMapping("/conciliacion/conciliar")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ExtractoLineaResponse> conciliar(
            @Valid @RequestBody ConciliarRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(conciliacion.conciliar(actor, request));
    }

    @PostMapping("/conciliacion/lineas/{lineaId}/deshacer")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ExtractoLineaResponse> deshacer(
            @PathVariable UUID lineaId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(conciliacion.deshacer(actor, lineaId));
    }

    @PostMapping("/conciliacion/lineas/{lineaId}/ignorar")
    @PreAuthorize(FINANZAS)
    public ResponseEntity<ExtractoLineaResponse> ignorar(
            @PathVariable UUID lineaId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(conciliacion.ignorar(actor, lineaId));
    }
}
