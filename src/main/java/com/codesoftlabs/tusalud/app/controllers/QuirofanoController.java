package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ChecklistRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.InsumoCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.MotivoCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.NotaOperatoriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ProgramacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.QuirofanoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.RegistroAnestesicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SolicitudCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.UsoSetRequest;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CirugiaDetalle;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CirugiaResumen;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.EventoCirugiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.InsumoDisponibleResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.MiCirugiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.MiembroEquipoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.QuirofanoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.quirofano.CirugiaService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quirofano")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Quirófano",
        description = "Quirófanos, programación quirúrgica, equipo, lista de verificación, nota operatoria y anestesia")
public class QuirofanoController {

    private final CirugiaService cirugias;

    @GetMapping("/quirofanos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'ADMIN')")
    public ResponseEntity<List<QuirofanoResponse>> quirofanos(
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(cirugias.quirofanos(soloActivos));
    }

    @PostMapping("/quirofanos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuirofanoResponse> crearQuirofano(@Valid @RequestBody QuirofanoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cirugias.crearQuirofano(request));
    }

    @PutMapping("/quirofanos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuirofanoResponse> actualizarQuirofano(
            @PathVariable UUID id, @Valid @RequestBody QuirofanoRequest request) {
        return ResponseEntity.ok(cirugias.actualizarQuirofano(id, request));
    }

    @GetMapping("/personal")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<List<MiembroEquipoResponse>> personal(
            @RequestParam RolQuirurgico rol,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.personalDisponible(actor, rol));
    }

    @GetMapping("/insumos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<List<InsumoDisponibleResponse>> insumos(
            @RequestParam(required = false) String q,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.insumosDisponibles(actor, q));
    }

    @GetMapping("/cirugias/agenda")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<List<CirugiaResumen>> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) UUID quirofanoId,
            @RequestParam(required = false) EstadoCirugia estado,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.agenda(actor, desde, hasta, quirofanoId, estado));
    }

    @GetMapping("/cirugias/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<List<MiCirugiaResponse>> mias(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.mias(actor));
    }

    @GetMapping("/cirugias/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> detalle(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.detalle(actor, id));
    }

    @GetMapping("/cirugias/{id}/historial")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<List<EventoCirugiaResponse>> historial(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.historial(actor, id));
    }

    @PostMapping("/cirugias")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> solicitar(
            @Valid @RequestBody SolicitudCirugiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cirugias.solicitar(actor, request));
    }

    @PostMapping("/cirugias/{id}/programar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> programar(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramacionRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.programar(actor, id, request));
    }

    @PostMapping("/cirugias/{id}/consentimientos")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> consentimientos(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.solicitarConsentimientos(actor, id));
    }

    @PostMapping("/cirugias/{id}/preparar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> preparar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.preparar(actor, id));
    }

    @PostMapping("/cirugias/{id}/checklist")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> checklist(
            @PathVariable UUID id,
            @Valid @RequestBody ChecklistRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.registrarChecklist(actor, id, request));
    }

    @PostMapping("/cirugias/{id}/iniciar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> iniciar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.iniciar(actor, id));
    }

    @PostMapping("/cirugias/{id}/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> finalizar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.finalizar(actor, id));
    }

    @PostMapping("/cirugias/{id}/suspender")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> suspender(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCirugiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.suspender(actor, id, request.motivo()));
    }

    @PostMapping("/cirugias/{id}/cancelar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> cancelar(
            @PathVariable UUID id,
            @Valid @RequestBody MotivoCirugiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.cancelar(actor, id, request.motivo()));
    }

    @PutMapping("/cirugias/{id}/nota-operatoria")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> notaOperatoria(
            @PathVariable UUID id,
            @Valid @RequestBody NotaOperatoriaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.notaOperatoria(actor, id, request));
    }

    @PutMapping("/cirugias/{id}/registro-anestesico")
    @PreAuthorize("hasRole('MEDICO')")
    public ResponseEntity<CirugiaDetalle> registroAnestesico(
            @PathVariable UUID id,
            @Valid @RequestBody RegistroAnestesicoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.ok(cirugias.registroAnestesico(actor, id, request));
    }

    @PostMapping("/cirugias/{id}/insumos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> insumo(
            @PathVariable UUID id,
            @Valid @RequestBody InsumoCirugiaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cirugias.registrarInsumo(actor, id, request));
    }

    @PostMapping("/cirugias/{id}/sets")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    public ResponseEntity<CirugiaDetalle> set(
            @PathVariable UUID id,
            @Valid @RequestBody UsoSetRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cirugias.usarSet(actor, id, request));
    }
}
