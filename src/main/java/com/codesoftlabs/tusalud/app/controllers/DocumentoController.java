package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.documentos.DocumentoGenerado;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.DocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@Tag(
        name = "Documentos",
        description = "Documentos PDF de un registro puntual, con la misma autorización que el dato de origen")
public class DocumentoController {

    private final DocumentoService documentoService;

    @GetMapping("/{tipo}/{id}")
    @PreAuthorize(
            "hasAnyRole('PACIENTE', 'MEDICO', 'CAJA', 'ADMIN', 'FARMACIA', 'LABORATORIO', 'IMAGENES', 'FINANZAS', 'GERENCIA',"
                    + " 'RRHH', 'ENFERMERIA', 'RECEPCION', 'ALMACEN', 'OPERACIONES', 'CALIDAD')")
    @Operation(
            summary = "Generar un documento en PDF",
            description = "Tipos: comprobante, receta, resultados-laboratorio, informe-imagen, constancia,"
                    + " resumen-historia y liquidacion. El id es el del registro de origen. El documento se genera al momento y no"
                    + " se guarda.")
    public ResponseEntity<byte[]> generar(
            @PathVariable String tipo,
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        TipoDocumento documento = TipoDocumento.desdeCodigo(tipo);
        if (documento == null || !documento.directo()) {
            throw new ResourceNotFoundException("Documento", "tipo", tipo);
        }
        DocumentoGenerado generado = documentoService.generarDirecto(principal, documento, id);
        ContentDisposition disposicion = ContentDisposition.builder("inline")
                .filename(generado.nombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(generado.mime()))
                .contentLength(generado.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.noStore())
                .body(generado.contenido());
    }
}
