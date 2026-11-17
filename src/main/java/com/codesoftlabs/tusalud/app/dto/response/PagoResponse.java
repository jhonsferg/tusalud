package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Pago registrado de una cita")
public record PagoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "10") Long citaId,
        @Schema(example = "Juan Pérez") String pacienteNombre,
        @Schema(example = "Luis García") String medicoNombre,
        @Schema(example = "Cardiología") String especialidad,

        @Schema(type = "string", example = "2026-10-19T09:00:00")
        LocalDateTime fechaCita,

        @Schema(example = "150.00") BigDecimal monto,
        @Schema(example = "15.00") BigDecimal descuento,
        @Schema(example = "135.00") BigDecimal total,
        @Schema(example = "BIENVENIDA10") String codigoCupon,
        @Schema(example = "PAGADO") EstadoPago estado,
        @Schema(example = "B001-000001") String comprobante,
        @Schema(example = "VISA terminada en 4242") String metodo,

        @Schema(type = "string", example = "2026-10-12T21:00:00")
        LocalDateTime fechaPago) {

    public static PagoResponse from(Pago pago) {
        Cita cita = pago.getCita();
        return new PagoResponse(
                pago.getId(),
                cita.getId(),
                cita.getPaciente().getUsuario().getNombreCompleto(),
                cita.getMedico().getUsuario().getNombreCompleto(),
                cita.getMedico().getEspecialidad().getNombre(),
                cita.getFechaHora(),
                pago.getMonto(),
                pago.getDescuento(),
                pago.getTotal(),
                pago.getCodigoCupon(),
                pago.getEstado(),
                pago.getComprobante(),
                pago.getMetodoDescripcion(),
                pago.getFechaPago());
    }
}
