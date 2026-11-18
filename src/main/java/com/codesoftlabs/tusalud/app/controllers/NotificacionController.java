package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResumenResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "Notificaciones",
        description = "Avisos en tiempo real del usuario autenticado sobre su actividad en la clínica")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final NotificacionStreamService streamService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Recibir notificaciones en tiempo real",
            description =
                    "Abre un flujo Server-Sent Events. Emite el evento conectado al iniciar, el evento notificacion con el JSON de cada aviso nuevo y el evento ping cada 25 segundos como latido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flujo de eventos abierto"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public SseEmitter stream(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return streamService.suscribir(principal.getId());
    }

    @GetMapping
    @Operation(
            summary = "Listar mis notificaciones",
            description = "Devuelve las notificaciones del usuario, de la más reciente a la más antigua.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de notificaciones"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<NotificacionResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Solo notificaciones no leídas", example = "false")
                    @RequestParam(defaultValue = "false")
                    boolean soloNoLeidas,
            @Parameter(description = "Cantidad máxima de resultados (1 a 100)", example = "50")
                    @RequestParam(defaultValue = "50")
                    @Min(1)
                    @Max(100)
                    int limit) {
        return ResponseEntity.ok(notificacionService.listar(principal, soloNoLeidas, limit));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de notificaciones", description = "Devuelve la cantidad de notificaciones no leídas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen de notificaciones"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificacionResumenResponse> resumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(notificacionService.resumen(principal));
    }

    @PutMapping("/{id}/leida")
    @Operation(summary = "Marcar notificación como leída", description = "Solo el dueño de la notificación.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
        @ApiResponse(
                responseCode = "404",
                description = "Notificación no encontrada para el usuario",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @Parameter(description = "ID de la notificación", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(notificacionService.marcarLeida(principal, id));
    }

    @PutMapping("/leidas")
    @Operation(summary = "Marcar todas como leídas")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Notificaciones marcadas como leídas")})
    public ResponseEntity<Void> marcarTodasLeidas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        notificacionService.marcarTodasLeidas(principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificación", description = "Solo el dueño de la notificación.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notificación eliminada"),
        @ApiResponse(
                responseCode = "404",
                description = "Notificación no encontrada para el usuario",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID de la notificación", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        notificacionService.eliminar(principal, id);
        return ResponseEntity.noContent().build();
    }
}
