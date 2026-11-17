package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.Billetera;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de un código QR para pagar una cita con una billetera digital")
public record PagoQrRequest(
        @Schema(example = "10") @NotNull(message = "La cita es obligatoria")
        Long citaId,

        @Schema(example = "YAPE") @NotNull(message = "La billetera es obligatoria")
        Billetera billetera,

        @Schema(example = "SALUD20", description = "Código de cupón opcional")
        @Size(max = 30, message = "El código del cupón no debe superar 30 caracteres")
        String codigoCupon) {}
