package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.EstadisticasResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.EstadisticasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/estadisticas")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Indicadores y gestión reservados para administradores")
public class AdminEstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Indicadores generales",
            description =
                    "Totales de pacientes, médicos y especialidades, citas de hoy y pendientes, ingresos del mes y totales, los cinco médicos con más citas atendidas, los ingresos de los últimos seis meses y las citas por especialidad. Requiere rol ADMIN.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
        @ApiResponse(
                responseCode = "403",
                description = "Se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EstadisticasResponse> obtener() {
        return ResponseEntity.ok(estadisticasService.obtener());
    }
}
