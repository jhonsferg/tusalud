package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.ProductoResponse;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService.StockProducto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductoResponseMapper {

    private static final StockProducto SIN_STOCK = new StockProducto(0, null);

    private final InventarioService inventarioService;

    public List<ProductoResponse> aRespuestas(List<Producto> productos) {
        Map<Long, StockProducto> stock = inventarioService.stockDe(
                productos.stream().map(Producto::getId).toList());
        return productos.stream()
                .map(producto -> {
                    StockProducto s = stock.getOrDefault(producto.getId(), SIN_STOCK);
                    return ProductoResponse.from(producto, s.stock(), s.proximoVencimiento());
                })
                .toList();
    }

    public ProductoResponse aRespuesta(Producto producto) {
        return aRespuestas(List.of(producto)).get(0);
    }
}
