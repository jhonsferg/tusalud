package com.codesoftlabs.tusalud.app.controllers;

import com.codesoftlabs.tusalud.app.dto.response.ErrorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ValidacionCuponResponse;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cupones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('PACIENTE')")
@Tag(name = "Cupones", description = "Validación de cupones de descuento para pacientes")
public class CuponController {

    private final CuponService cuponService;

    @GetMapping("/validar")
    @Operation(
            summary = "Validar cupón",
            description =
                    "Indica si el cupón puede aplicarse al monto indicado y cuánto descuenta. Un cupón no válido no es un error HTTP: responde valido=false con el motivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resultado de la validación"),
        @ApiResponse(
                responseCode = "400",
                description = "Monto inválido",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Acceso denegado: se requiere rol PACIENTE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ValidacionCuponResponse> validar(
            @Parameter(description = "Código del cupón", example = "SALUD20") @RequestParam(defaultValue = "")
                    String codigo,
            @Parameter(description = "Monto sobre el que se aplica el cupón", example = "150.00")
                    @RequestParam
                    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
                    BigDecimal monto) {
        return ResponseEntity.ok(cuponService.validar(codigo, monto));
    }
}
