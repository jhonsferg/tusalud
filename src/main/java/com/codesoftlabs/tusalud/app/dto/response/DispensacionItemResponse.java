package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.DispensacionItem;
import java.math.BigDecimal;

public record DispensacionItemResponse(
        Long productoId,
        String productoNombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        Integer recetaItemId) {

    public static DispensacionItemResponse from(DispensacionItem item) {
        return new DispensacionItemResponse(
                item.getProducto().getId(),
                item.getProducto().getNombre(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal(),
                item.getRecetaPosicion());
    }
}
