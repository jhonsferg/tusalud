package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.entities.Lote;
import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.LoteRepository;
import com.codesoftlabs.tusalud.app.repositories.MovimientoInventarioRepository;
import com.codesoftlabs.tusalud.app.repositories.StockProyeccion;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventarioServiceImpl implements InventarioService {

    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Override
    @Transactional
    public List<MovimientoInventario> descontar(
            Producto producto,
            int cantidad,
            MotivoMovimiento motivo,
            String referencia,
            Usuario usuario,
            String observaciones) {
        LocalDate hoy = LocalDate.now();
        List<Lote> lotes = motivo == MotivoMovimiento.VENCIMIENTO
                ? loteRepository.vencidosParaDescontar(producto.getId(), hoy)
                : loteRepository.vigentesParaDescontar(producto.getId(), hoy);
        long disponible = lotes.stream().mapToLong(Lote::getCantidadActual).sum();
        if (disponible < cantidad) {
            throw new ConflictException("Stock insuficiente de " + producto.getNombre() + ": disponible " + disponible
                    + ", solicitado " + cantidad);
        }
        int pendiente = cantidad;
        List<MovimientoInventario> movimientos = new ArrayList<>();
        for (Lote lote : lotes) {
            if (pendiente == 0) {
                break;
            }
            int tomado = Math.min(pendiente, lote.getCantidadActual());
            lote.setCantidadActual(lote.getCantidadActual() - tomado);
            pendiente -= tomado;
            movimientos.add(movimientoRepository.save(MovimientoInventario.builder()
                    .fecha(LocalDateTime.now())
                    .tipo(motivo.tipo())
                    .motivo(motivo)
                    .producto(producto)
                    .lote(lote)
                    .cantidad(tomado)
                    .usuario(usuario)
                    .referencia(referencia)
                    .observaciones(observaciones)
                    .build()));
        }
        return movimientos;
    }

    @Override
    @Transactional
    public void devolverPorReferencia(String referencia, Usuario usuario) {
        List<MovimientoInventario> salidas =
                movimientoRepository.porReferencia(referencia, TipoMovimiento.SALIDA, MotivoMovimiento.DISPENSACION);
        for (MovimientoInventario salida : salidas) {
            Lote lote = salida.getLote();
            lote.setCantidadActual(lote.getCantidadActual() + salida.getCantidad());
            movimientoRepository.save(MovimientoInventario.builder()
                    .fecha(LocalDateTime.now())
                    .tipo(MotivoMovimiento.ANULACION_DISPENSACION.tipo())
                    .motivo(MotivoMovimiento.ANULACION_DISPENSACION)
                    .producto(salida.getProducto())
                    .lote(lote)
                    .cantidad(salida.getCantidad())
                    .usuario(usuario)
                    .referencia(referencia)
                    .build());
        }
    }

    @Override
    public Map<Long, StockProducto> stockDe(Collection<Long> productoIds) {
        Map<Long, StockProducto> resultado = new HashMap<>();
        if (productoIds.isEmpty()) {
            return resultado;
        }
        for (StockProyeccion fila : loteRepository.stockDe(productoIds, LocalDate.now())) {
            resultado.put(fila.getProductoId(), new StockProducto(fila.getStock(), fila.getProximo()));
        }
        return resultado;
    }
}
