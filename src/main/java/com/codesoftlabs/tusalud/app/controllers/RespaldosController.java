package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.RespaldoEjecucionRequest;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.EstadoRespaldosResponse;
import com.codesoftlabs.tusalud.app.dto.response.CanalesResponses.RespaldoEjecucionResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.services.respaldos.RespaldosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/respaldos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Respaldos", description = "Historial de respaldos y pruebas de restauración")
public class RespaldosController {

    private final RespaldosService servicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERACIONES')")
    @Operation(summary = "Historial de respaldos y pruebas de restauración")
    public ResponseEntity<List<RespaldoEjecucionResponse>> listar(
            @RequestParam(required = false) TipoEjecucionRespaldo tipo, @RequestParam(defaultValue = "30") int limite) {
        return ResponseEntity.ok(servicio.listar(tipo, limite));
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERACIONES')")
    @Operation(
            summary = "Estado de los respaldos",
            description =
                    "Último respaldo y última prueba de restauración exitosos, vencimientos y objetivos de RPO y RTO.")
    public ResponseEntity<EstadoRespaldosResponse> estado() {
        return ResponseEntity.ok(servicio.estado());
    }

    @PostMapping("/ejecuciones")
    @Operation(
            summary = "Registrar el resultado de un respaldo o de una prueba de restauración",
            description =
                    "Lo invoca el contenedor de respaldos. No usa sesión: exige la cabecera X-Respaldo-Token con el secreto compartido.")
    public ResponseEntity<RespaldoEjecucionResponse> registrar(
            @RequestHeader(value = "X-Respaldo-Token", required = false) String token,
            @Valid @RequestBody RespaldoEjecucionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(token, request));
    }
}
