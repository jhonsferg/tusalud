package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.ConfiguracionFiscalRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.CorreccionClienteRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.MotivoBajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.NotaRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.ResumenDiarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.SerieEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.SerieRequest;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ConfiguracionFiscalResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.DocumentoElectronicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.DocumentoElectronicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.EstadoFacturacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.MotivoNotaResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.RegistroVentasResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ResumenSunatResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.SerieResponse;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.facturacion.ColaFacturacion;
import com.codesoftlabs.tusalud.app.services.facturacion.ConfiguracionFiscalService;
import com.codesoftlabs.tusalud.app.services.facturacion.FacturacionDocumentos;
import com.codesoftlabs.tusalud.app.services.facturacion.FacturacionDocumentos.Archivo;
import com.codesoftlabs.tusalud.app.services.facturacion.FacturacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/facturacion")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Facturación electrónica",
        description =
                "Comprobantes electrónicos para SUNAT: series, boletas, facturas, notas, resúmenes, bajas y registro de ventas")
public class FacturacionController {

    private static final String LECTURA = "hasAnyRole('ADMIN', 'FINANZAS', 'CAJA', 'GERENCIA')";
    private static final String GESTION = "hasAnyRole('ADMIN', 'FINANZAS')";

    private final FacturacionService servicio;
    private final FacturacionDocumentos documentos;
    private final ConfiguracionFiscalService configuracion;
    private final ColaFacturacion cola;

    @GetMapping("/estado")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Estado de la facturación electrónica y conteo de comprobantes por estado")
    public ResponseEntity<EstadoFacturacionResponse> estado() {
        return ResponseEntity.ok(servicio.estado());
    }

    @GetMapping("/configuracion")
    @PreAuthorize(GESTION)
    @Operation(summary = "Configuración fiscal del emisor")
    public ResponseEntity<ConfiguracionFiscalResponse> configuracion() {
        return ResponseEntity.ok(configuracion
                .obtener()
                .orElseThrow(
                        () -> new ResourceNotFoundException("Configuración fiscal", "sede", configuracion.sede())));
    }

    @PutMapping("/configuracion")
    @PreAuthorize(GESTION)
    @Operation(
            summary = "Guardar la configuración fiscal",
            description =
                    "Los datos del emisor, el proveedor y la afectación. Las credenciales SOL y el certificado digital se configuran como secretos del servidor.")
    public ResponseEntity<ConfiguracionFiscalResponse> guardarConfiguracion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConfiguracionFiscalRequest request) {
        return ResponseEntity.ok(configuracion.actualizar(principal, request));
    }

    @GetMapping("/series")
    @PreAuthorize(GESTION)
    @Operation(summary = "Series y correlativos")
    public ResponseEntity<List<SerieResponse>> series() {
        return ResponseEntity.ok(configuracion.listarSeries());
    }

    @PostMapping("/series")
    @PreAuthorize(GESTION)
    @Operation(summary = "Crear una serie")
    public ResponseEntity<SerieResponse> crearSerie(@Valid @RequestBody SerieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(configuracion.crearSerie(request));
    }

    @PatchMapping("/series/{id}")
    @PreAuthorize(GESTION)
    @Operation(summary = "Activar o desactivar una serie")
    public ResponseEntity<SerieResponse> estadoSerie(
            @PathVariable UUID id, @Valid @RequestBody SerieEstadoRequest request) {
        return ResponseEntity.ok(configuracion.cambiarEstado(id, request.activa()));
    }

    @GetMapping("/motivos")
    @PreAuthorize(GESTION)
    @Operation(summary = "Motivos de notas de crédito y de débito")
    public ResponseEntity<List<MotivoNotaResponse>> motivos() {
        return ResponseEntity.ok(servicio.motivos());
    }

    @GetMapping("/documentos")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Comprobantes electrónicos")
    public ResponseEntity<List<DocumentoElectronicoResumen>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) TipoCpe tipo,
            @RequestParam(required = false) EstadoCpe estado,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicio.listar(desde, hasta, tipo, estado, q));
    }

    @GetMapping("/documentos/{id}")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Detalle de un comprobante electrónico con su bitácora de envíos")
    public ResponseEntity<DocumentoElectronicoDetalle> detalle(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.detalle(id));
    }

    @GetMapping("/documentos/por-comprobante/{comprobanteId}")
    @PreAuthorize(LECTURA)
    @Operation(summary = "Comprobante electrónico de un cobro de caja")
    public ResponseEntity<DocumentoElectronicoDetalle> porComprobante(@PathVariable UUID comprobanteId) {
        return ResponseEntity.ok(servicio.deComprobante(comprobanteId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Comprobante electrónico", "comprobante", comprobanteId)));
    }

    @GetMapping("/documentos/{id}/xml")
    @PreAuthorize(GESTION)
    @Operation(summary = "XML firmado del comprobante")
    public ResponseEntity<byte[]> xml(@PathVariable UUID id) {
        return adjunto(servicio.xml(id), "comprobante-" + id + ".xml", MediaType.APPLICATION_XML);
    }

    @GetMapping("/documentos/{id}/cdr")
    @PreAuthorize(GESTION)
    @Operation(summary = "Constancia de recepción de SUNAT")
    public ResponseEntity<byte[]> cdr(@PathVariable UUID id) {
        return adjunto(servicio.cdr(id), "cdr-" + id + ".zip", MediaType.parseMediaType("application/zip"));
    }

    @GetMapping("/documentos/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS', 'CAJA', 'GERENCIA', 'PACIENTE')")
    @Operation(summary = "Representación impresa del comprobante con código QR")
    public ResponseEntity<byte[]> pdf(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        Archivo archivo = documentos.pdf(principal, id);
        return adjunto(archivo.contenido(), archivo.nombre(), MediaType.APPLICATION_PDF);
    }

    @PostMapping("/documentos/{id}/reenviar")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS', 'CAJA')")
    @Operation(summary = "Reenviar un comprobante con error o rechazado")
    public ResponseEntity<DocumentoElectronicoDetalle> reenviar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(servicio.reenviar(principal, id));
    }

    @PutMapping("/documentos/{id}/cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS', 'CAJA')")
    @Operation(
            summary = "Corregir los datos del cliente de un comprobante no aceptado",
            description =
                    "Regenera el XML, lo firma y lo vuelve a enviar. Un comprobante aceptado se corrige con una nota.")
    public ResponseEntity<DocumentoElectronicoDetalle> corregirCliente(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CorreccionClienteRequest request) {
        return ResponseEntity.ok(servicio.corregirCliente(principal, id, request));
    }

    @PostMapping("/documentos/{id}/baja")
    @PreAuthorize(GESTION)
    @Operation(
            summary = "Comunicar la baja de un comprobante aceptado",
            description =
                    "Genera una comunicación de baja, o un resumen diario para boletas, dentro del plazo configurado.")
    public ResponseEntity<DocumentoElectronicoDetalle> baja(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody MotivoBajaRequest request) {
        return ResponseEntity.ok(servicio.solicitarBaja(principal, id, request.motivo()));
    }

    @PostMapping("/notas")
    @PreAuthorize(GESTION)
    @Operation(summary = "Emitir una nota de crédito o de débito sobre un comprobante aceptado")
    public ResponseEntity<DocumentoElectronicoDetalle> nota(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody NotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearNota(principal, request));
    }

    @GetMapping("/resumenes")
    @PreAuthorize(GESTION)
    @Operation(summary = "Resúmenes diarios y comunicaciones de baja")
    public ResponseEntity<List<ResumenSunatResponse>> resumenes() {
        return ResponseEntity.ok(servicio.listarResumenes());
    }

    @PostMapping("/resumenes")
    @PreAuthorize(GESTION)
    @Operation(summary = "Generar el resumen diario de boletas de una fecha")
    public ResponseEntity<ResumenSunatResponse> generarResumen(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ResumenDiarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.generarResumenDiario(principal, request.fecha()));
    }

    @PostMapping("/cola/procesar")
    @PreAuthorize(GESTION)
    @Operation(summary = "Procesar de inmediato los envíos y resúmenes pendientes")
    public ResponseEntity<Map<String, Integer>> procesarCola() {
        return ResponseEntity.ok(Map.of("procesados", cola.procesarAhora()));
    }

    @GetMapping("/registro-ventas")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS', 'GERENCIA')")
    @Operation(summary = "Registro de ventas del periodo")
    public ResponseEntity<RegistroVentasResponse> registro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(documentos.registroVentas(desde, hasta));
    }

    @GetMapping("/registro-ventas/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANZAS', 'GERENCIA')")
    @Operation(summary = "Registro de ventas en Excel")
    public ResponseEntity<byte[]> registroExcel(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Archivo archivo = documentos.excelRegistroVentas(principal, desde, hasta);
        return adjunto(
                archivo.contenido(),
                archivo.nombre(),
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/mis-comprobantes")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Comprobantes electrónicos del paciente autenticado")
    public ResponseEntity<List<DocumentoElectronicoResumen>> misComprobantes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(servicio.misComprobantes(principal));
    }

    private static ResponseEntity<byte[]> adjunto(byte[] contenido, String nombre, MediaType tipo) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(contenido);
    }
}
