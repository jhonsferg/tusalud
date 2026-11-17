package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.CancelarOrdenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.ExamenLabRequest;
import com.codesoftlabs.tusalud.app.dto.request.OrdenLabCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.ResultadosLabRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ExamenLabResponse;
import com.codesoftlabs.tusalud.app.dto.response.LabResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.OrdenLabResponse;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaExamen;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.LaboratorioService;
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
@RequestMapping("/api/v1/laboratorio")
@RequiredArgsConstructor
@Validated
@Tag(name = "Laboratorio", description = "Catálogo de exámenes, órdenes, resultados y validación")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    @GetMapping("/examenes")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'MEDICO', 'ADMIN')")
    @Operation(summary = "Listar exámenes del catálogo", description = "Filtra por texto, categoría y estado.")
    @ApiResponse(responseCode = "200", description = "Listado de exámenes")
    public ResponseEntity<List<ExamenLabResponse>> listarExamenes(
            @Parameter(description = "Texto en el nombre o el código") @RequestParam(required = false) String q,
            @Parameter(description = "Categoría del examen") @RequestParam(required = false) CategoriaExamen categoria,
            @Parameter(description = "Solo activos o inactivos") @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(laboratorioService.listarExamenes(q, categoria, activo));
    }

    @GetMapping("/examenes/{id}")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'MEDICO', 'ADMIN')")
    @Operation(summary = "Obtener un examen", description = "Devuelve el examen con sus parámetros.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Examen encontrado"),
        @ApiResponse(
                responseCode = "404",
                description = "Examen no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ExamenLabResponse> obtenerExamen(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(laboratorioService.obtenerExamen(id));
    }

    @PostMapping("/examenes")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    @Operation(summary = "Crear un examen", description = "Requiere rol LABORATORIO o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Examen creado"),
        @ApiResponse(
                responseCode = "409",
                description = "El código ya existe",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ExamenLabResponse> crearExamen(@Valid @RequestBody ExamenLabRequest request) {
        ExamenLabResponse creado = laboratorioService.crearExamen(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/examenes/{id}")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    @Operation(
            summary = "Actualizar un examen",
            description = "Reemplaza los datos y los parámetros del examen. Requiere rol LABORATORIO o ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Examen actualizado"),
        @ApiResponse(
                responseCode = "404",
                description = "Examen no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ExamenLabResponse> actualizarExamen(
            @PathVariable @Positive Long id, @Valid @RequestBody ExamenLabRequest request) {
        return ResponseEntity.ok(laboratorioService.actualizarExamen(id, request));
    }

    @PostMapping("/ordenes")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Solicitar una orden de laboratorio",
            description = "El médico solicita exámenes para un paciente que atiende. Requiere rol MEDICO.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Orden solicitada"),
        @ApiResponse(
                responseCode = "403",
                description = "El médico no atiende al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Algún examen no está disponible",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> crearOrden(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody OrdenLabCreateRequest request) {
        OrdenLabResponse creada = laboratorioService.crearOrden(principal, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(location).body(creada);
    }

    @GetMapping("/ordenes")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN', 'MEDICO')")
    @Operation(
            summary = "Listar órdenes de laboratorio",
            description =
                    "El laboratorio y el administrador ven todas las órdenes y el médico solo las que solicitó. Máximo 200.")
    @ApiResponse(responseCode = "200", description = "Listado de órdenes")
    public ResponseEntity<List<OrdenLabResponse>> listarOrdenes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @Parameter(description = "Estado de la orden") @RequestParam(required = false) EstadoOrdenLab estado,
            @Parameter(description = "DNI, nombre o código") @RequestParam(required = false) String q,
            @Parameter(description = "Prioridad de la orden") @RequestParam(required = false)
                    PrioridadOrden prioridad) {
        return ResponseEntity.ok(laboratorioService.listarOrdenes(principal, estado, q, prioridad));
    }

    @GetMapping("/ordenes/{id}")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN', 'MEDICO', 'PACIENTE')")
    @Operation(
            summary = "Obtener una orden",
            description =
                    "Disponible para el laboratorio, el administrador, el médico solicitante y el paciente dueño cuando la orden está completada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada"),
        @ApiResponse(
                responseCode = "403",
                description = "Sin acceso a la orden",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Orden no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> obtenerOrden(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(laboratorioService.obtenerOrden(principal, id));
    }

    @PutMapping("/ordenes/{id}/muestra")
    @PreAuthorize("hasRole('LABORATORIO')")
    @Operation(summary = "Registrar la toma de muestra", description = "Pasa la orden de SOLICITADA a MUESTRA_TOMADA.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Muestra registrada"),
        @ApiResponse(
                responseCode = "409",
                description = "La orden no está solicitada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> tomarMuestra(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(laboratorioService.tomarMuestra(principal, id));
    }

    @PutMapping("/ordenes/{id}/items/{itemId}/resultados")
    @PreAuthorize("hasRole('LABORATORIO')")
    @Operation(
            summary = "Registrar los resultados de un examen",
            description = "Calcula si cada valor numérico queda fuera de su rango de referencia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultados registrados"),
        @ApiResponse(
                responseCode = "400",
                description = "Parámetro o valor inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La orden no admite resultados",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> registrarResultados(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody ResultadosLabRequest request) {
        return ResponseEntity.ok(laboratorioService.registrarResultados(principal, id, itemId, request));
    }

    @PutMapping("/ordenes/{id}/validar")
    @PreAuthorize("hasRole('LABORATORIO')")
    @Operation(
            summary = "Validar la orden",
            description = "Exige resultados de todos los exámenes, completa la orden y avisa al paciente y al médico.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden validada"),
        @ApiResponse(
                responseCode = "409",
                description = "Faltan resultados",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> validar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(laboratorioService.validar(principal, id));
    }

    @PutMapping("/ordenes/{id}/cancelar")
    @PreAuthorize("hasAnyRole('MEDICO', 'LABORATORIO', 'ADMIN')")
    @Operation(
            summary = "Cancelar una orden",
            description =
                    "El médico solicitante cancela mientras la orden está solicitada. El laboratorio y el administrador, hasta antes de completarla.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden cancelada"),
        @ApiResponse(
                responseCode = "403",
                description = "Sin permiso para cancelar",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La orden ya no se puede cancelar",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrdenLabResponse> cancelar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) CancelarOrdenLabRequest request) {
        return ResponseEntity.ok(laboratorioService.cancelar(principal, id, request));
    }

    @GetMapping("/mis-ordenes")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Mis órdenes de laboratorio",
            description =
                    "Devuelve las órdenes del paciente. Los resultados solo se incluyen en las órdenes completadas.")
    @ApiResponse(responseCode = "200", description = "Órdenes del paciente")
    public ResponseEntity<List<OrdenLabResponse>> misOrdenes(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(laboratorioService.misOrdenes(principal));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('LABORATORIO', 'ADMIN')")
    @Operation(
            summary = "Indicadores del laboratorio",
            description = "Pendientes, en proceso, completadas hoy y urgentes.")
    @ApiResponse(responseCode = "200", description = "Resumen de la cola")
    public ResponseEntity<LabResumenResponse> resumen() {
        return ResponseEntity.ok(laboratorioService.resumen());
    }
}
