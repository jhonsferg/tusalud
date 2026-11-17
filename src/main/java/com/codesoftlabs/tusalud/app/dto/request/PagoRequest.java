package com.codesoftlabs.tusalud.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Solicitud de pago de una cita")
public record PagoRequest(
        @Schema(example = "1") @NotNull(message = "El método de pago es obligatorio")
        UUID metodoPagoId,

        @Schema(example = "SALUD20", description = "Código de cupón opcional")
        @Size(max = 30, message = "El código del cupón no debe superar 30 caracteres")
        String codigoCupon) {}
