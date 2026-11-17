package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.AltaHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaAsignacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EmergenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EvolucionHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoHospCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CamaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.EvolucionHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.HospResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.MapaCamasResponse;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HospitalizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/v1/hospitalizacion")
@RequiredArgsConstructor
@Validated
@Tag(name = "Hospitalización", description = "Camas, ingresos, evoluciones y emergencias con triaje")
public class HospitalizacionController {

    private static final String ERROR_409 = "Conflicto con el estado actual";

    private final HospitalizacionService hospitalizacionService;

    @GetMapping("/camas")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION')")
    @Operation(summary = "Listar camas", description = "Filtra por área y estado e informa el ocupante de cada cama.")
    @ApiResponse(responseCode = "200", description = "Listado de camas")
    public ResponseEntity<List<CamaResponse>> listarCamas(
            @Parameter(description = "Área de la cama") @RequestParam(required = false) AreaCama area,
            @Parameter(description = "Estado de la cama") @RequestParam(required = false) EstadoCama estado) {
        return ResponseEntity.ok(hospitalizacionService.listarCamas(area, estado));
    }

    @PostMapping("/camas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una cama", description = "Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cama creada"),
        @ApiResponse(
                responseCode = "409",
                description = "El código ya existe",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CamaResponse> crearCama(@Valid @RequestBody CamaRequest request) {
        CamaResponse creada = hospitalizacionService.crearCama(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/camas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una cama", description = "Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cama actualizada"),
        @ApiResponse(
                responseCode = "404",
                description = "Cama no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CamaResponse> actualizarCama(
            @PathVariable @Positive Long id, @Valid @RequestBody CamaRequest request) {
        return ResponseEntity.ok(hospitalizacionService.actualizarCama(id, request));
    }

    @PutMapping("/camas/{id}/estado")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'ADMIN')")
    @Operation(
            summary = "Cambiar el estado de una cama",
            description = "Una cama ocupada no cambia de estado: se libera con el alta o el traslado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(
                responseCode = "409",
                description = ERROR_409,
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CamaResponse> cambiarEstadoCama(
            @PathVariable @Positive Long id, @Valid @RequestBody CamaEstadoRequest request) {
        return ResponseEntity.ok(hospitalizacionService.cambiarEstadoCama(id, request));
    }

    @PostMapping("/ingresos")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(
            summary = "Registrar un ingreso hospitalario",
            description = "Ocupa una cama libre. Un paciente no puede tener dos ingresos activos.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ingreso registrado"),
        @ApiResponse(
                responseCode = "409",
                description = ERROR_409,
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IngresoHospResponse> crearIngreso(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody IngresoHospCreateRequest request) {
        IngresoHospResponse creado = hospitalizacionService.crearIngreso(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @GetMapping("/ingresos")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION')")
    @Operation(summary = "Listar ingresos", description = "Filtra por estado y busca por código, DNI o paciente.")
    @ApiResponse(responseCode = "200", description = "Listado de ingresos")
    public ResponseEntity<List<IngresoHospResponse>> listarIngresos(
            @Parameter(description = "Estado del ingreso") @RequestParam(required = false) EstadoIngresoHosp estado,
            @Parameter(description = "Texto en el código, DNI o nombre") @RequestParam(required = false) String q) {
        return ResponseEntity.ok(hospitalizacionService.listarIngresos(estado, q));
    }

    @GetMapping("/ingresos/{id}")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION', 'PACIENTE')")
    @Operation(summary = "Obtener un ingreso", description = "El paciente solo consulta sus propios ingresos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ingreso encontrado"),
        @ApiResponse(
                responseCode = "403",
                description = "Ingreso ajeno",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Ingreso no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<IngresoHospResponse> obtenerIngreso(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(hospitalizacionService.obtenerIngreso(principal, id));
    }

    @PutMapping("/ingresos/{id}/trasladar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA', 'ADMIN')")
    @Operation(summary = "Trasladar a otra cama", description = "Libera la cama anterior a limpieza.")
    @ApiResponse(responseCode = "200", description = "Traslado realizado")
    public ResponseEntity<IngresoHospResponse> trasladar(
            @PathVariable @Positive Long id, @Valid @RequestBody CamaAsignacionRequest request) {
        return ResponseEntity.ok(hospitalizacionService.trasladar(id, request));
    }

    @PutMapping("/ingresos/{id}/asignar-cama")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    @Operation(
            summary = "Asignar cama a una emergencia",
            description = "Pasa una emergencia a una cama de emergencia, UCI u observación.")
    @ApiResponse(responseCode = "200", description = "Cama asignada")
    public ResponseEntity<IngresoHospResponse> asignarCama(
            @PathVariable @Positive Long id, @Valid @RequestBody CamaAsignacionRequest request) {
        return ResponseEntity.ok(hospitalizacionService.asignarCama(id, request));
    }

    @PutMapping("/ingresos/{id}/alta")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(summary = "Dar de alta", description = "Cierra el ingreso, liquida la cama y la deja en limpieza.")
    @ApiResponse(responseCode = "200", description = "Alta registrada")
    public ResponseEntity<IngresoHospResponse> darAlta(
            @PathVariable @Positive Long id, @Valid @RequestBody AltaHospRequest request) {
        return ResponseEntity.ok(hospitalizacionService.darAlta(id, request));
    }

    @PutMapping("/ingresos/{id}/cancelar")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    @Operation(summary = "Cancelar un ingreso", description = "Solo mientras no tenga evoluciones.")
    @ApiResponse(responseCode = "200", description = "Ingreso cancelado")
    public ResponseEntity<IngresoHospResponse> cancelarIngreso(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(hospitalizacionService.cancelarIngreso(id));
    }

    @PostMapping("/ingresos/{id}/evoluciones")
    @PreAuthorize("hasAnyRole('MEDICO', 'ENFERMERIA')")
    @Operation(
            summary = "Registrar una evolución",
            description =
                    "Las evoluciones médicas las escribe un médico y las de enfermería el personal de enfermería.")
    @ApiResponse(responseCode = "201", description = "Evolución registrada")
    public ResponseEntity<EvolucionHospResponse> registrarEvolucion(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody EvolucionHospRequest request) {
        EvolucionHospResponse creada = hospitalizacionService.registrarEvolucion(principal, id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/ingresos/{id}/evoluciones")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION', 'PACIENTE')")
    @Operation(
            summary = "Listar las evoluciones de un ingreso",
            description = "El paciente dueño solo ve las evoluciones médicas resumidas.")
    @ApiResponse(responseCode = "200", description = "Evoluciones del ingreso")
    public ResponseEntity<List<EvolucionHospResponse>> listarEvoluciones(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(hospitalizacionService.listarEvoluciones(principal, id));
    }

    @PostMapping("/emergencias")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'RECEPCION', 'ADMIN')")
    @Operation(
            summary = "Registrar una emergencia",
            description = "Crea un ingreso de emergencia con su triaje. Los niveles 1 y 2 avisan a los médicos.")
    @ApiResponse(responseCode = "201", description = "Emergencia registrada")
    public ResponseEntity<IngresoHospResponse> registrarEmergencia(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody EmergenciaRequest request) {
        IngresoHospResponse creada = hospitalizacionService.registrarEmergencia(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/hospitalizacion/ingresos/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/emergencias")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION')")
    @Operation(
            summary = "Cola de emergencias",
            description = "Emergencias activas ordenadas por nivel de triaje y antigüedad.")
    @ApiResponse(responseCode = "200", description = "Emergencias activas")
    public ResponseEntity<List<IngresoHospResponse>> listarEmergencias() {
        return ResponseEntity.ok(hospitalizacionService.listarEmergencias());
    }

    @GetMapping("/mapa")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION')")
    @Operation(summary = "Mapa de camas", description = "Camas activas agrupadas por área con su ocupante.")
    @ApiResponse(responseCode = "200", description = "Mapa de camas")
    public ResponseEntity<MapaCamasResponse> mapa() {
        return ResponseEntity.ok(hospitalizacionService.mapa());
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ENFERMERIA', 'MEDICO', 'ADMIN', 'RECEPCION')")
    @Operation(summary = "Indicadores de hospitalización", description = "Ocupación, ingresos y emergencias.")
    @ApiResponse(responseCode = "200", description = "Indicadores")
    public ResponseEntity<HospResumenResponse> resumen() {
        return ResponseEntity.ok(hospitalizacionService.resumen());
    }

    @GetMapping("/mis-ingresos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mis ingresos", description = "Ingresos del paciente autenticado.")
    @ApiResponse(responseCode = "200", description = "Ingresos del paciente")
    public ResponseEntity<List<IngresoHospResponse>> misIngresos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(hospitalizacionService.misIngresos(principal));
    }
}
