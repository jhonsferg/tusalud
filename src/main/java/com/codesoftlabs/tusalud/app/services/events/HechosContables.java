package com.codesoftlabs.tusalud.app.services.events;

import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class HechosContables {

    private HechosContables() {}

    public record CobroRegistrado(
            UUID comprobanteId,
            String codigo,
            LocalDate fecha,
            MetodoPagoCaja metodo,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total,
            BigDecimal cubierto) {}

    public record CobroAnulado(UUID comprobanteId, String codigo, LocalDate fecha, String motivo) {}

    public record CompraRegistrada(
            UUID comprobanteId,
            String numero,
            String proveedor,
            LocalDate fecha,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total) {}

    public record CompraAnulada(UUID comprobanteId, String numero, LocalDate fecha, String motivo) {}

    public record PagoProveedorRegistrado(
            UUID pagoId,
            UUID comprobanteId,
            String numero,
            String proveedor,
            LocalDate fecha,
            BigDecimal monto,
            MetodoPagoProveedor metodo) {}

    public record PlanillaAprobada(
            UUID planillaId,
            String codigo,
            LocalDate fecha,
            BigDecimal remuneracionBruta,
            BigDecimal aporteEmpleador,
            BigDecimal retencionPension,
            BigDecimal retencionRenta,
            BigDecimal otrosDescuentos,
            BigDecimal neto) {}

    public record PlanillaPagada(UUID planillaId, String codigo, LocalDate fecha, BigDecimal neto) {}
}
