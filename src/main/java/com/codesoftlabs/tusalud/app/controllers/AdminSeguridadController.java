package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.PaginaEventosSeguridadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenSeguridadResponse;
import com.codesoftlabs.tusalud.app.entities.enums.SeveridadSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import com.codesoftlabs.tusalud.app.services.interfaces.EventoSeguridadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seguridad")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Seguridad operativa", description = "Eventos de seguridad de la plataforma, solo administración")
public class AdminSeguridadController {

    private final EventoSeguridadService eventoService;

    @GetMapping("/eventos")
    @Operation(
            summary = "Eventos de seguridad",
            description = "Lista paginada con filtros por tipo, severidad, rango de fechas y usuario.")
    public ResponseEntity<PaginaEventosSeguridadResponse> eventos(
            @RequestParam(required = false) TipoEventoSeguridad tipo,
            @RequestParam(required = false) SeveridadSeguridad severidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(eventoService.listar(tipo, severidad, desde, hasta, usuarioId, pagina, tamano));
    }

    @GetMapping("/resumen")
    @Operation(
            summary = "Resumen de seguridad",
            description =
                    "Conteos por tipo y severidad de las últimas 24 horas y 7 días, e inicios de sesión inusuales.")
    public ResponseEntity<ResumenSeguridadResponse> resumen() {
        return ResponseEntity.ok(eventoService.resumen());
    }
}
