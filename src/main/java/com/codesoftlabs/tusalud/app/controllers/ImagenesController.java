package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.EstudioImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.InformeImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenImagenCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ProgramarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.request.RealizarImagenRequest;
import com.codesoftlabs.tusalud.app.dto.response.AgendaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ArchivoImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.EstudioImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.ImagenesResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.InformeImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.MiFirmaImagenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenImagenResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.ModalidadImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService.ArchivoDescarga;
import com.codesoftlabs.tusalud.app.services.interfaces.ImagenesService.ArchivoEntrante;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/imagenes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Imágenes diagnósticas", description = "Catálogo, órdenes, tomas, archivos e informes")
public class ImagenesController {

    private final ImagenesService imagenesService;

    @GetMapping("/estudios")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO', 'ADMIN')")
    @Operation(summary = "Listar estudios del catálogo", description = "Filtra por texto, modalidad y estado.")
    @ApiResponse(responseCode = "200", description = "Listado de estudios")
    public ResponseEntity<List<EstudioImagenResponse>> listarEstudios(
            @Parameter(description = "Texto en el nombre, el código o la región") @RequestParam(required = false)
                    String q,
            @RequestParam(required = false) ModalidadImagen modalidad,
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(imagenesService.listarEstudios(q, modalidad, activo));
    }

    @GetMapping("/estudios/{id}")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO', 'ADMIN')")
    @Operation(summary = "Obtener un estudio")
    public ResponseEntity<EstudioImagenResponse> obtenerEstudio(@PathVariable UUID id) {
        return ResponseEntity.ok(imagenesService.obtenerEstudio(id));
    }

    @PostMapping("/estudios")
    @PreAuthorize("hasAnyRole('IMAGENES', 'ADMIN')")
    @Operation(summary = "Crear un estudio", description = "Requiere rol IMAGENES o ADMIN.")
    public ResponseEntity<EstudioImagenResponse> crearEstudio(@Valid @RequestBody EstudioImagenRequest request) {
        EstudioImagenResponse creado = imagenesService.crearEstudio(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/estudios/{id}")
    @PreAuthorize("hasAnyRole('IMAGENES', 'ADMIN')")
    @Operation(summary = "Actualizar un estudio", description = "Requiere rol IMAGENES o ADMIN.")
    public ResponseEntity<EstudioImagenResponse> actualizarEstudio(
            @PathVariable UUID id, @Valid @RequestBody EstudioImagenRequest request) {
        return ResponseEntity.ok(imagenesService.actualizarEstudio(id, request));
    }

    @PostMapping("/ordenes")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Solicitar un estudio de imagen",
            description = "El médico solicita estudios para un paciente con el que tiene relación asistencial.")
    public ResponseEntity<OrdenImagenResponse> crearOrden(
            @Valid @RequestBody OrdenImagenCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        OrdenImagenResponse creada = imagenesService.crearOrden(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/ordenes")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO')")
    @Operation(
            summary = "Listar órdenes",
            description = "El personal de imágenes ve la cola completa y el médico las órdenes que solicitó.")
    public ResponseEntity<List<OrdenImagenResponse>> listarOrdenes(
            @RequestParam(required = false) EstadoOrdenImagen estado,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PrioridadOrden prioridad,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.listarOrdenes(principal, estado, q, prioridad));
    }

    @GetMapping("/pacientes/{pacienteId}/ordenes")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Estudios de un paciente",
            description = "Solo para médicos con relación asistencial con el paciente. La consulta se audita.")
    public ResponseEntity<List<OrdenImagenResponse>> ordenesDePaciente(
            @PathVariable UUID pacienteId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.ordenesDePaciente(principal, pacienteId));
    }

    @GetMapping("/mis-estudios")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Mis estudios",
            description = "El paciente ve el informe y las imágenes solo de los estudios ya informados.")
    public ResponseEntity<List<OrdenImagenResponse>> misEstudios(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.misEstudios(principal));
    }

    @GetMapping("/ordenes/{id}")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO', 'ADMIN', 'RECEPCION', 'PACIENTE')")
    @Operation(
            summary = "Obtener una orden",
            description = "Cada rol recibe solo los campos que le corresponden: administración y recepción ven"
                    + " únicamente la agenda, y el paciente solo ve el informe de un estudio informado.")
    public ResponseEntity<OrdenImagenResponse> obtenerOrden(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.obtenerOrden(principal, id));
    }

    @PutMapping("/ordenes/{id}/programar")
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(summary = "Programar o reprogramar el estudio", description = "Valida que la sala esté libre.")
    public ResponseEntity<OrdenImagenResponse> programar(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramarImagenRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.programar(principal, id, request));
    }

    @PutMapping("/ordenes/{id}/realizar")
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(summary = "Registrar la toma", description = "Exige al menos una imagen cargada.")
    public ResponseEntity<OrdenImagenResponse> realizar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RealizarImagenRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.realizar(principal, id, request));
    }

    @PutMapping("/ordenes/{id}/informe")
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(
            summary = "Emitir o corregir el informe",
            description = "Un informe emitido solo se corrige con motivo y conserva su historial.")
    public ResponseEntity<OrdenImagenResponse> informar(
            @PathVariable UUID id,
            @Valid @RequestBody InformeImagenRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.informar(principal, id, request));
    }

    @GetMapping("/ordenes/{id}/informes")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO')")
    @Operation(summary = "Historial de versiones del informe")
    public ResponseEntity<List<InformeImagenResponse>> historialInformes(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.historialInformes(principal, id));
    }

    @PutMapping("/ordenes/{id}/cancelar")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO', 'ADMIN')")
    @Operation(summary = "Cancelar una orden", description = "Solo antes de realizar la toma.")
    public ResponseEntity<OrdenImagenResponse> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelarOrdenImagenRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.cancelar(principal, id, request));
    }

    @PostMapping(value = "/ordenes/{id}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(
            summary = "Cargar imágenes",
            description = "JPEG, PNG o PDF de hasta 5 MB, validados por su contenido, hasta 12 por orden.")
    public ResponseEntity<List<ArchivoImagenResponse>> subirArchivos(
            @PathVariable UUID id,
            @RequestParam("archivos") List<MultipartFile> archivos,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        List<ArchivoEntrante> entrantes = new java.util.ArrayList<>();
        for (MultipartFile archivo : archivos) {
            try {
                entrantes.add(new ArchivoEntrante(archivo.getOriginalFilename(), archivo.getBytes()));
            } catch (IOException ex) {
                throw new DatosInvalidosException("No se pudo leer el archivo cargado");
            }
        }
        return ResponseEntity.status(201).body(imagenesService.subirArchivos(principal, id, entrantes));
    }

    @GetMapping("/ordenes/{id}/archivos/{archivoId}")
    @PreAuthorize("hasAnyRole('IMAGENES', 'MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Descargar una imagen",
            description = "Se sirve solo con sesión y con acceso a la orden, sin enlaces públicos.")
    public ResponseEntity<byte[]> descargar(
            @PathVariable UUID id,
            @PathVariable UUID archivoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        ArchivoDescarga archivo = imagenesService.descargarArchivo(principal, id, archivoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(archivo.nombre())
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(archivo.contenido());
    }

    @DeleteMapping("/ordenes/{id}/archivos/{archivoId}")
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(summary = "Quitar una imagen", description = "Solo con la orden programada o realizada.")
    public ResponseEntity<Void> eliminarArchivo(
            @PathVariable UUID id,
            @PathVariable UUID archivoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        imagenesService.eliminarArchivo(principal, id, archivoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('IMAGENES', 'ADMIN', 'RECEPCION')")
    @Operation(
            summary = "Agenda de salas",
            description = "Estudios programados en el rango, sin datos clínicos. El rango máximo es de 31 días.")
    public ResponseEntity<List<AgendaImagenResponse>> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(imagenesService.agenda(desde, hasta));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('IMAGENES', 'ADMIN')")
    @Operation(summary = "Indicadores de la cola")
    public ResponseEntity<ImagenesResumenResponse> resumen() {
        return ResponseEntity.ok(imagenesService.resumen());
    }

    @GetMapping("/mi-firma")
    @PreAuthorize("hasRole('IMAGENES')")
    @Operation(summary = "Firma del radiólogo", description = "Nombre y última colegiatura usada en un informe.")
    public ResponseEntity<MiFirmaImagenResponse> miFirma(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(imagenesService.miFirma(principal));
    }
}
