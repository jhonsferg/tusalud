package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ActualizarDocumentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.DocumentoMedicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.EpicrisisBorradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.MotivoRequest;
import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ReferenciaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ReferenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ResumenIngresoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.DocumentoMedicoService;
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
@RequestMapping("/api/v1/documentos-medicos")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Documentos médicos",
        description =
                "Certificados, descansos, informes, referencias, contrarreferencias y epicrisis con ciclo de vida inmutable")
public class DocumentosMedicosController {

    private final DocumentoMedicoService servicio;

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DocumentoMedicoDetalle> crear(
            @Valid @RequestBody DocumentoMedicoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearBorrador(actor, request));
    }

    @PostMapping("/epicrisis/borrador")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DocumentoMedicoDetalle> epicrisis(
            @Valid @RequestBody EpicrisisBorradorRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.epicrisisBorrador(actor, request.ingresoId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DocumentoMedicoDetalle> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarDocumentoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.actualizarBorrador(actor, id, request));
    }

    @PostMapping("/{id}/emitir")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DocumentoMedicoDetalle> emitir(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.emitir(actor, id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<DocumentoMedicoDetalle> anular(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.anular(actor, id, request.motivo()));
    }

    @PutMapping("/{id}/referencia/estado")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<ReferenciaResponse> estadoReferencia(
            @PathVariable UUID id,
            @Valid @RequestBody ReferenciaEstadoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.cambiarEstadoReferencia(actor, id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<DocumentoMedicoDetalle> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.obtener(actor, id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
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

    @GetMapping("/mios")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<DocumentoMedicoResumen>> mios(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.listarMios(actor));
    }

    @GetMapping("/emitidos")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<DocumentoMedicoResumen>> emitidos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.listarEmitidosPorMi(actor));
    }

    @GetMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<DocumentoMedicoResumen>> delPaciente(
            @RequestParam UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.listarDePaciente(actor, pacienteId));
    }

    @GetMapping("/ingreso/{ingresoId}/resumen")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<ResumenIngresoResponse> resumenIngreso(
            @PathVariable UUID ingresoId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.resumenIngreso(actor, ingresoId));
    }
}
