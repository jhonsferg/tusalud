package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.HorarioRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.MedicoEspecialidadesRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadRangoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.HorarioResponse;
import com.codesoftlabs.tusalud.app.dto.response.MedicoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.MedicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/medicos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Médicos", description = "Consulta de médicos, gestión de horarios de atención, disponibilidad y agenda")
public class MedicoController {

    private final MedicoService medicoService;
    private final CitaService citaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar médicos",
            description =
                    "Devuelve todos los médicos o, si se indica especialidadId, solo los médicos activos de esa especialidad.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de médicos"),
        @ApiResponse(
                responseCode = "400",
                description = "Parámetro inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<MedicoResponse>> listar(
            @Parameter(description = "Filtrar por ID de especialidad (opcional)", example = "1")
                    @RequestParam(required = false)
                    UUID especialidadId) {
        return ResponseEntity.ok(medicoService.listar(especialidadId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener médico", description = "Devuelve el detalle de un médico por su identificador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Médico encontrado"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MedicoResponse> obtener(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id) {
        return ResponseEntity.ok(medicoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Registrar médico",
            description =
                    "Da de alta un médico creando su usuario con rol MEDICO y asignándole una especialidad. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Médico registrado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Especialidad no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El email o la matrícula ya se encuentran registrados",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MedicoResponse> crear(@Valid @RequestBody MedicoCreateRequest request) {
        MedicoResponse creado = medicoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}/especialidades")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Actualizar las especialidades de un médico",
            description = "Reemplaza las especialidades que atiende el médico. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialidades actualizadas"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico o especialidad no encontrados",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MedicoResponse> actualizarEspecialidades(
            @Parameter(description = "Identificador del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                    @PathVariable
                    UUID id,
            @Valid @RequestBody MedicoEspecialidadesRequest request) {
        return ResponseEntity.ok(medicoService.actualizarEspecialidades(id, request));
    }

    @GetMapping("/{id}/horarios")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Listar horarios de un médico",
            description = "Devuelve los bloques de atención semanales configurados para el médico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de horarios"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<HorarioResponse>> listarHorarios(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id) {
        return ResponseEntity.ok(medicoService.listarHorarios(id));
    }

    @PostMapping("/{id}/horarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Agregar horario de atención",
            description =
                    "Agrega un bloque de atención al médico. La hora de fin debe ser posterior a la de inicio y el bloque no puede cruzarse con otro existente. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Horario agregado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos de entrada inválidos o rango horario incorrecto",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El bloque se cruza con otro horario del médico",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<HorarioResponse> agregarHorario(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Valid @RequestBody HorarioRequest request) {
        HorarioResponse creado = medicoService.agregarHorario(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{horarioId}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @DeleteMapping("/{id}/horarios/{horarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Eliminar horario de atención",
            description = "Elimina un bloque de atención del médico. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Horario eliminado"),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico u horario no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminarHorario(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Parameter(description = "ID del horario", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID horarioId) {
        medicoService.eliminarHorario(id, horarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/disponibilidad")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Consultar disponibilidad",
            description =
                    "Devuelve las franjas libres del médico en la fecha indicada según sus horarios de atención y las citas no canceladas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disponibilidad del médico"),
        @ApiResponse(
                responseCode = "400",
                description = "Fecha ausente o con formato inválido (yyyy-MM-dd)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DisponibilidadResponse> consultarDisponibilidad(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Parameter(description = "Fecha a consultar (yyyy-MM-dd)", example = "2026-10-20", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fecha) {
        return ResponseEntity.ok(citaService.consultarDisponibilidad(id, fecha));
    }

    @GetMapping("/{id}/disponibilidad/rango")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Consultar disponibilidad en un rango de fechas",
            description =
                    "Devuelve, para cada día con atención dentro del rango (máximo 62 días), las horas libres del médico. Omite los días sin horario y los días pasados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disponibilidad por día"),
        @ApiResponse(
                responseCode = "400",
                description = "Rango inválido, mayor a 62 días o con formato incorrecto",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Médico no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<DisponibilidadRangoResponse>> consultarDisponibilidadRango(
            @Parameter(description = "ID del médico", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Parameter(description = "Primer día (yyyy-MM-dd)", example = "2026-10-01", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Último día (yyyy-MM-dd)", example = "2026-10-31", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin,
            @Parameter(description = "Reservado; se ignora") @RequestParam(required = false) UUID especialidadId) {
        return ResponseEntity.ok(citaService.consultarDisponibilidadRango(id, inicio, fin));
    }

    @GetMapping("/me/agenda")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Agenda del médico autenticado",
            description =
                    "Devuelve las citas del médico autenticado para la fecha indicada (por defecto, hoy). Requiere rol MEDICO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agenda del día"),
        @ApiResponse(
                responseCode = "400",
                description = "Fecha con formato inválido (yyyy-MM-dd)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "No autenticado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol MEDICO",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CitaResponse>> agenda(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Fecha de la agenda (yyyy-MM-dd); por defecto hoy", example = "2026-10-20")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fecha,
            @Parameter(description = "Inicio del rango (yyyy-MM-dd); requiere fin", example = "2026-10-01")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Fin del rango (yyyy-MM-dd); máximo 62 días", example = "2026-10-31")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin) {
        return ResponseEntity.ok(citaService.agendaMedico(principal, fecha, inicio, fin));
    }
}
