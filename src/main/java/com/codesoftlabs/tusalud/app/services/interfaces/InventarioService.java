package com.codesoftlabs.tusalud.app.services.interfaces;

import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventarioService {

    record StockProducto(long stock, LocalDate proximoVencimiento) {}

    List<MovimientoInventario> descontar(
            Producto producto,
            int cantidad,
            MotivoMovimiento motivo,
            String referencia,
            Usuario usuario,
            String observaciones);

    void devolverPorReferencia(String referencia, Usuario usuario);

    Map<Long, StockProducto> stockDe(Collection<Long> productoIds);
}
