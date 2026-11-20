package com.codesoftlabs.tusalud.app.services.contabilidad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrigenesContables {

    record CompraContable(
            UUID id,
            String tipo,
            String serie,
            String numero,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            String rucProveedor,
            String proveedor,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total) {}

    record VencimientoPorPagar(LocalDate fecha, BigDecimal saldo) {}

    record VencimientoPorCobrar(LocalDate fecha, BigDecimal monto) {}

    record Emisor(String ruc, String razonSocial) {}

    List<CompraContable> comprasEntre(LocalDate desde, LocalDate hasta);

    List<VencimientoPorPagar> porPagar();

    List<VencimientoPorCobrar> porCobrar(int plazoDias);

    Optional<Emisor> emisor();
}
