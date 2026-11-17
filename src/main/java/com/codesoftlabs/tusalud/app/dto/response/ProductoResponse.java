package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        TipoProducto tipo,
        String principioActivo,
        String presentacion,
        String unidad,
        BigDecimal precioVenta,
        int stockMinimo,
        boolean requiereReceta,
        boolean activo,
        long stockActual,
        LocalDate proximoVencimiento,
        boolean bajoStock) {

    public static ProductoResponse from(Producto producto, long stockActual, LocalDate proximoVencimiento) {
        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getTipo(),
                producto.getPrincipioActivo(),
                producto.getPresentacion(),
                producto.getUnidad(),
                producto.getPrecioVenta(),
                producto.getStockMinimo(),
                producto.isRequiereReceta(),
                producto.isActivo(),
                stockActual,
                proximoVencimiento,
                stockActual <= producto.getStockMinimo());
    }
}
