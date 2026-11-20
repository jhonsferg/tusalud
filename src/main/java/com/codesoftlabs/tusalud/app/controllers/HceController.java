package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.HceRequests.AdendaRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.CorreccionRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.FusionPacientesRequest;
import com.codesoftlabs.tusalud.app.dto.request.HceRequests.PoliticaConservacionRequest;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.CierreResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.DuplicadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.FusionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.HistorialAtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.PoliticaConservacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.HceResponses.VersionResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.hce.HceConservacionService;
import com.codesoftlabs.tusalud.app.services.hce.HceFusionService;
import com.codesoftlabs.tusalud.app.services.hce.HceImpresionService;
import com.codesoftlabs.tusalud.app.services.hce.HceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hce")
@RequiredArgsConstructor
@Validated
@Tag(name = "Historia clínica electrónica", description = "Cierre, rectificación con rastro y conservación")
public class HceController {

    private final HceService hce;
    private final HceConservacionService conservacion;
    private final HceFusionService fusion;
    private final HceImpresionService impresion;

    @PostMapping("/atenciones/{id}/cierre")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Cerrar y firmar la atención")
    public ResponseEntity<CierreResponse> cerrar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hce.cerrar(actor, id));
    }

    @GetMapping("/atenciones/{id}/cierre")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Estado de cierre de la atención")
    public CierreResponse estado(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return hce.estado(actor, id);
    }

    @PostMapping("/atenciones/{id}/adendas")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Agregar una adenda a una atención cerrada")
    public ResponseEntity<VersionResponse> adenda(
            @PathVariable UUID id,
            @Valid @RequestBody AdendaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hce.adenda(actor, id, request));
    }

    @PostMapping("/atenciones/{id}/correcciones")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Corregir un campo de una atención cerrada conservando el valor previo")
    public ResponseEntity<VersionResponse> corregir(
            @PathVariable UUID id,
            @Valid @RequestBody CorreccionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hce.corregir(actor, id, request));
    }

    @GetMapping("/atenciones/{id}/versiones")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Línea de tiempo y versiones de una atención")
    public HistorialAtencionResponse historial(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return hce.historial(actor, id);
    }

    @GetMapping("/mi-historia/impresion")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mi historia clínica completa en PDF")
    public ResponseEntity<byte[]> imprimirPropia(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historia-clinica.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(impresion.imprimirPropia(actor));
    }

    @GetMapping("/pacientes/{id}/impresion")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Historia clínica completa en PDF con código de verificación")
    public ResponseEntity<byte[]> imprimir(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        byte[] pdf = impresion.imprimir(actor, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historia-clinica.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/conservacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Políticas de conservación por tipo de registro")
    public List<PoliticaConservacionResponse> politicas() {
        return conservacion.listar();
    }

    @PutMapping("/conservacion/{tipo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajustar el plazo referencial de conservación, la depuración sigue bloqueada")
    public PoliticaConservacionResponse actualizarPolitica(
            @PathVariable String tipo,
            @Valid @RequestBody PoliticaConservacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return conservacion.actualizar(actor, tipo, request);
    }

    @GetMapping("/pacientes/duplicados")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pacientes con posible duplicidad")
    public List<DuplicadoResponse> duplicados() {
        return fusion.duplicados();
    }

    @GetMapping("/pacientes/fusiones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fusiones de historias realizadas")
    public List<FusionResponse> fusiones() {
        return fusion.historial();
    }

    @PostMapping("/pacientes/fusion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fusionar dos historias, con motivo y auditoría")
    public ResponseEntity<FusionResponse> fusionar(
            @Valid @RequestBody FusionPacientesRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fusion.fusionar(actor, request));
    }
}
