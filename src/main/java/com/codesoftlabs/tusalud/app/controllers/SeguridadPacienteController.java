package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.AnalisisRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.CierreEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.EventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CalidadRequests.GuiaRequest;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.EventoResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.GuiaDetalleResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.GuiaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.CalidadResponses.TableroResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoEvento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.calidad.EventosAdversosService;
import com.codesoftlabs.tusalud.app.services.calidad.GuiasClinicasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/seguridad-paciente")
@RequiredArgsConstructor
@Validated
@Tag(name = "Seguridad del paciente", description = "Eventos adversos y guías de práctica clínica")
public class SeguridadPacienteController {

    private static final String PERSONAL = "isAuthenticated() and !hasRole('PACIENTE')";

    private final EventosAdversosService eventos;
    private final GuiasClinicasService guias;

    @PostMapping("/eventos")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Notificar un evento adverso o casi incidente")
    public ResponseEntity<EventoResumenResponse> notificar(
            @Valid @RequestBody EventoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventos.notificar(actor, request));
    }

    @GetMapping("/eventos/mios")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Reportes que notifiqué")
    public List<EventoResumenResponse> mios(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.mios(actor);
    }

    @GetMapping("/eventos")
    @PreAuthorize("hasAnyRole('CALIDAD', 'GERENCIA')")
    @Operation(summary = "Todos los reportes")
    public List<EventoResumenResponse> listar(
            @RequestParam(required = false) EstadoEvento estado,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.listar(actor, estado);
    }

    @GetMapping("/eventos/{id}")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Detalle de un reporte")
    public EventoDetalleResponse detalle(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.detalle(actor, id);
    }

    @PostMapping("/eventos/{id}/analisis")
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Registrar el análisis de causa")
    public EventoDetalleResponse analizar(
            @PathVariable UUID id,
            @Valid @RequestBody AnalisisRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.analizar(actor, id, request);
    }

    @PostMapping("/eventos/{id}/acciones")
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Agregar una acción de mejora")
    public EventoDetalleResponse agregarAccion(
            @PathVariable UUID id,
            @Valid @RequestBody AccionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.agregarAccion(actor, id, request);
    }

    @PostMapping("/acciones/{accionId}/completar")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Completar una acción de mejora")
    public EventoDetalleResponse completar(
            @PathVariable UUID accionId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.completarAccion(actor, accionId);
    }

    @PostMapping("/eventos/{id}/cierre")
    @PreAuthorize("hasRole('CALIDAD')")
    @Operation(summary = "Cerrar el reporte con verificación de eficacia")
    public EventoDetalleResponse cerrar(
            @PathVariable UUID id,
            @Valid @RequestBody CierreEventoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.cerrar(actor, id, request);
    }

    @GetMapping("/tablero")
    @PreAuthorize("hasAnyRole('CALIDAD', 'GERENCIA')")
    @Operation(summary = "Indicadores y tendencias")
    public TableroResponse tablero(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return eventos.tablero(actor);
    }

    @GetMapping("/eventos/exportacion")
    @PreAuthorize("hasAnyRole('CALIDAD', 'GERENCIA')")
    @Operation(summary = "Exportar los reportes a Excel")
    public ResponseEntity<byte[]> exportar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"eventos-adversos.xlsx\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(eventos.exportar(actor));
    }

    @GetMapping("/guias")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Biblioteca de guías de práctica clínica")
    public List<GuiaResumenResponse> buscarGuias(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID especialidadId,
            @RequestParam(defaultValue = "true") boolean soloVigentes) {
        return guias.buscar(q, especialidadId, soloVigentes);
    }

    @GetMapping("/guias/sugeridas")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Guías aplicables a los diagnósticos CIE-10 indicados")
    public List<GuiaResumenResponse> sugeridas(@RequestParam List<String> codigos) {
        return guias.sugeridas(codigos);
    }

    @GetMapping("/guias/{id}")
    @PreAuthorize(PERSONAL)
    @Operation(summary = "Detalle de una guía con sus versiones")
    public GuiaDetalleResponse guia(@PathVariable UUID id) {
        return guias.detalle(id);
    }

    @PostMapping("/guias")
    @PreAuthorize("hasAnyRole('CALIDAD', 'ADMIN')")
    @Operation(summary = "Crear una guía")
    public ResponseEntity<GuiaDetalleResponse> crearGuia(
            @Valid @RequestBody GuiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guias.crear(actor, request));
    }

    @PostMapping("/guias/{id}/versiones")
    @PreAuthorize("hasAnyRole('CALIDAD', 'ADMIN')")
    @Operation(summary = "Publicar una nueva versión de una guía")
    public ResponseEntity<GuiaDetalleResponse> nuevaVersion(
            @PathVariable UUID id,
            @Valid @RequestBody GuiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guias.nuevaVersion(actor, id, request));
    }
}
