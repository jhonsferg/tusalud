package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.InteraccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.MedicamentoRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.PrincipioActivoRequest;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.ImportacionCatalogoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.InteraccionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.MedicamentoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.PrincipioActivoResponse;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.services.receta.CatalogoMedicamentosService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/medicamentos")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Catálogo de medicamentos",
        description =
                "Medicamentos por denominación común, principios activos, interacciones y carga masiva. Los datos de demostración están por validar con un químico farmacéutico")
public class MedicamentosController {

    private static final long LIMITE_ARCHIVO = 2L * 1024 * 1024;

    private final CatalogoMedicamentosService servicio;

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'FARMACIA', 'ALMACEN', 'ADMIN')")
    public ResponseEntity<List<MedicamentoResponse>> buscar(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicio.buscar(q));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'FARMACIA', 'ALMACEN', 'ADMIN')")
    public ResponseEntity<MedicamentoResponse> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> crear(@Valid @RequestBody MedicamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearMedicamento(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicamentoResponse> actualizar(
            @PathVariable UUID id, @Valid @RequestBody MedicamentoRequest request) {
        return ResponseEntity.ok(servicio.actualizarMedicamento(id, request));
    }

    @GetMapping("/principios-activos")
    @PreAuthorize("hasAnyRole('MEDICO', 'FARMACIA', 'ADMIN')")
    public ResponseEntity<List<PrincipioActivoResponse>> principios() {
        return ResponseEntity.ok(servicio.listarPrincipios());
    }

    @PostMapping("/principios-activos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrincipioActivoResponse> crearPrincipio(@Valid @RequestBody PrincipioActivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearPrincipio(request));
    }

    @PutMapping("/principios-activos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PrincipioActivoResponse> actualizarPrincipio(
            @PathVariable UUID id, @Valid @RequestBody PrincipioActivoRequest request) {
        return ResponseEntity.ok(servicio.actualizarPrincipio(id, request));
    }

    @GetMapping("/interacciones")
    @PreAuthorize("hasAnyRole('MEDICO', 'FARMACIA', 'ADMIN')")
    public ResponseEntity<List<InteraccionResponse>> interacciones() {
        return ResponseEntity.ok(servicio.listarInteracciones());
    }

    @PostMapping("/interacciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InteraccionResponse> crearInteraccion(@Valid @RequestBody InteraccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearInteraccion(request));
    }

    @DeleteMapping("/interacciones/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarInteraccion(@PathVariable UUID id) {
        servicio.eliminarInteraccion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/importaciones/plantilla")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> plantilla() {
        byte[] contenido = servicio.plantillaCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("plantilla-medicamentos.csv", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(contenido);
    }

    @PostMapping(value = "/importaciones/vista-previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportacionCatalogoResponse> vistaPrevia(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return ResponseEntity.ok(servicio.importar(leer(archivo), true));
    }

    @PostMapping(value = "/importaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportacionCatalogoResponse> importar(@RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        return ResponseEntity.ok(servicio.importar(leer(archivo), false));
    }

    private byte[] leer(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new DatosInvalidosException("El archivo está vacío");
        }
        if (archivo.getSize() > LIMITE_ARCHIVO) {
            throw new DatosInvalidosException("El archivo supera 2 MB");
        }
        return archivo.getBytes();
    }
}
