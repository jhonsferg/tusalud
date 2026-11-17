package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoteResponse(
        UUID id,
        UUID productoId,
        String productoNombre,
        String numeroLote,
        LocalDate fechaVencimiento,
        int cantidadInicial,
        int cantidadActual,
        BigDecimal costoUnitario,
        UUID proveedorId,
        String proveedorNombre,
        String ubicacion,
        EstadoLote estado,
        LocalDateTime createdAt) {

    public static LoteResponse from(Lote lote, LocalDate hoy) {
        return new LoteResponse(
                lote.getId(),
                lote.getProducto().getId(),
                lote.getProducto().getNombre(),
                lote.getNumeroLote(),
                lote.getFechaVencimiento(),
                lote.getCantidadInicial(),
                lote.getCantidadActual(),
                lote.getCostoUnitario(),
                lote.getProveedor() == null ? null : lote.getProveedor().getId(),
                lote.getProveedor() == null ? null : lote.getProveedor().getRazonSocial(),
                lote.getUbicacion(),
                lote.estado(hoy),
                lote.getCreatedAt());
    }
}
