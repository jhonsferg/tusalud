package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.PlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaVersionResponse;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PlantillaDocumentoService;
import com.codesoftlabs.tusalud.app.services.interfaces.PlantillaDocumentoService.PlantillaContenido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/plantillas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Plantillas de documentos", description = "Gestión de las plantillas JRXML con las que se generan los PDF")
public class PlantillaController {

    private final PlantillaDocumentoService plantillaService;

    @GetMapping
    @Operation(summary = "Plantillas con su versión vigente")
    public ResponseEntity<List<PlantillaResponse>> listar() {
        return ResponseEntity.ok(plantillaService.listar());
    }

    @GetMapping("/{codigo}/descargar")
    @Operation(summary = "Descargar la plantilla vigente en JRXML")
    public ResponseEntity<byte[]> descargar(@PathVariable String codigo) {
        PlantillaContenido plantilla = plantillaService.descargar(codigo);
        ContentDisposition disposicion = ContentDisposition.builder("attachment")
                .filename(plantilla.nombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(plantilla.contenido());
    }

    @GetMapping("/{codigo}/historial")
    @Operation(summary = "Historial de versiones de una plantilla")
    public ResponseEntity<List<PlantillaVersionResponse>> historial(@PathVariable String codigo) {
        return ResponseEntity.ok(plantillaService.historial(codigo));
    }

    @PutMapping(value = "/{codigo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Reemplazar una plantilla",
            description = "Valida que el JRXML compile y que no use scripts, importaciones, subinformes ni recursos"
                    + " externos. La versión anterior se conserva en el historial.")
    public ResponseEntity<PlantillaResponse> reemplazar(
            @PathVariable String codigo,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "motivo", required = false) String motivo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new DatosInvalidosException("No se pudo leer el archivo cargado");
        }
        return ResponseEntity.ok(plantillaService.reemplazar(principal, codigo, contenido, motivo));
    }

    @PostMapping("/{codigo}/restaurar")
    @Operation(summary = "Volver a la plantilla predeterminada")
    public ResponseEntity<PlantillaResponse> restaurar(
            @PathVariable String codigo,
            @RequestParam(value = "motivo", required = false) String motivo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(plantillaService.restaurar(principal, codigo, motivo));
    }
}
