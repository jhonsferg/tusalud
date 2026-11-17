package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CompletarEventoRequest;
import com.codesoftlabs.tusalud.app.dto.request.EventoCalendarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MoverEventoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.EventoCalendarioResponse;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvento;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoCalendarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/calendario")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Calendario", description = "Eventos personales: tareas, reuniones, recordatorios y bloqueos de agenda")
public class EventoCalendarioController {

    private final EventoCalendarioService eventoService;

    @GetMapping("/tipos")
    @Operation(
            summary = "Tipos de evento permitidos",
            description = "Devuelve los tipos de evento que el rol del usuario autenticado puede crear.")
    @ApiResponse(responseCode = "200", description = "Tipos permitidos")
    public ResponseEntity<List<TipoEvento>> tipos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(eventoService.tiposPermitidos(principal));
    }

    @GetMapping("/eventos")
    @Operation(
            summary = "Listar los eventos del usuario en un rango",
            description = "Devuelve los eventos propios que se solapan con el rango de fechas, de hasta 100 días.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Eventos del rango"),
        @ApiResponse(
                responseCode = "400",
                description = "Rango inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EventoCalendarioResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Primer día (yyyy-MM-dd)", example = "2026-11-01")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Último día (yyyy-MM-dd)", example = "2026-11-30")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin) {
        return ResponseEntity.ok(eventoService.listar(principal, inicio, fin));
    }

    @PostMapping("/eventos")
    @Operation(summary = "Crear un evento", description = "Los bloqueos de agenda solo los crean los médicos.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento creado"),
        @ApiResponse(
                responseCode = "403",
                description = "Solo los médicos crean bloqueos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El bloqueo se solapa con citas activas",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EventoCalendarioResponse> crear(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody EventoCalendarioRequest request) {
        EventoCalendarioResponse creado = eventoService.crear(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/eventos/{id}")
    @Operation(summary = "Actualizar un evento", description = "Reemplaza los datos de un evento propio.")
    public ResponseEntity<EventoCalendarioResponse> actualizar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody EventoCalendarioRequest request) {
        return ResponseEntity.ok(eventoService.actualizar(principal, id, request));
    }

    @PutMapping("/eventos/{id}/mover")
    @Operation(
            summary = "Mover o redimensionar un evento",
            description = "Cambia el inicio y el fin de un evento propio, por ejemplo al arrastrarlo en el calendario.")
    public ResponseEntity<EventoCalendarioResponse> mover(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody MoverEventoRequest request) {
        return ResponseEntity.ok(eventoService.mover(principal, id, request));
    }

    @PutMapping("/eventos/{id}/completado")
    @Operation(summary = "Completar una tarea", description = "Marca una tarea propia como completada o pendiente.")
    public ResponseEntity<EventoCalendarioResponse> completar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody CompletarEventoRequest request) {
        return ResponseEntity.ok(eventoService.completar(principal, id, request));
    }

    @DeleteMapping("/eventos/{id}")
    @Operation(summary = "Eliminar un evento", description = "Elimina un evento propio.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Evento eliminado"),
        @ApiResponse(
                responseCode = "404",
                description = "Evento no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        eventoService.eliminar(principal, id);
        return ResponseEntity.noContent().build();
    }
}
