package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ArchivoResponse;
import com.codesoftlabs.tusalud.app.entities.Archivo;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService.Descarga;
import com.codesoftlabs.tusalud.app.storage.PropositoArchivo;
import com.codesoftlabs.tusalud.app.storage.StorageService.ObjetoAlmacenado;
import com.codesoftlabs.tusalud.app.storage.TipoContenido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/archivos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Archivos", description = "Almacenamiento de archivos con descarga autorizada por el backend")
public class ArchivosController {

    private final ArchivoService archivoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Subir un archivo",
            description =
                    "Valida el tipo por su contenido, el tamaño y la extensión según el propósito y lo guarda en el"
                            + " almacén de objetos. Las imágenes diagnósticas se cargan desde la orden de imagen.")
    public ResponseEntity<ArchivoResponse> subir(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("proposito") PropositoArchivo proposito,
            @RequestParam(value = "referenciaId", required = false) UUID referenciaId,
            @RequestParam(value = "referenciaTipo", required = false) String referenciaTipo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new DatosInvalidosException("No se pudo leer el archivo cargado");
        }
        ArchivoResponse respuesta = archivoService.subir(
                principal, proposito, archivo.getOriginalFilename(), contenido, referenciaId, referenciaTipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Descargar un archivo",
            description = "Se sirve solo con sesión y con acceso según el propósito, con soporte de Range y ETag.")
    public ResponseEntity<Resource> descargar(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rango,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String condicion,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        Descarga descarga = archivoService.descargar(principal, id, rango);
        Archivo archivo = descarga.archivo();
        String etag = "\"" + archivo.getSha256() + "\"";
        if (!descarga.parcial() && etag.equals(condicion)) {
            cerrar(descarga.objeto());
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .build();
        }
        TipoContenido tipo = TipoContenido.desdeMime(archivo.getTipoContenido());
        boolean enLinea = tipo != null && tipo.visualizable();
        ContentDisposition disposicion = ContentDisposition.builder(enLinea ? "inline" : "attachment")
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
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(descarga.parcial() ? 206 : 200)
                .contentType(MediaType.parseMediaType(archivo.getTipoContenido()))
                .contentLength(descarga.longitud())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .eTag(etag)
                .cacheControl(CacheControl.noCache().cachePrivate());
        if (descarga.parcial()) {
            respuesta.header(
                    HttpHeaders.CONTENT_RANGE,
                    "bytes " + descarga.desde() + "-" + descarga.hasta() + "/" + archivo.getTamano());
        }
        return respuesta.body(cuerpo);
    }

    @GetMapping("/{id}/metadatos")
    @Operation(
            summary = "Datos de un archivo",
            description = "Nombre, tipo, tamaño y huella sin descargar el contenido.")
    public ResponseEntity<ArchivoResponse> metadatos(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(archivoService.metadatos(principal, id));
    }

    @GetMapping
    @Operation(
            summary = "Archivos de una referencia",
            description = "Lista los archivos de un objeto del dominio a los que el usuario tiene acceso.")
    public ResponseEntity<List<ArchivoResponse>> listar(
            @RequestParam("referenciaTipo") String referenciaTipo,
            @RequestParam("referenciaId") UUID referenciaId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(archivoService.listar(principal, referenciaTipo, referenciaId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un archivo", description = "Elimina el archivo y su objeto según el propósito.")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        archivoService.eliminar(principal, id);
        return ResponseEntity.noContent().build();
    }

    private static void cerrar(ObjetoAlmacenado objeto) {
        try {
            objeto.close();
        } catch (IOException ex) {
            return;
        }
    }
}
