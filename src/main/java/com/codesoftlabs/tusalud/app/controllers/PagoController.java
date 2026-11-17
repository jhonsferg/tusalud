package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pagos", description = "Pago de citas y comprobantes del paciente")
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/citas/{id}/pago")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(
            summary = "Pagar cita",
            description =
                    "Registra el pago de una cita PENDIENTE o CONFIRMADA del paciente con uno de sus métodos de pago. Aplica el cupón si se envía; si no es válido, la operación se rechaza.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pago registrado"),
        @ApiResponse(
                responseCode = "400",
                description = "Datos inválidos",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "La cita no pertenece al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita o método de pago no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La cita ya fue pagada, no admite pago en su estado o el cupón no es válido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoResponse> pagar(
            @Parameter(description = "ID de la cita", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Valid @RequestBody PagoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.pagarCita(id, request, principal));
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Mis pagos", description = "Pagos del paciente, los más recientes primero.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de pagos"),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<PagoResponse>> misPagos(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoService.listarMisPagos(principal));
    }

    @GetMapping("/pagos/{id}")
    @PreAuthorize("hasRole('PACIENTE')")
    @Operation(summary = "Obtener un pago", description = "Solo el paciente dueño del pago.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago encontrado"),
        @ApiResponse(
                responseCode = "404",
                description = "Pago no encontrado para el paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoResponse> obtener(
            @Parameter(description = "ID del pago", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") @PathVariable
                    UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoService.obtenerMiPago(principal, id));
    }
}
