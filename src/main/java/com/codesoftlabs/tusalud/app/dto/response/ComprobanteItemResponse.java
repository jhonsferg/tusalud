package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.ComprobanteItem;
import java.math.BigDecimal;

public record ComprobanteItemResponse(
        String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {

    public static ComprobanteItemResponse from(ComprobanteItem item) {
        return new ComprobanteItemResponse(
                item.getDescripcion(), item.getCantidad(), item.getPrecioUnitario(), item.getSubtotal());
    }
}
