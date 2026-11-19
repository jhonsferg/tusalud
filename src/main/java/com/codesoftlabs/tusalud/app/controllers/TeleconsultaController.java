package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.ConsentimientoTelemedicinaRequest;
import com.codesoftlabs.tusalud.app.dto.response.ConsentimientoTelemedicinaResponse;
import com.codesoftlabs.tusalud.app.dto.response.IceServersResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenPacienteTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TeleconsultaResponse;
import com.codesoftlabs.tusalud.app.dto.response.TicketTeleconsultaResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teleconsultas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Telemedicina", description = "Teleconsultas por video entre el médico y el paciente de una cita virtual")
public class TeleconsultaController {

    private final TeleconsultaService teleconsultaService;

    @GetMapping("/mias")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Mis teleconsultas",
            description = "Teleconsultas del paciente autenticado, las más recientes primero.")
    public List<TeleconsultaResponse> mias(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.listarMias(actor);
    }

    @GetMapping("/medico")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Teleconsultas del médico", description = "Desde la fecha indicada, por defecto hoy.")
    public List<TeleconsultaResponse> delMedico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.listarDelMedico(actor, desde);
    }

    @GetMapping("/consentimiento")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Consentimiento de telemedicina vigente")
    public ConsentimientoTelemedicinaResponse consentimiento(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.consentimiento(actor);
    }

    @PostMapping("/consentimiento")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Aceptar el consentimiento de telemedicina")
    public ConsentimientoTelemedicinaResponse aceptar(
            @Valid @RequestBody ConsentimientoTelemedicinaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.aceptarConsentimiento(actor, request.version());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Detalle de una teleconsulta", description = "Solo para el médico y el paciente de la cita.")
    public TeleconsultaResponse obtener(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.obtener(id, actor);
    }

    @PostMapping("/{id}/abrir")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Abrir la sala", description = "El médico habilita la sala dentro de la ventana de ingreso.")
    public TeleconsultaResponse abrir(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.abrir(id, actor);
    }

    @PostMapping("/{id}/ticket")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Ticket de conexión",
            description = "Ticket de un solo uso y 30 segundos de vida para abrir el canal de señalización.")
    public ResponseEntity<TicketTeleconsultaResponse> ticket(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teleconsultaService.emitirTicket(id, actor));
    }

    @GetMapping("/{id}/ice")
    @PreAuthorize("hasAnyRole('MEDICO', 'PACIENTE')")
    @Operation(summary = "Servidores ICE con credenciales temporales")
    public IceServersResponse ice(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.ice(id, actor);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Finalizar la teleconsulta para ambos")
    public TeleconsultaResponse finalizar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.finalizar(id, actor);
    }

    @GetMapping("/{id}/resumen-paciente")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Resumen mínimo del paciente",
            description = "Datos mínimos para la consulta virtual; el acceso queda auditado.")
    public ResumenPacienteTeleconsultaResponse resumen(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal actor) {
        return teleconsultaService.resumenPaciente(id, actor);
    }
}
