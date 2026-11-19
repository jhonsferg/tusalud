package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.EntregaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.PaginaEntregasResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.ResumenEntregasResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.services.canales.EntregasService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/canales/entregas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Entregas de notificaciones", description = "Bitácora de envíos externos, sin contenido de los mensajes")
public class EntregasController {

    private final EntregasService servicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERACIONES')")
    @Operation(
            summary = "Listar entregas",
            description =
                    "Estado y motivo de cada envío por canal. El destino va enmascarado y no se incluye el texto enviado.")
    public ResponseEntity<PaginaEntregasResponse> listar(
            @RequestParam(required = false) Canal canal,
            @RequestParam(required = false) EstadoEntrega estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(servicio.listar(canal, estado, desde, hasta, pagina, tamano));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERACIONES')")
    @Operation(summary = "Resumen de entregas por canal y estado")
    public ResponseEntity<ResumenEntregasResponse> resumen(@RequestParam(defaultValue = "7") int dias) {
        return ResponseEntity.ok(servicio.resumen(dias));
    }

    @PostMapping("/{id}/reintentar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reintentar una entrega fallida")
    public ResponseEntity<EntregaResponse> reintentar(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.reintentar(id));
    }
}
