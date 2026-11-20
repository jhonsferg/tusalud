package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.repositories.ComprobanteProveedorRepository;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.services.facturacion.ConfiguracionFiscalService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrigenesContablesJpa implements OrigenesContables {

    private final ComprobanteProveedorRepository comprobantes;
    private final CuentaAseguradoraRepository cuentasAseguradora;
    private final ConfiguracionFiscalService configuracionFiscal;

    @Override
    public List<CompraContable> comprasEntre(LocalDate desde, LocalDate hasta) {
        return comprobantes.emitidosEnPeriodo(desde, hasta, null).stream()
                .map(OrigenesContablesJpa::compra)
                .toList();
    }

    @Override
    public List<VencimientoPorPagar> porPagar() {
        return comprobantes
                .deEstados(EnumSet.of(EstadoComprobanteProveedor.PENDIENTE, EstadoComprobanteProveedor.PARCIAL))
                .stream()
                .filter(c -> c.saldo().signum() > 0)
                .map(c -> new VencimientoPorPagar(c.getFechaVencimiento(), c.saldo()))
                .toList();
    }

    @Override
    public List<VencimientoPorCobrar> porCobrar(int plazoDias) {
        return cuentasAseguradora.porEstado(EstadoCuentaAseguradora.PENDIENTE).stream()
                .map((CuentaAseguradora c) ->
                        new VencimientoPorCobrar(c.getFecha().toLocalDate().plusDays(plazoDias), c.getMontoCubierto()))
                .toList();
    }

    @Override
    public Optional<Emisor> emisor() {
        return configuracionFiscal.actual().map(c -> new Emisor(c.getRuc(), c.getRazonSocial()));
    }

    private static CompraContable compra(ComprobanteProveedor c) {
        return new CompraContable(
                c.getId(),
                c.getTipo().name(),
                c.getSerie(),
                c.getNumero(),
                c.getFechaEmision(),
                c.getFechaVencimiento(),
                c.getRucEmisor(),
                c.getProveedor().getRazonSocial(),
                c.getSubtotal(),
                c.getIgv(),
                c.getTotal());
    }
}
