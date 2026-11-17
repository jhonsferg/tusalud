package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.PagoQrRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.PagoQrResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pagos/qr")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PACIENTE')")
@Tag(name = "Pagos con billeteras", description = "Pago de citas con Yape o Plin mediante un código QR simulado")
public class PagoQrController {

    private final PagoQrService pagoQrService;

    @PostMapping
    @Operation(
            summary = "Generar un código QR de pago",
            description =
                    "Crea una intención de pago con Yape o Plin para una cita del paciente. El QR expira a los 5 minutos. Valida el cupón si se envía.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Intención creada"),
        @ApiResponse(
                responseCode = "403",
                description = "La cita no pertenece al paciente",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Cita no encontrada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "La cita ya fue pagada, no admite pago en su estado o el cupón no es válido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoQrResponse> crear(
            @Valid @RequestBody PagoQrRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoQrService.crear(request, principal));
    }

    @GetMapping("/{codigo}")
    @Operation(
            summary = "Consultar un pago con QR",
            description =
                    "Devuelve el estado de la intención; la marca como EXPIRADO si venció. Incluye el pagoId cuando está CONFIRMADO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado de la intención"),
        @ApiResponse(
                responseCode = "404",
                description = "Código no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoQrResponse> obtener(
            @Parameter(description = "Código de la intención") @PathVariable String codigo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoQrService.obtener(codigo, principal));
    }

    @PostMapping("/{codigo}/simular-confirmacion")
    @Operation(
            summary = "Simular la confirmación de la billetera",
            description =
                    "Equivale a escanear el QR y aprobar el pago en la app de la billetera. Solo está disponible si app.pagos.simulacion-habilitada es true; en producción se sustituye por la notificación real del proveedor.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pago confirmado y registrado"),
        @ApiResponse(
                responseCode = "403",
                description = "La simulación no está habilitada",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Código no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El código expiró o fue cancelado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoQrResponse> simularConfirmacion(
            @Parameter(description = "Código de la intención") @PathVariable String codigo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoQrService.simularConfirmacion(codigo, principal));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Cancelar un pago con QR", description = "Cancela una intención pendiente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Intención cancelada"),
        @ApiResponse(
                responseCode = "404",
                description = "Código no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "El pago ya fue confirmado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagoQrResponse> cancelar(
            @Parameter(description = "Código de la intención") @PathVariable String codigo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoQrService.cancelar(codigo, principal));
    }
}
