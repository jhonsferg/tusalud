package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.AlertasRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.AnularRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.DispensarRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.EmitirRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertasResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.VerificacionFirmaResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.receta.RecetaElectronicaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recetas")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Recetas electrónicas",
        description =
                "Receta con numeración, firma electrónica del servidor, alertas al prescribir y dispensación parcial. La firma no equivale a una firma digital acreditada ante INDECOPI")
public class RecetasElectronicasController {

    private final RecetaElectronicaService servicio;

    @PostMapping("/alertas")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<AlertasResponse> alertas(
            @Valid @RequestBody AlertasRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.evaluarAlertas(actor, request));
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<RecetaResponse> emitir(
            @Valid @RequestBody EmitirRecetaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.emitir(actor, request));
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<RecetaResumenResponse>> mias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.misRecetas(actor));
    }

    @GetMapping("/emitidas")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<RecetaResumenResponse>> emitidas(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.emitidasPor(actor));
    }

    @GetMapping("/paciente/{pacienteId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<RecetaResumenResponse>> delPaciente(
            @PathVariable UUID pacienteId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.delPaciente(actor, pacienteId));
    }

    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<List<RecetaResumenResponse>> deLaAtencion(
            @PathVariable UUID atencionId, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.deLaAtencion(actor, atencionId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<RecetaResponse> obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.obtener(actor, id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE', 'FARMACIA')")
    public ResponseEntity<byte[]> pdf(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        byte[] contenido = servicio.generarPdf(actor, id);
        ContentDisposition disposicion = ContentDisposition.builder("inline")
                .filename("receta-electronica.pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(contenido.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.noStore())
                .body(contenido);
    }

    @GetMapping("/{id}/firma")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    public ResponseEntity<VerificacionFirmaResponse> firma(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.verificarFirma(actor, id));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<RecetaResponse> anular(
            @PathVariable UUID id,
            @Valid @RequestBody AnularRecetaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(servicio.anular(actor, id, request.motivo()));
    }

    @GetMapping("/farmacia")
    @PreAuthorize("hasRole('FARMACIA')")
    public ResponseEntity<List<RecetaFarmaciaResponse>> paraDispensar(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(servicio.paraDispensar(q));
    }

    @GetMapping("/farmacia/{id}")
    @PreAuthorize("hasRole('FARMACIA')")
    public ResponseEntity<RecetaFarmaciaResponse> farmaciaDetalle(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.farmaciaDetalle(id));
    }

    @PostMapping("/farmacia/{id}/dispensar")
    @PreAuthorize("hasRole('FARMACIA')")
    public ResponseEntity<DispensacionResponse> dispensar(
            @PathVariable UUID id,
            @Valid @RequestBody DispensarRecetaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.dispensar(actor, id, request));
    }
}
