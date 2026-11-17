package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CitaMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.PacienteMostradorRequest;
import com.codesoftlabs.tusalud.app.dto.request.TurnoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaHoyResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMostradorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteRegistradoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PantallaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecepcionResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoResponse;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AdmisionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecepcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/recepcion")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recepción", description = "Admisión de pacientes, llegada a citas, cola de turnos y pantalla de sala")
public class RecepcionController {

    private final RecepcionService recepcionService;
    private final AdmisionService admisionService;

    @GetMapping("/citas-hoy")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(summary = "Citas del día", description = "Lista las citas de hoy con la marca de llegada de cada una.")
    @ApiResponse(responseCode = "200", description = "Citas del día")
    public ResponseEntity<List<CitaHoyResponse>> citasHoy(
            @Parameter(description = "DNI o nombre del paciente o del médico") @RequestParam(required = false)
                    String q) {
        return ResponseEntity.ok(recepcionService.citasHoy(q));
    }

    @PostMapping("/citas/{id}/llegada")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(
            summary = "Registrar la llegada de un paciente",
            description = "Crea el turno en espera de una cita de hoy y avisa al médico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Turno creado"),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La llegada ya fue registrada o la cita no corresponde a hoy",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TurnoResponse> registrarLlegada(@PathVariable UUID id) {
        TurnoResponse turno = recepcionService.registrarLlegada(id);
        return ResponseEntity.created(ubicacionTurno(turno.id())).body(turno);
    }

    @PostMapping("/turnos")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(summary = "Crear un turno sin cita", description = "Para pacientes que llegan sin cita previa.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Turno creado"),
        @ApiResponse(
                responseCode = "409",
                description = "El paciente ya tiene un turno abierto",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TurnoResponse> crearTurno(@Valid @RequestBody TurnoCreateRequest request) {
        TurnoResponse turno = recepcionService.crearTurno(request);
        return ResponseEntity.created(ubicacionTurno(turno.id())).body(turno);
    }

    @GetMapping("/cola")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN', 'MEDICO')")
    @Operation(
            summary = "Cola de turnos del día",
            description = "Recepción y administración ven todos los turnos y el médico solo los suyos. Por defecto "
                    + "muestra los turnos abiertos y los ordena por prioridad y hora de llegada.")
    @ApiResponse(responseCode = "200", description = "Cola de turnos")
    public ResponseEntity<List<TurnoResponse>> cola(
            @Parameter(description = "Estado del turno") @RequestParam(required = false) EstadoTurno estado,
            @Parameter(description = "Médico, solo para recepción y administración") @RequestParam(required = false)
                    UUID medicoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.cola(estado, medicoId, principal));
    }

    @PutMapping("/turnos/{id}/llamar")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN', 'MEDICO')")
    @Operation(summary = "Llamar un turno", description = "Avisa al paciente que acuda al consultorio.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Turno llamado"),
        @ApiResponse(
                responseCode = "409",
                description = "El turno no está en espera",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TurnoResponse> llamar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.llamar(id, principal));
    }

    @PutMapping("/turnos/{id}/iniciar")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Iniciar la atención de un turno", description = "Solo el médico asignado al turno.")
    @ApiResponse(responseCode = "200", description = "Atención iniciada")
    public ResponseEntity<TurnoResponse> iniciar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.iniciar(id, principal));
    }

    @PutMapping("/turnos/{id}/finalizar")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(summary = "Finalizar la atención de un turno", description = "Solo el médico asignado al turno.")
    @ApiResponse(responseCode = "200", description = "Turno atendido")
    public ResponseEntity<TurnoResponse> finalizar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.finalizar(id, principal));
    }

    @PutMapping("/turnos/{id}/ausente")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN', 'MEDICO')")
    @Operation(summary = "Marcar a un paciente como ausente", description = "Para turnos en espera o llamados.")
    @ApiResponse(responseCode = "200", description = "Turno marcado como ausente")
    public ResponseEntity<TurnoResponse> ausente(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.marcarAusente(id, principal));
    }

    @PutMapping("/turnos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN', 'MEDICO')")
    @Operation(summary = "Cancelar un turno", description = "Para turnos en espera o llamados.")
    @ApiResponse(responseCode = "200", description = "Turno cancelado")
    public ResponseEntity<TurnoResponse> cancelar(
            @PathVariable UUID id, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(recepcionService.cancelar(id, principal));
    }

    @GetMapping("/pantalla")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Pantalla de la sala de espera",
            description = "Últimos turnos llamados o en atención para mostrar en la sala.")
    @ApiResponse(responseCode = "200", description = "Turnos de la pantalla")
    public ResponseEntity<List<PantallaTurnoResponse>> pantalla() {
        return ResponseEntity.ok(recepcionService.pantalla());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(summary = "Resumen de la recepción", description = "Indicadores del día.")
    @ApiResponse(responseCode = "200", description = "Indicadores")
    public ResponseEntity<RecepcionResumenResponse> resumen() {
        return ResponseEntity.ok(recepcionService.resumen());
    }

    @GetMapping("/pacientes")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN', 'CAJA', 'MEDICO', 'FARMACIA', 'ENFERMERIA')")
    @Operation(summary = "Buscar pacientes", description = "Busca por DNI o nombre y devuelve hasta 30 resultados.")
    @ApiResponse(responseCode = "200", description = "Pacientes encontrados")
    public ResponseEntity<List<PacienteMostradorResponse>> buscarPacientes(
            @Parameter(description = "DNI o parte del nombre") @RequestParam(required = false) String q) {
        return ResponseEntity.ok(admisionService.buscarPacientes(q));
    }

    @PostMapping("/pacientes")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(
            summary = "Registrar un paciente en el mostrador",
            description = "Crea la cuenta y la historia clínica y devuelve una contraseña temporal solo esta vez.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Paciente registrado"),
        @ApiResponse(
                responseCode = "409",
                description = "El DNI o el email ya existen",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PacienteRegistradoResponse> registrarPaciente(
            @Valid @RequestBody PacienteMostradorRequest request) {
        PacienteRegistradoResponse creado = admisionService.registrarPaciente(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/v1/recepcion/pacientes")
                .queryParam("q", creado.paciente().dni())
                .build()
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PostMapping("/citas")
    @PreAuthorize("hasAnyRole('RECEPCION', 'ADMIN')")
    @Operation(
            summary = "Reservar una cita en el mostrador",
            description = "Aplica las mismas reglas de horario, solapes y bloqueos que la reserva del paciente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cita registrada"),
        @ApiResponse(
                responseCode = "409",
                description = "Conflicto de horario",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CitaResponse> reservarCita(@Valid @RequestBody CitaMostradorRequest request) {
        CitaResponse cita = admisionService.reservarCita(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/citas/{id}")
                .buildAndExpand(cita.id())
                .toUri();
        return ResponseEntity.created(location).body(cita);
    }

    private static URI ubicacionTurno(UUID id) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/recepcion/cola")
                .queryParam("turnoId", id)
                .build()
                .toUri();
    }
}
