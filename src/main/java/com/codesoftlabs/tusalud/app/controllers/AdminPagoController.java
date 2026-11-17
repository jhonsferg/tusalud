package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/pagos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración - Pagos", description = "Consulta de pagos registrados (requiere rol ADMIN)")
public class AdminPagoController {

    private final PagoService pagoService;

    @GetMapping
    @Operation(
            summary = "Listar pagos",
            description = "Pagos entre dos fechas (inclusive). Sin fechas, devuelve todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de pagos"),
        @ApiResponse(
                responseCode = "400",
                description = "Fechas con formato inválido (yyyy-MM-dd) o rango incorrecto",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol ADMIN",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PagoResponse>> listar(
            @Parameter(description = "Fecha inicial (yyyy-MM-dd)", example = "2026-10-01")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @Parameter(description = "Fecha final (yyyy-MM-dd)", example = "2026-10-31")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fin) {
        return ResponseEntity.ok(pagoService.listarTodos(inicio, fin));
    }
}
