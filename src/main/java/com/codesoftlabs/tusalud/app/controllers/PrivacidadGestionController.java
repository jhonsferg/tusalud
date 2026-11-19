package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.BancoDatosRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.EncargadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.FinalidadRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteActualizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.IncidenteRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRetencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.ResolucionArcoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.BancoDatosResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EjecucionRetencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EncargadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EstadoPrivacidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.FinalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.IncidenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaRetencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.RegistroTratamientosResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.privacidad.IncidentesDatosService;
import com.codesoftlabs.tusalud.app.services.privacidad.PoliticaPrivacidadService;
import com.codesoftlabs.tusalud.app.services.privacidad.RegistroTratamientosService;
import com.codesoftlabs.tusalud.app.services.privacidad.RetencionService;
import com.codesoftlabs.tusalud.app.services.privacidad.SolicitudArcoService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/privacidad/gestion")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
@Tag(
        name = "Gestión de la protección de datos",
        description = "Registro de tratamientos, solicitudes ARCO, retención, incidentes y encargados")
public class PrivacidadGestionController {

    private final RegistroTratamientosService registro;
    private final SolicitudArcoService solicitudes;
    private final PoliticaPrivacidadService politicas;
    private final RetencionService retencion;
    private final IncidentesDatosService incidentes;

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Indicadores de cumplimiento")
    public ResponseEntity<EstadoPrivacidadResponse> estado() {
        return ResponseEntity.ok(registro.estado());
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Bandeja de solicitudes ARCO")
    public ResponseEntity<List<SolicitudArcoResponse>> bandeja(
            @RequestParam(required = false) EstadoArco estado, @RequestParam(required = false) TipoArco tipo) {
        return ResponseEntity.ok(solicitudes.bandeja(estado, tipo));
    }

    @PostMapping("/solicitudes/{id}/tomar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Asumir la responsabilidad de una solicitud")
    public ResponseEntity<SolicitudArcoResponse> tomar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(solicitudes.tomar(principal, id));
    }

    @PostMapping("/solicitudes/{id}/resolver")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(
            summary = "Resolver una solicitud",
            description =
                    "Aplica los efectos del derecho: exporta los datos, rectifica el campo, cancela lo cancelable o revoca el consentimiento.")
    public ResponseEntity<SolicitudArcoResponse> resolver(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ResolucionArcoRequest request) {
        return ResponseEntity.ok(solicitudes.resolver(principal, id, request));
    }

    @GetMapping("/registro")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Registro de tratamientos: bancos de datos, finalidades y encargados")
    public ResponseEntity<RegistroTratamientosResponse> registro() {
        return ResponseEntity.ok(registro.registro());
    }

    @PostMapping("/bancos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Registrar un banco de datos")
    public ResponseEntity<BancoDatosResponse> crearBanco(@Valid @RequestBody BancoDatosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registro.guardarBanco(null, request));
    }

    @PutMapping("/bancos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Actualizar un banco de datos")
    public ResponseEntity<BancoDatosResponse> actualizarBanco(
            @PathVariable UUID id, @Valid @RequestBody BancoDatosRequest request) {
        return ResponseEntity.ok(registro.guardarBanco(id, request));
    }

    @PostMapping("/finalidades")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Registrar una finalidad de tratamiento")
    public ResponseEntity<FinalidadResponse> crearFinalidad(@Valid @RequestBody FinalidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registro.guardarFinalidad(null, request));
    }

    @PutMapping("/finalidades/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Actualizar una finalidad de tratamiento")
    public ResponseEntity<FinalidadResponse> actualizarFinalidad(
            @PathVariable UUID id, @Valid @RequestBody FinalidadRequest request) {
        return ResponseEntity.ok(registro.guardarFinalidad(id, request));
    }

    @PostMapping("/encargados")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Registrar un encargado de tratamiento")
    public ResponseEntity<EncargadoResponse> crearEncargado(@Valid @RequestBody EncargadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registro.guardarEncargado(null, request));
    }

    @PutMapping("/encargados/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Actualizar un encargado de tratamiento")
    public ResponseEntity<EncargadoResponse> actualizarEncargado(
            @PathVariable UUID id, @Valid @RequestBody EncargadoRequest request) {
        return ResponseEntity.ok(registro.guardarEncargado(id, request));
    }

    @GetMapping("/politicas")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Versiones de la política de privacidad")
    public ResponseEntity<List<PoliticaResponse>> politicas() {
        return ResponseEntity.ok(politicas.listar());
    }

    @PostMapping("/politicas")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Crear un borrador de la política de privacidad")
    public ResponseEntity<PoliticaResponse> crearPolitica(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PoliticaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(politicas.crear(principal, request));
    }

    @PutMapping("/politicas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Editar un borrador de la política")
    public ResponseEntity<PoliticaResponse> actualizarPolitica(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PoliticaRequest request) {
        return ResponseEntity.ok(politicas.actualizar(principal, id, request));
    }

    @PostMapping("/politicas/{id}/publicar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Publicar una versión de la política, que desde entonces no se puede modificar")
    public ResponseEntity<PoliticaResponse> publicar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(politicas.publicar(principal, id));
    }

    @GetMapping("/retencion")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Políticas de retención por tipo de dato")
    public ResponseEntity<List<PoliticaRetencionResponse>> retenciones() {
        return ResponseEntity.ok(retencion.listar());
    }

    @PutMapping("/retencion/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Cambiar el plazo o la vigencia de una política de retención")
    public ResponseEntity<PoliticaRetencionResponse> actualizarRetencion(
            @PathVariable UUID id, @Valid @RequestBody PoliticaRetencionRequest request) {
        return ResponseEntity.ok(retencion.actualizar(id, request));
    }

    @PostMapping("/retencion/ejecutar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Ejecutar ahora la retención de todas las políticas activas")
    public ResponseEntity<List<EjecucionRetencionResponse>> ejecutarRetencion() {
        return ResponseEntity.ok(retencion.ejecutarTodas());
    }

    @GetMapping("/retencion/historial")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Historial de ejecuciones de la retención")
    public ResponseEntity<List<EjecucionRetencionResponse>> historialRetencion() {
        return ResponseEntity.ok(retencion.historial());
    }

    @GetMapping("/incidentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Incidentes de seguridad de datos personales")
    public ResponseEntity<List<IncidenteResponse>> incidentes() {
        return ResponseEntity.ok(incidentes.listar());
    }

    @PostMapping("/incidentes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Registrar un incidente y calcular el plazo de notificación a la autoridad")
    public ResponseEntity<IncidenteResponse> registrarIncidente(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody IncidenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentes.registrar(principal, request));
    }

    @PutMapping("/incidentes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CALIDAD')")
    @Operation(summary = "Actualizar el estado, la contención y las notificaciones de un incidente")
    public ResponseEntity<IncidenteResponse> actualizarIncidente(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody IncidenteActualizacionRequest request) {
        return ResponseEntity.ok(incidentes.actualizar(principal, id, request));
    }
}
