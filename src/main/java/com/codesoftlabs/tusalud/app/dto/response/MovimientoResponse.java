package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import java.time.LocalDateTime;
import java.util.UUID;

public record MovimientoResponse(
        UUID id,
        LocalDateTime fecha,
        TipoMovimiento tipo,
        MotivoMovimiento motivo,
        UUID productoId,
        String productoNombre,
        UUID loteId,
        String numeroLote,
        int cantidad,
        String usuarioNombre,
        String referencia) {

    public static MovimientoResponse from(MovimientoInventario movimiento) {
        return new MovimientoResponse(
                movimiento.getId(),
                movimiento.getFecha(),
                movimiento.getTipo(),
                movimiento.getMotivo(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getNombre(),
                movimiento.getLote().getId(),
                movimiento.getLote().getNumeroLote(),
                movimiento.getCantidad(),
                movimiento.getUsuario().getNombreCompleto(),
                movimiento.getReferencia());
    }
}
