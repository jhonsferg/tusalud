package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.CicloRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.EsterilizadorRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.IndicadoresRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SetInstrumentalRequest;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.AlertaEsterilizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CicloResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.EsterilizadorResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.LoteEsterilResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.SetInstrumentalResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.TrazabilidadLoteResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.quirofano.EsterilizacionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/esterilizacion")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Esterilización",
        description = "Ciclos de esterilización, sets de instrumental, lotes, trazabilidad y alertas")
public class EsterilizacionController {

    private final EsterilizacionService esterilizacion;

    @GetMapping("/esterilizadores")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD', 'ADMIN')")
    public ResponseEntity<List<EsterilizadorResponse>> esterilizadores() {
        return ResponseEntity.ok(esterilizacion.esterilizadores());
    }

    @PostMapping("/esterilizadores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EsterilizadorResponse> crearEsterilizador(@Valid @RequestBody EsterilizadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(esterilizacion.crearEsterilizador(request));
    }

    @PutMapping("/esterilizadores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EsterilizadorResponse> actualizarEsterilizador(
            @PathVariable UUID id, @Valid @RequestBody EsterilizadorRequest request) {
        return ResponseEntity.ok(esterilizacion.actualizarEsterilizador(id, request));
    }

    @GetMapping("/sets")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'CALIDAD', 'ADMIN')")
    public ResponseEntity<List<SetInstrumentalResponse>> sets() {
        return ResponseEntity.ok(esterilizacion.sets());
    }

    @PostMapping("/sets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SetInstrumentalResponse> crearSet(@Valid @RequestBody SetInstrumentalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(esterilizacion.crearSet(request));
    }

    @PutMapping("/sets/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SetInstrumentalResponse> actualizarSet(
            @PathVariable UUID id, @Valid @RequestBody SetInstrumentalRequest request) {
        return ResponseEntity.ok(esterilizacion.actualizarSet(id, request));
    }

    @GetMapping("/ciclos")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<List<CicloResponse>> ciclos(@RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(esterilizacion.ciclos(limite));
    }

    @GetMapping("/ciclos/{id}")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<CicloResponse> ciclo(@PathVariable UUID id) {
        return ResponseEntity.ok(esterilizacion.ciclo(id));
    }

    @PostMapping("/ciclos")
    @PreAuthorize("hasRole('ENFERMERIA')")
    public ResponseEntity<CicloResponse> crearCiclo(
            @Valid @RequestBody CicloRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(esterilizacion.crearCiclo(actor, request));
    }

    @PutMapping("/ciclos/{id}/indicadores")
    @PreAuthorize("hasRole('ENFERMERIA')")
    public ResponseEntity<CicloResponse> indicadores(
            @PathVariable UUID id,
            @Valid @RequestBody IndicadoresRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(esterilizacion.registrarIndicadores(actor, id, request));
    }

    @GetMapping("/ciclos/{id}/trazabilidad")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<List<TrazabilidadLoteResponse>> trazabilidadCiclo(@PathVariable UUID id) {
        return ResponseEntity.ok(esterilizacion.trazabilidadCiclo(id));
    }

    @GetMapping("/lotes")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<List<LoteEsterilResponse>> lotes(
            @RequestParam(defaultValue = "false") boolean soloUtilizables) {
        return ResponseEntity.ok(esterilizacion.lotes(soloUtilizables));
    }

    @GetMapping("/lotes/{id}/trazabilidad")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<TrazabilidadLoteResponse> trazabilidadLote(@PathVariable UUID id) {
        return ResponseEntity.ok(esterilizacion.trazabilidadLote(id));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'CALIDAD')")
    public ResponseEntity<List<AlertaEsterilizacionResponse>> alertas() {
        return ResponseEntity.ok(esterilizacion.alertas());
    }
}
