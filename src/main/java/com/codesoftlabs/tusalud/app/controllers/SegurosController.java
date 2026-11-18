package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AprobarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AseguradoraRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.ConvenioRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionCrearRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionPagoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.RechazarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.AseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CarteraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CoberturaPacienteResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.ConvenioResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CuentaAseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.LiquidacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.MiPolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PreautorizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.SegurosResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.VerificacionResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.seguros.LiquidacionService;
import com.codesoftlabs.tusalud.app.services.seguros.PolizaService;
import com.codesoftlabs.tusalud.app.services.seguros.PreautorizacionService;
import com.codesoftlabs.tusalud.app.services.seguros.SegurosCatalogoService;
import com.codesoftlabs.tusalud.app.services.seguros.SegurosReporteService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/seguros")
@RequiredArgsConstructor
@Validated
@Tag(name = "Seguros y convenios", description = "Aseguradoras, convenios, pólizas, preautorizaciones y liquidaciones")
public class SegurosController {

    private static final String LECTURA_CATALOGO = "hasAnyRole('ADMIN', 'FINANZAS', 'GERENCIA', 'CAJA', 'RECEPCION')";
    private static final String ESCRITURA_CATALOGO = "hasAnyRole('ADMIN', 'FINANZAS')";
    private static final String MOSTRADOR = "hasAnyRole('RECEPCION', 'ADMIN')";
    private static final String MOSTRADOR_Y_CAJA = "hasAnyRole('RECEPCION', 'ADMIN', 'CAJA')";
    private static final String GESTION_CARTERA = "hasRole('FINANZAS')";
    private static final String LECTURA_CARTERA = "hasAnyRole('FINANZAS', 'GERENCIA', 'ADMIN')";
    private static final String GESTORES_PREAUTORIZACION = "hasAnyRole('FINANZAS', 'RECEPCION')";

    private final SegurosCatalogoService catalogo;
    private final PolizaService polizas;
    private final PreautorizacionService preautorizaciones;
    private final LiquidacionService liquidaciones;
    private final SegurosReporteService reportes;

    @GetMapping("/aseguradoras")
    @PreAuthorize(LECTURA_CATALOGO)
    public ResponseEntity<List<AseguradoraResponse>> aseguradoras() {
        return ResponseEntity.ok(catalogo.aseguradoras());
    }

    @PostMapping("/aseguradoras")
    @PreAuthorize(ESCRITURA_CATALOGO)
    public ResponseEntity<AseguradoraResponse> crearAseguradora(@Valid @RequestBody AseguradoraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogo.crearAseguradora(request));
    }

    @PutMapping("/aseguradoras/{id}")
    @PreAuthorize(ESCRITURA_CATALOGO)
    public ResponseEntity<AseguradoraResponse> actualizarAseguradora(
            @PathVariable UUID id, @Valid @RequestBody AseguradoraRequest request) {
        return ResponseEntity.ok(catalogo.actualizarAseguradora(id, request));
    }

    @GetMapping("/convenios")
    @PreAuthorize(LECTURA_CATALOGO)
    public ResponseEntity<List<ConvenioResponse>> convenios() {
        return ResponseEntity.ok(catalogo.convenios());
    }

    @GetMapping("/convenios/{id}")
    @PreAuthorize(LECTURA_CATALOGO)
    public ResponseEntity<ConvenioResponse> convenio(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogo.convenio(id));
    }

    @PostMapping("/convenios")
    @PreAuthorize(ESCRITURA_CATALOGO)
    public ResponseEntity<ConvenioResponse> crearConvenio(@Valid @RequestBody ConvenioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogo.crearConvenio(request));
    }

    @PutMapping("/convenios/{id}")
    @PreAuthorize(ESCRITURA_CATALOGO)
    public ResponseEntity<ConvenioResponse> actualizarConvenio(
            @PathVariable UUID id, @Valid @RequestBody ConvenioRequest request) {
        return ResponseEntity.ok(catalogo.actualizarConvenio(id, request));
    }

    @GetMapping("/polizas")
    @PreAuthorize(MOSTRADOR_Y_CAJA)
    public ResponseEntity<List<PolizaResponse>> buscarPolizas(@RequestParam String q) {
        return ResponseEntity.ok(polizas.buscar(q));
    }

    @GetMapping("/pacientes/{pacienteId}/polizas")
    @PreAuthorize(MOSTRADOR_Y_CAJA)
    public ResponseEntity<List<PolizaResponse>> polizasDelPaciente(@PathVariable UUID pacienteId) {
        return ResponseEntity.ok(polizas.delPaciente(pacienteId));
    }

    @PostMapping("/polizas")
    @PreAuthorize(MOSTRADOR)
    public ResponseEntity<PolizaResponse> registrarPoliza(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PolizaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(polizas.registrar(principal, request));
    }

    @PutMapping("/polizas/{id}/estado")
    @PreAuthorize(MOSTRADOR)
    public ResponseEntity<PolizaResponse> cambiarEstadoPoliza(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PolizaEstadoRequest request) {
        return ResponseEntity.ok(polizas.cambiarEstado(principal, id, request));
    }

    @PostMapping("/polizas/{id}/verificaciones")
    @PreAuthorize(MOSTRADOR_Y_CAJA)
    public ResponseEntity<VerificacionResponse> verificarPoliza(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(polizas.verificar(principal, id));
    }

    @GetMapping("/mis-polizas")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<MiPolizaResponse>> misPolizas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(polizas.mias(principal));
    }

    @GetMapping("/cobertura/pacientes/{pacienteId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CoberturaPacienteResponse> coberturaDelPaciente(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID pacienteId) {
        return ResponseEntity.ok(polizas.coberturaParaMedico(principal, pacienteId));
    }

    @PostMapping("/preautorizaciones")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<PreautorizacionResponse> solicitarPreautorizacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PreautorizacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preautorizaciones.solicitar(principal, request));
    }

    @GetMapping("/preautorizaciones")
    @PreAuthorize("hasAnyRole('FINANZAS', 'RECEPCION', 'ADMIN')")
    public ResponseEntity<List<PreautorizacionResponse>> bandejaPreautorizaciones(
            @RequestParam(required = false) EstadoPreautorizacion estado) {
        return ResponseEntity.ok(preautorizaciones.listar(estado));
    }

    @GetMapping("/preautorizaciones/mias")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<List<PreautorizacionResponse>> misPreautorizaciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        boolean medico = principal.getRoles().contains(RolNombre.ROLE_MEDICO.name());
        return ResponseEntity.ok(
                medico ? preautorizaciones.delMedico(principal) : preautorizaciones.delPaciente(principal));
    }

    @PutMapping("/preautorizaciones/{id}/aprobar")
    @PreAuthorize(GESTORES_PREAUTORIZACION)
    public ResponseEntity<PreautorizacionResponse> aprobarPreautorizacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AprobarPreautorizacionRequest request) {
        return ResponseEntity.ok(preautorizaciones.aprobar(principal, id, request));
    }

    @PutMapping("/preautorizaciones/{id}/rechazar")
    @PreAuthorize(GESTORES_PREAUTORIZACION)
    public ResponseEntity<PreautorizacionResponse> rechazarPreautorizacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody RechazarPreautorizacionRequest request) {
        return ResponseEntity.ok(preautorizaciones.rechazar(principal, id, request));
    }

    @GetMapping("/cartera")
    @PreAuthorize(LECTURA_CARTERA)
    public ResponseEntity<List<CarteraResponse>> cartera() {
        return ResponseEntity.ok(liquidaciones.cartera());
    }

    @GetMapping("/aseguradoras/{id}/cuentas")
    @PreAuthorize(LECTURA_CARTERA)
    public ResponseEntity<List<CuentaAseguradoraResponse>> cuentas(@PathVariable UUID id) {
        return ResponseEntity.ok(liquidaciones.cuentas(id));
    }

    @GetMapping("/liquidaciones")
    @PreAuthorize(LECTURA_CARTERA)
    public ResponseEntity<List<LiquidacionResponse>> liquidaciones() {
        return ResponseEntity.ok(liquidaciones.listar());
    }

    @GetMapping("/liquidaciones/{id}")
    @PreAuthorize(LECTURA_CARTERA)
    public ResponseEntity<LiquidacionResponse> liquidacion(@PathVariable UUID id) {
        return ResponseEntity.ok(liquidaciones.obtener(id));
    }

    @GetMapping("/liquidaciones/{id}/cuentas")
    @PreAuthorize(LECTURA_CARTERA)
    public ResponseEntity<List<CuentaAseguradoraResponse>> cuentasDeLiquidacion(@PathVariable UUID id) {
        return ResponseEntity.ok(liquidaciones.cuentasDe(id));
    }

    @PostMapping("/liquidaciones")
    @PreAuthorize(GESTION_CARTERA)
    public ResponseEntity<LiquidacionResponse> crearLiquidacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody LiquidacionCrearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(liquidaciones.crear(principal, request));
    }

    @PutMapping("/liquidaciones/{id}/enviar")
    @PreAuthorize(GESTION_CARTERA)
    public ResponseEntity<LiquidacionResponse> enviarLiquidacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(liquidaciones.enviar(principal, id));
    }

    @PutMapping("/liquidaciones/{id}/pago")
    @PreAuthorize(GESTION_CARTERA)
    public ResponseEntity<LiquidacionResponse> registrarPago(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody LiquidacionPagoRequest request) {
        return ResponseEntity.ok(liquidaciones.registrarPago(principal, id, request));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('FINANZAS', 'GERENCIA', 'ADMIN', 'RECEPCION')")
    public ResponseEntity<SegurosResumenResponse> resumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(reportes.resumen(principal));
    }
}
