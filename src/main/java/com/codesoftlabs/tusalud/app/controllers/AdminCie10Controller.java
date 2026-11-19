package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.Cie10ImportacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.Cie10ResumenResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/cie10")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Catálogo CIE-10 - Administración", description = "Versiones del catálogo e importación por CSV")
public class AdminCie10Controller {

    private final Cie10Service cie10Service;

    @GetMapping("/resumen")
    @Operation(summary = "Resumen del catálogo", description = "Totales, versión vigente e historial de versiones.")
    public ResponseEntity<Cie10ResumenResponse> resumen() {
        return ResponseEntity.ok(cie10Service.resumen());
    }

    @GetMapping("/plantilla.csv")
    @Operation(summary = "Plantilla CSV de importación")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("plantilla-cie10.csv")
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(cie10Service.plantillaCsv());
    }

    @PostMapping(value = "/importaciones/vista-previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Vista previa de una importación",
            description = "Valida el CSV y cuenta altas, cambios y errores sin modificar el catálogo.")
    public ResponseEntity<Cie10ImportacionResponse> vistaPrevia(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(name = "desactivarAusentes", defaultValue = "false") boolean desactivarAusentes,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(cie10Service.importar(archivo, null, false, desactivarAusentes, true, principal));
    }

    @PostMapping(value = "/importaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Importar el catálogo",
            description = "Aplica el CSV de forma idempotente por código y registra una versión nueva del catálogo.")
    public ResponseEntity<Cie10ImportacionResponse> importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("etiqueta") String etiqueta,
            @RequestParam(name = "desactivarAusentes", defaultValue = "false") boolean desactivarAusentes,
            @RequestParam(name = "omitirErrores", defaultValue = "false") boolean omitirErrores,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(
                cie10Service.importar(archivo, etiqueta, true, desactivarAusentes, omitirErrores, principal));
    }
}
