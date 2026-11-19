package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.MotivoConsentimientoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.SolicitudArcoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.AccesoDatosResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.ConsentimientoEstadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EstadoAceptacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.HistorialConsentimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaPublicaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PreviaCancelacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.privacidad.AccesosDatosService;
import com.codesoftlabs.tusalud.app.services.privacidad.ConsentimientosPrivacidadService;
import com.codesoftlabs.tusalud.app.services.privacidad.PoliticaPrivacidadService;
import com.codesoftlabs.tusalud.app.services.privacidad.SolicitudArcoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/privacidad")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Privacidad del titular",
        description = "Política de privacidad, consentimientos de tratamiento, derechos ARCO y accesos a los datos")
public class PrivacidadController {

    private final PoliticaPrivacidadService politicas;
    private final ConsentimientosPrivacidadService consentimientos;
    private final SolicitudArcoService solicitudes;
    private final AccesosDatosService accesos;

    @GetMapping("/politica")
    @Operation(summary = "Política de privacidad vigente", description = "Es pública y no requiere sesión.")
    public ResponseEntity<PoliticaPublicaResponse> politica() {
        return ResponseEntity.ok(politicas
                .vigentePublica()
                .orElseThrow(() -> new ResourceNotFoundException("Política de privacidad", "estado", "vigente")));
    }

    @GetMapping("/politica/estado")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Si el usuario aceptó la política vigente")
    public ResponseEntity<EstadoAceptacionResponse> estadoPolitica(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(politicas.estado(principal));
    }

    @PostMapping("/politica/aceptar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registrar la aceptación de la política vigente")
    public ResponseEntity<EstadoAceptacionResponse> aceptar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(politicas.aceptar(principal, OrigenPrivacidad.APP));
    }

    @GetMapping("/consentimientos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Consentimientos de tratamiento por finalidad")
    public ResponseEntity<List<ConsentimientoEstadoResponse>> consentimientos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(consentimientos.mios(principal.getId()));
    }

    @GetMapping("/consentimientos/historial")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Historial de consentimientos otorgados y revocados")
    public ResponseEntity<List<HistorialConsentimientoResponse>> historial(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(consentimientos.historial(principal.getId()));
    }

    @PostMapping("/consentimientos/{finalidadId}/otorgar")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Otorgar el consentimiento de una finalidad opcional")
    public ResponseEntity<ConsentimientoEstadoResponse> otorgar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID finalidadId,
            @Valid @RequestBody(required = false) MotivoConsentimientoRequest request) {
        return ResponseEntity.ok(
                consentimientos.otorgar(principal, finalidadId, request == null ? null : request.motivo()));
    }

    @PostMapping("/consentimientos/{finalidadId}/revocar")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Revocar el consentimiento de una finalidad opcional")
    public ResponseEntity<ConsentimientoEstadoResponse> revocar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable UUID finalidadId,
            @Valid @RequestBody(required = false) MotivoConsentimientoRequest request) {
        return ResponseEntity.ok(
                consentimientos.revocar(principal, finalidadId, request == null ? null : request.motivo()));
    }

    @GetMapping("/accesos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Quién accedió a mis datos",
            description = "Los accesos registrados a su historia clínica y a sus documentos.")
    public ResponseEntity<List<AccesoDatosResponse>> accesos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(accesos.misAccesos(principal));
    }

    @GetMapping("/cancelacion/previa")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Qué se puede cancelar y qué se conserva",
            description =
                    "Muestra por categoría si los datos se pueden cancelar o están bloqueados por una obligación legal.")
    public ResponseEntity<PreviaCancelacionResponse> previaCancelacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudes.previaCancelacion(principal.getId()));
    }

    @PostMapping("/solicitudes")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Registrar una solicitud de acceso, rectificación, cancelación u oposición")
    public ResponseEntity<SolicitudArcoResponse> solicitar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody SolicitudArcoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudes.crear(principal, request));
    }

    @GetMapping("/solicitudes/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mis solicitudes de privacidad")
    public ResponseEntity<List<SolicitudArcoResponse>> mias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(solicitudes.mias(principal));
    }

    @GetMapping("/solicitudes/{id}")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN', 'CALIDAD')")
    @Operation(summary = "Detalle de una solicitud con su línea de tiempo")
    public ResponseEntity<SolicitudArcoDetalle> detalle(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        return ResponseEntity.ok(solicitudes.detalle(principal, id));
    }

    @GetMapping("/solicitudes/{id}/exportacion")
    @PreAuthorize("hasAnyRole('PACIENTE', 'ADMIN', 'CALIDAD')")
    @Operation(summary = "Descargar la exportación de datos de una solicitud de acceso")
    public ResponseEntity<byte[]> exportacion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal, @PathVariable UUID id) {
        byte[] contenido = solicitudes.exportar(principal, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("mis-datos-" + id + ".json")
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }
}
