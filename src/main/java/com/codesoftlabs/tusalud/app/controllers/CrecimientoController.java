package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.AnulacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.MedicionRequest;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CrecimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.MedicionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.pediatria.CrecimientoService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crecimiento")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Crecimiento",
        description = "Mediciones antropométricas, curvas de referencia, percentiles y alertas para menores de 19 años")
public class CrecimientoController {

    private final CrecimientoService servicio;

    @GetMapping("/pacientes/{pacienteId}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'PACIENTE')")
    public ResponseEntity<CrecimientoResponse> curvas(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.curvas(actor, pacienteId));
    }

    @GetMapping("/mio")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CrecimientoResponse> mias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.curvasMias(actor));
    }

    @PostMapping("/mediciones")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<MedicionResponse> registrar(
            @Valid @RequestBody MedicionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(actor, request));
    }

    @PostMapping("/mediciones/{id}/anular")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<MedicionResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody AnulacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.anular(actor, id, request.motivo()));
    }
}
