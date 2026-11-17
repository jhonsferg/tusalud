package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.Billetera;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIntencionPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Intención de pago con billetera digital y su código QR")
public record PagoQrResponse(
        @Schema(example = "5b0f0a9e-6c2e-4f6d-9a3c-0a1b2c3d4e5f")
        String codigo,

        @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID citaId,

        @Schema(example = "YAPE") Billetera billetera,

        @Schema(example = "150.00") BigDecimal monto,

        @Schema(example = "30.00") BigDecimal descuento,

        @Schema(example = "120.00") BigDecimal total,

        @Schema(example = "SALUD20") String codigoCupon,

        @Schema(example = "tusalud-pay://yape?codigo=5b0f0a9e&monto=120.00&ref=CITA-10")
        String qrPayload,

        @Schema(example = "PENDIENTE") EstadoIntencionPago estado,

        @Schema(type = "string", example = "2026-10-19T09:05:00")
        LocalDateTime expiraEn,

        @Schema(example = "7", description = "Pago generado cuando la intención está CONFIRMADA")
        UUID pagoId) {

    public static PagoQrResponse from(IntencionPago intencion, String qrPayload) {
        return new PagoQrResponse(
                intencion.getCodigo(),
                intencion.getCita().getId(),
                intencion.getBilletera(),
                intencion.getMonto(),
                intencion.getDescuento(),
                intencion.getTotal(),
                intencion.getCodigoCupon(),
                qrPayload,
                intencion.getEstado(),
                intencion.getExpiraEn(),
                intencion.getPago() == null ? null : intencion.getPago().getId());
    }
}
