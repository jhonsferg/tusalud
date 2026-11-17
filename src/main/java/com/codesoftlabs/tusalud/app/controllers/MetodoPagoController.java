package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.MetodoPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metodos-pago")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PACIENTE')")
@Tag(name = "Métodos de pago", description = "Tarjetas guardadas del paciente autenticado")
public class MetodoPagoController {

    private final MetodoPagoService metodoPagoService;

    @GetMapping
    @Operation(summary = "Listar mis métodos de pago", description = "El predeterminado aparece primero.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Métodos de pago del paciente"),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<MetodoPagoResponse>> listar(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(metodoPagoService.listar(principal));
    }

    @PostMapping
    @Operation(
            summary = "Registrar método de pago",
            description =
                    "Valida el número (Luhn, 13 a 19 dígitos) y el vencimiento. Solo se guardan la marca y los últimos 4 dígitos; el CVV no se almacena. El primer método queda como predeterminado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Método de pago registrado"),
        @ApiResponse(
                responseCode = "400",
                description = "Número, vencimiento o CVV inválidos, o tarjeta vencida",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MetodoPagoResponse> crear(
            @Valid @RequestBody MetodoPagoRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metodoPagoService.crear(principal, request));
    }

    @PutMapping("/{id:\\d+}/predeterminado")
    @Operation(summary = "Marcar como predeterminado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Método marcado como predeterminado"),
        @ApiResponse(
                responseCode = "404",
                description = "Método de pago no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MetodoPagoResponse> marcarPredeterminado(
            @Parameter(description = "ID del método de pago", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(metodoPagoService.marcarPredeterminado(principal, id));
    }

    @DeleteMapping("/{id:\\d+}")
    @Operation(
            summary = "Eliminar método de pago",
            description =
                    "Si era el predeterminado, el siguiente método pasa a serlo. Los pagos anteriores conservan su descripción.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Método de pago eliminado"),
        @ApiResponse(
                responseCode = "404",
                description = "Método de pago no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del método de pago", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioPrincipal principal) {
        metodoPagoService.eliminar(principal, id);
        return ResponseEntity.noContent().build();
    }
}
