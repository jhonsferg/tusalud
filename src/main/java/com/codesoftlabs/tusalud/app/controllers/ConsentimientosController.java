package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoPlantillaRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoSolicitudRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.FirmaRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.MotivoRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoPlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoResumen;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.ConsentimientoService;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.DocumentoMedicoService.PdfDocumento;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/consentimientos")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Consentimientos informados",
        description = "Plantillas por procedimiento, solicitud, firma manuscrita y revocación")
public class ConsentimientosController {

    private final ConsentimientoService servicio;

    @GetMapping("/plantillas")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    public ResponseEntity<List<ConsentimientoPlantillaResponse>> plantillas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.plantillas(actor));
    }

    @PostMapping("/plantillas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConsentimientoPlantillaResponse> crearPlantilla(
            @Valid @RequestBody ConsentimientoPlantillaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearPlantilla(request));
    }

    @PutMapping("/plantillas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConsentimientoPlantillaResponse> actualizarPlantilla(
            @PathVariable UUID id, @Valid @RequestBody ConsentimientoPlantillaRequest request) {
        return ResponseEntity.ok(servicio.actualizarPlantilla(id, request));
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ConsentimientoResumen> solicitar(
            @Valid @RequestBody ConsentimientoSolicitudRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.solicitar(actor, request));
    }

    @PostMapping("/{id}/firma")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ConsentimientoDetalle> firmar(
            @PathVariable UUID id,
            @Valid @RequestBody FirmaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.firmar(actor, id, request));
    }

    @PostMapping("/{id}/revocar")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ConsentimientoDetalle> revocar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.revocar(actor, id, request.motivo()));
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<ConsentimientoResumen>> mios(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.listarMios(actor));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<ConsentimientoResumen>> delPaciente(
            @RequestParam UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.listarDePaciente(actor, pacienteId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<ConsentimientoDetalle> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.obtener(actor, id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('PACIENTE', 'MEDICO')")
    public ResponseEntity<byte[]> pdf(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        PdfDocumento documento = servicio.pdf(actor, id);
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
}
