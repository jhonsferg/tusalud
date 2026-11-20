package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ReporteCompraProveedor;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ResumenCompras;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ResumenCuentasPorPagar;
import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.OrdenCompraItem;
import com.codesoftlabs.tusalud.app.entities.Proveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenCompraRepository;
import com.codesoftlabs.tusalud.app.repositories.RequisicionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportesComprasService {

    private final OrdenCompraRepository ordenRepository;
    private final ComprobanteProveedorRepository comprobanteRepository;
    private final RequisicionRepository requisicionRepository;
    private final CuentasPorPagarService cuentasPorPagar;

    @Value("${app.compras.limite-finanzas:5000.00}")
    private BigDecimal limiteFinanzas;

    public List<ReporteCompraProveedor> porProveedor(LocalDate desde, LocalDate hasta, UUID proveedorId) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(90) : desde;
        List<OrdenCompra> ordenes = ordenRepository.aprobadasEnPeriodo(
                EnumSet.of(
                        EstadoOrdenCompra.APROBADA,
                        EstadoOrdenCompra.PARCIAL,
                        EstadoOrdenCompra.RECIBIDA,
                        EstadoOrdenCompra.CERRADA),
                inicio.atStartOfDay(),
                fin.plusDays(1).atStartOfDay(),
                proveedorId);
        List<ComprobanteProveedor> comprobantes = comprobanteRepository.emitidosEnPeriodo(inicio, fin, proveedorId);
        Map<UUID, Acumulado> acumulados = new LinkedHashMap<>();
        for (OrdenCompra orden : ordenes) {
            Proveedor proveedor = orden.getProveedor();
            Acumulado a = acumulados.computeIfAbsent(proveedor.getId(), k -> new Acumulado(proveedor));
            a.ordenes++;
            a.ordenado = a.ordenado.add(orden.getTotal());
            BigDecimal recibido = BigDecimal.ZERO;
            for (OrdenCompraItem item : orden.getItems()) {
                recibido =
                        recibido.add(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidadRecibida())));
            }
            if (orden.isAfectaIgv()) {
                recibido = recibido.add(recibido.multiply(OrdenCompra.TASA_IGV));
            }
            a.recibido = a.recibido.add(recibido);
        }
        for (ComprobanteProveedor c : comprobantes) {
            Proveedor proveedor = c.getProveedor();
            Acumulado a = acumulados.computeIfAbsent(proveedor.getId(), k -> new Acumulado(proveedor));
            a.facturado = a.facturado.add(c.getTotal());
            a.pagado = a.pagado.add(c.getMontoPagado());
            a.saldo = a.saldo.add(c.saldo());
        }
        return acumulados.values().stream()
                .map(a -> new ReporteCompraProveedor(
                        a.proveedor.getId(),
                        a.proveedor.getRazonSocial(),
                        a.proveedor.getRuc(),
                        a.ordenes,
                        a.ordenado,
                        a.recibido,
                        a.facturado,
                        a.pagado,
                        a.saldo))
                .sorted(Comparator.comparing(ReporteCompraProveedor::montoOrdenado)
                        .reversed())
                .toList();
    }

    public ResumenCompras resumen() {
        ResumenCuentasPorPagar cuentas = cuentasPorPagar.resumen();
        return new ResumenCompras(
                requisicionRepository.contarAbiertas(),
                ordenRepository.countByEstado(EstadoOrdenCompra.PENDIENTE_APROBACION),
                ordenRepository.countByEstado(EstadoOrdenCompra.APROBADA)
                        + ordenRepository.countByEstado(EstadoOrdenCompra.PARCIAL),
                cuentas.totalPendiente(),
                cuentas.totalVencido(),
                limiteFinanzas);
    }

    private static final class Acumulado {
        private final Proveedor proveedor;
        private long ordenes;
        private BigDecimal ordenado = BigDecimal.ZERO;
        private BigDecimal recibido = BigDecimal.ZERO;
        private BigDecimal facturado = BigDecimal.ZERO;
        private BigDecimal pagado = BigDecimal.ZERO;
        private BigDecimal saldo = BigDecimal.ZERO;

        private Acumulado(Proveedor proveedor) {
            this.proveedor = proveedor;
        }
    }
}
