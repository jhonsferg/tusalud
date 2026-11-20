package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsiento;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsientoLinea;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.ConceptoMonto;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EventoContable;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.LadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.PlantillaAsientoRepository;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroAnulado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroRegistrado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraAnulada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraRegistrada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PagoProveedorRegistrado;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class Contabilizador {

    private final PlantillaAsientoRepository plantillas;
    private final AsientoService asientos;
    private final TesoreriaService tesoreria;

    @Transactional
    public void procesar(Object hecho) {
        switch (hecho) {
            case CobroRegistrado h -> cobro(h);
            case CobroAnulado h -> cobroAnulado(h);
            case CompraRegistrada h -> compra(h);
            case CompraAnulada h -> compraAnulada(h);
            case PagoProveedorRegistrado h -> pagoProveedor(h);
            default -> throw new IllegalArgumentException("Hecho contable desconocido: " + hecho.getClass());
        }
    }

    private void cobro(CobroRegistrado h) {
        boolean efectivo = h.metodo() == MetodoPagoCaja.EFECTIVO;
        BigDecimal cubierto = h.cubierto() == null ? BigDecimal.ZERO : h.cubierto();
        Map<ConceptoMonto, BigDecimal> importes = new EnumMap<>(ConceptoMonto.class);
        importes.put(ConceptoMonto.TOTAL, h.total());
        importes.put(ConceptoMonto.SUBTOTAL, h.subtotal());
        importes.put(ConceptoMonto.IGV, h.igv());
        importes.put(ConceptoMonto.CUBIERTO, cubierto);
        importes.put(ConceptoMonto.INGRESO_BRUTO, h.subtotal().add(cubierto));
        Asiento asiento = generar(
                efectivo ? EventoContable.COBRO_EFECTIVO : EventoContable.COBRO_ELECTRONICO,
                OrigenAsiento.COBRO,
                h.comprobanteId(),
                h.fecha(),
                "Cobro del comprobante " + h.codigo(),
                importes);
        if (!efectivo) {
            tesoreria.movimientoAutomatico(
                    OrigenMovimiento.COBRO,
                    h.comprobanteId(),
                    h.fecha(),
                    TipoMovimientoTesoreria.INGRESO,
                    h.total(),
                    "Cobro del comprobante " + h.codigo() + " con " + h.metodo(),
                    h.codigo(),
                    asiento);
        }
    }

    private void cobroAnulado(CobroAnulado h) {
        asientos.reversarAutomatico(OrigenAsiento.COBRO, h.comprobanteId(), "Anulación del comprobante " + h.codigo());
        tesoreria.anularMovimientoAutomatico(
                OrigenMovimiento.COBRO, h.comprobanteId(), h.fecha(), "Anulación del cobro " + h.codigo());
    }

    private void compra(CompraRegistrada h) {
        Map<ConceptoMonto, BigDecimal> importes = new EnumMap<>(ConceptoMonto.class);
        importes.put(ConceptoMonto.TOTAL, h.total());
        importes.put(ConceptoMonto.SUBTOTAL, h.subtotal());
        importes.put(ConceptoMonto.IGV, h.igv());
        generar(
                EventoContable.COMPRA_PROVEEDOR,
                OrigenAsiento.COMPRA_PROVEEDOR,
                h.comprobanteId(),
                h.fecha(),
                "Comprobante " + h.numero() + " de " + h.proveedor(),
                importes);
    }

    private void compraAnulada(CompraAnulada h) {
        asientos.reversarAutomatico(
                OrigenAsiento.COMPRA_PROVEEDOR, h.comprobanteId(), "Anulación del comprobante " + h.numero());
    }

    private void pagoProveedor(PagoProveedorRegistrado h) {
        boolean efectivo = h.metodo() == MetodoPagoProveedor.EFECTIVO;
        Map<ConceptoMonto, BigDecimal> importes = new EnumMap<>(ConceptoMonto.class);
        importes.put(ConceptoMonto.MONTO, h.monto());
        Asiento asiento = generar(
                efectivo ? EventoContable.PAGO_PROVEEDOR_EFECTIVO : EventoContable.PAGO_PROVEEDOR_BANCO,
                OrigenAsiento.PAGO_PROVEEDOR,
                h.pagoId(),
                h.fecha(),
                "Pago del comprobante " + h.numero() + " a " + h.proveedor(),
                importes);
        if (!efectivo) {
            tesoreria.movimientoAutomatico(
                    OrigenMovimiento.PAGO_PROVEEDOR,
                    h.pagoId(),
                    h.fecha(),
                    TipoMovimientoTesoreria.EGRESO,
                    h.monto(),
                    "Pago a " + h.proveedor() + " del comprobante " + h.numero(),
                    h.numero(),
                    asiento);
        }
    }

    private Asiento generar(
            EventoContable evento,
            OrigenAsiento origen,
            java.util.UUID referenciaId,
            java.time.LocalDate fecha,
            String glosa,
            Map<ConceptoMonto, BigDecimal> importes) {
        PlantillaAsiento plantilla = plantillas
                .conLineas(evento)
                .orElseThrow(() -> new ConflictException("No existe la plantilla de asiento " + evento));
        if (!plantilla.isActiva()) {
            throw new ConflictException("La plantilla de asiento " + evento + " está inactiva");
        }
        List<AsientoService.LineaCalculada> lineas = new ArrayList<>();
        for (PlantillaAsientoLinea linea : plantilla.getLineas()) {
            BigDecimal importe = importes.getOrDefault(linea.getConcepto(), BigDecimal.ZERO);
            if (importe.signum() <= 0) {
                continue;
            }
            boolean debe = linea.getLado() == LadoAsiento.DEBE;
            lineas.add(new AsientoService.LineaCalculada(
                    linea.getCuenta(), debe ? importe : BigDecimal.ZERO, debe ? BigDecimal.ZERO : importe, null));
        }
        return asientos.registrarAutomatico(origen, referenciaId, fecha, glosa, lineas);
    }
}
