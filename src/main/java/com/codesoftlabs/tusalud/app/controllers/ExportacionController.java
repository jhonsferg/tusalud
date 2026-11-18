package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ExportacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.ExportacionResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import com.codesoftlabs.tusalud.app.services.interfaces.ExportacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exportaciones")
@RequiredArgsConstructor
@Validated
@PreAuthorize(
        "hasAnyRole('ADMIN', 'GERENCIA', 'FINANZAS', 'OPERACIONES', 'CALIDAD', 'CAJA', 'ALMACEN', 'RRHH', 'MEDICO')")
@Tag(name = "Exportaciones", description = "Reportes y listados en PDF o Excel generados en segundo plano")
public class ExportacionController {

    private final ExportacionService exportacionService;

    @PostMapping
    @Operation(
            summary = "Solicitar una exportación",
            description =
                    "Responde 202 con la exportación en proceso. Cuando termina se avisa con una notificación y el"
                            + " resultado se conserva unos días en el almacén. Solo el solicitante puede descargarlo.")
    public ResponseEntity<ExportacionResponse> solicitar(
            @Valid @RequestBody ExportacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(exportacionService.solicitar(principal, request));
    }

    @GetMapping
    @Operation(summary = "Mis exportaciones")
    public ResponseEntity<List<ExportacionResponse>> mias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(exportacionService.mias(principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Estado de una exportación")
    public ResponseEntity<ExportacionResponse> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(exportacionService.obtener(principal, id));
    }

    @GetMapping("/{id}/descargar")
    @Operation(summary = "Descargar el resultado de una exportación")
    public ResponseEntity<Resource> descargar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        Descarga descarga = exportacionService.descargar(principal, id);
        Archivo archivo = descarga.archivo();
        ContentDisposition disposicion = ContentDisposition.builder("attachment")
                .filename(archivo.getNombreOriginal(), StandardCharsets.UTF_8)
                .build();
        long longitud = descarga.longitud();
        Resource cuerpo = new InputStreamResource(descarga.objeto().flujo()) {
            @Override
            public long contentLength() {
                return longitud;
            }

            @Override
            public String getFilename() {
                return archivo.getNombreOriginal();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.getTipoContenido()))
                .contentLength(longitud)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.noStore())
                .body(cuerpo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una exportación y su archivo")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        exportacionService.eliminar(principal, id);
        return ResponseEntity.noContent().build();
    }

    static void cerrar(java.io.Closeable recurso) {
        try {
            recurso.close();
        } catch (IOException ex) {
            return;
        }
    }
}
