package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.AnulacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.DosisRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.EsquemaRequest;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CarneResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.EsquemaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.PacienteClinicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.RegistroVacunaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.VacunaResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.pediatria.VacunacionService;
import com.codesoftlabs.tusalud.app.services.pediatria.VacunacionService.PdfCarne;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vacunacion")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vacunación",
        description = "Esquema de vacunación configurable, carné por paciente, registro de dosis y recordatorios")
public class VacunacionController {

    private final VacunacionService servicio;

    @GetMapping("/vacunas")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<List<VacunaResponse>> vacunas() {
        return ResponseEntity.ok(servicio.catalogo());
    }

    @GetMapping("/esquema")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<EsquemaResponse> esquemaVigente() {
        return ResponseEntity.ok(servicio.esquemaVigente());
    }

    @GetMapping("/esquemas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EsquemaResponse>> esquemas() {
        return ResponseEntity.ok(servicio.listarEsquemas());
    }

    @PostMapping("/esquemas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EsquemaResponse> crearEsquema(
            @Valid @RequestBody EsquemaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearEsquema(actor, request));
    }

    @GetMapping("/pacientes")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<List<PacienteClinicoResumen>> pacientes(
            @RequestParam(required = false) String q,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.pacientes(actor, q));
    }

    @GetMapping("/pacientes/{pacienteId}/carne")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'PACIENTE')")
    public ResponseEntity<CarneResponse> carne(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.carne(actor, pacienteId));
    }

    @GetMapping("/mio")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CarneResponse> carneMio(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.carneMio(actor));
    }

    @GetMapping("/pacientes/{pacienteId}/carne/pdf")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'PACIENTE')")
    public ResponseEntity<byte[]> pdf(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        PdfCarne documento = servicio.pdf(actor, pacienteId);
        ContentDisposition disposicion = ContentDisposition.builder("inline")
                .filename(documento.nombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(documento.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.noStore())
                .body(documento.contenido());
    }

    @PostMapping("/dosis")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<RegistroVacunaResponse> registrar(
            @Valid @RequestBody DosisRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrarDosis(actor, request));
    }

    @PostMapping("/dosis/{id}/anular")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<RegistroVacunaResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody AnulacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.anular(actor, id, request.motivo()));
    }
}
