package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.EstablecimientoRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.EstablecimientoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.EstablecimientoSaludService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/establecimientos-salud")
@RequiredArgsConstructor
@Validated
@Tag(name = "Establecimientos de salud", description = "Catálogo de destinos de referencia y contrarreferencia")
public class EstablecimientosSaludController {

    private final EstablecimientoSaludService servicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    public ResponseEntity<List<EstablecimientoResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        boolean admin = actor.getRoles().contains("ROLE_ADMIN");
        return ResponseEntity.ok(servicio.listar(!admin));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EstablecimientoResponse> crear(@Valid @RequestBody EstablecimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EstablecimientoResponse> actualizar(
            @PathVariable UUID id, @Valid @RequestBody EstablecimientoRequest request) {
        return ResponseEntity.ok(servicio.actualizar(id, request));
    }
}
