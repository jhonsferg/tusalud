package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.Cie10FrecuenteResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10Response;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cie10")
@RequiredArgsConstructor
@Tag(name = "Catálogo CIE-10", description = "Búsqueda de diagnósticos codificados con la clasificación CIE-10")
public class Cie10Controller {

    private final Cie10Service cie10Service;

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN', 'CALIDAD', 'OPERACIONES', 'GERENCIA')")
    @Operation(
            summary = "Buscar diagnósticos",
            description =
                    "Busca por código o por texto sin distinguir tildes ni mayúsculas. Requiere al menos 2 caracteres.")
    public ResponseEntity<List<Cie10Response>> buscar(
            @RequestParam(name = "q", defaultValue = "") String consulta,
            @RequestParam(name = "limite", defaultValue = "15") int limite) {
        return ResponseEntity.ok(cie10Service.buscar(consulta, limite));
    }

    @GetMapping("/mis-frecuentes")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Mis diagnósticos frecuentes",
            description =
                    "Los diagnósticos que el médico autenticado registró con más frecuencia en los últimos 90 días.")
    public ResponseEntity<List<Cie10FrecuenteResponse>> misFrecuentes(
            @RequestParam(name = "limite", defaultValue = "8") int limite,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(cie10Service.frecuentes(principal, limite));
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN', 'CALIDAD', 'OPERACIONES', 'GERENCIA')")
    @Operation(summary = "Diagnóstico por código")
    public ResponseEntity<Cie10Response> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(cie10Service.obtener(codigo));
    }
}
