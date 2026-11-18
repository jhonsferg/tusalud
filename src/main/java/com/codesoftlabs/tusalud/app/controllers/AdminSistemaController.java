package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.EstadoSistemaResponse;
import com.codesoftlabs.tusalud.app.dto.response.UsoAlmacenamientoResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.SistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Sistema", description = "Estado de los servicios y uso del almacenamiento, solo administración")
public class AdminSistemaController {

    private final SistemaService sistemaService;

    @GetMapping("/sistema/estado")
    @Operation(
            summary = "Estado del sistema",
            description = "Salud de la base de datos y del almacén, versión, tiempo en marcha y migraciones aplicadas.")
    public ResponseEntity<EstadoSistemaResponse> estado() {
        return ResponseEntity.ok(sistemaService.estado());
    }

    @GetMapping("/almacenamiento/uso")
    @Operation(summary = "Uso del almacenamiento", description = "Objetos y bytes por bucket y propósito.")
    public ResponseEntity<UsoAlmacenamientoResponse> uso() {
        return ResponseEntity.ok(sistemaService.usoAlmacenamiento());
    }
}
