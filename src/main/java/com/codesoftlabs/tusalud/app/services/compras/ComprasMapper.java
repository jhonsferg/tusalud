package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.ComprobanteProveedorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.CotizacionItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.CotizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.DevolucionItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.DevolucionResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenCompraResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.OrdenItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.PagoProveedorResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionDetalle;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RecepcionResumen;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionItemResponse;
import com.codesoftlabs.tusalud.app.dto.response.ComprasResponses.RequisicionResumen;
import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.Cotizacion;
import com.codesoftlabs.tusalud.app.entities.CotizacionItem;
import com.codesoftlabs.tusalud.app.entities.DevolucionItem;
import com.codesoftlabs.tusalud.app.entities.DevolucionProveedor;
import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.OrdenCompraItem;
import com.codesoftlabs.tusalud.app.entities.PagoProveedor;
import com.codesoftlabs.tusalud.app.entities.RecepcionCompra;
import com.codesoftlabs.tusalud.app.entities.RecepcionItem;
import com.codesoftlabs.tusalud.app.entities.Requisicion;
import com.codesoftlabs.tusalud.app.entities.RequisicionItem;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService.StockProducto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ComprasMapper {

    private static final StockProducto SIN_STOCK = new StockProducto(0, null);

    private ComprasMapper() {}

    public static RequisicionResumen requisicion(Requisicion r) {
        return new RequisicionResumen(
                r.getId(),
                r.getCodigo(),
                r.getEstado(),
                r.getPrioridad(),
                r.getArea(),
                r.getSolicitante().getId(),
                r.getSolicitante().getNombreCompleto(),
                r.getMotivo(),
                r.isSugerida(),
                r.getItems().size(),
                r.getCreadaEn(),
                r.getEnviadaEn());
    }

    public static RequisicionItemResponse requisicionItem(RequisicionItem i, Map<UUID, StockProducto> stock) {
        StockProducto s = stock.getOrDefault(i.getProducto().getId(), SIN_STOCK);
        return new RequisicionItemResponse(
                i.getId(),
                i.getProducto().getId(),
                i.getProducto().getCodigo(),
                i.getProducto().getNombre(),
                i.getProducto().getUnidad(),
                i.getCantidad(),
                i.getObservacion(),
                s.stock(),
                i.getProducto().getStockMinimo());
    }

    public static CotizacionResponse cotizacion(Cotizacion c, LocalDate hoy) {
        List<CotizacionItemResponse> items =
                c.getItems().stream().map(ComprasMapper::cotizacionItem).toList();
        return new CotizacionResponse(
                c.getId(),
                c.getRequisicion().getId(),
                c.getProveedor().getId(),
                c.getProveedor().getRazonSocial(),
                c.getProveedor().getRuc(),
                c.getEstado(),
                c.getVigenciaHasta(),
                c.getVigenciaHasta() != null && c.getVigenciaHasta().isBefore(hoy),
                c.getPlazoEntregaDias(),
                c.getCondicionPagoDias(),
                c.getObservaciones(),
                c.getTotal(),
                c.getRegistradaPor().getNombreCompleto(),
                c.getCreadaEn(),
                items);
    }

    private static CotizacionItemResponse cotizacionItem(CotizacionItem i) {
        return new CotizacionItemResponse(
                i.getId(),
                i.getProducto().getId(),
                i.getProducto().getNombre(),
                i.getProducto().getUnidad(),
                i.getCantidad(),
                i.getPrecioUnitario(),
                i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
    }

    public static OrdenCompraResumen ordenResumen(OrdenCompra o) {
        return new OrdenCompraResumen(
                o.getId(),
                o.getCodigo(),
                o.getEstado(),
                o.getProveedor().getId(),
                o.getProveedor().getRazonSocial(),
                o.getTotal(),
                o.getNivelAprobacion(),
                o.getCreadaPor().getNombreCompleto(),
                o.getRequisicion() == null ? null : o.getRequisicion().getCodigo(),
                o.getCreadaEn(),
                o.getAprobadaEn(),
                porcentajeRecibido(o));
    }

    public static int porcentajeRecibido(OrdenCompra o) {
        int pedido =
                o.getItems().stream().mapToInt(OrdenCompraItem::getCantidad).sum();
        if (pedido == 0) {
            return 0;
        }
        int atendido = o.getItems().stream()
                .mapToInt(i -> i.getCantidadRecibida() + i.getCantidadRechazada())
                .sum();
        return Math.min(100, (int) Math.round(atendido * 100.0 / pedido));
    }

    public static OrdenItemResponse ordenItem(OrdenCompraItem i) {
        return new OrdenItemResponse(
                i.getId(),
                i.getProducto().getId(),
                i.getProducto().getCodigo(),
                i.getProducto().getNombre(),
                i.getProducto().getUnidad(),
                i.getCantidad(),
                i.getPrecioUnitario(),
                i.getSubtotal(),
                i.getCantidadRecibida(),
                i.getCantidadRechazada(),
                i.pendiente());
    }

    public static RecepcionResumen recepcionResumen(RecepcionCompra r) {
        return new RecepcionResumen(
                r.getId(),
                r.getCodigo(),
                r.getOrden().getId(),
                r.getOrden().codigoVisible(),
                r.getOrden().getProveedor().getRazonSocial(),
                r.getGuiaRemision(),
                r.getRecibidaPor().getNombreCompleto(),
                r.getRecibidaEn(),
                r.isConDiscrepancia(),
                r.getIngreso() == null ? null : r.getIngreso().getId());
    }

    public static RecepcionDetalle recepcionDetalle(RecepcionCompra r) {
        List<RecepcionItemResponse> items =
                r.getItems().stream().map(ComprasMapper::recepcionItem).toList();
        return new RecepcionDetalle(recepcionResumen(r), r.getObservaciones(), items);
    }

    private static RecepcionItemResponse recepcionItem(RecepcionItem i) {
        return new RecepcionItemResponse(
                i.getId(),
                i.getOrdenItem().getId(),
                i.getProducto().getId(),
                i.getProducto().getNombre(),
                i.getCantidadRecibida(),
                i.getCantidadRechazada(),
                i.getCantidadDevuelta(),
                i.getNumeroLote(),
                i.getFechaVencimiento(),
                i.getUbicacion(),
                i.getLote() == null ? null : i.getLote().getId(),
                i.getCostoUnitario(),
                i.getMotivoDiscrepancia());
    }

    public static DevolucionResponse devolucion(DevolucionProveedor d) {
        List<DevolucionItemResponse> items = d.getItems().stream()
                .map((DevolucionItem i) -> new DevolucionItemResponse(
                        i.getId(),
                        i.getLote().getProducto().getNombre(),
                        i.getLote().getNumeroLote(),
                        i.getCantidad(),
                        i.getCostoUnitario()))
                .toList();
        return new DevolucionResponse(
                d.getId(),
                d.getCodigo(),
                d.getProveedor().getId(),
                d.getProveedor().getRazonSocial(),
                d.getRecepcion().getId(),
                d.getRecepcion().getCodigo(),
                d.getComprobante() == null ? null : d.getComprobante().getId(),
                d.getMotivo(),
                d.getMonto(),
                d.getRegistradaPor().getNombreCompleto(),
                d.getRegistradaEn(),
                items);
    }

    public static PagoProveedorResponse pago(PagoProveedor p) {
        return new PagoProveedorResponse(
                p.getId(),
                p.getMonto(),
                p.getMetodo(),
                p.getReferencia(),
                p.getFecha(),
                p.getRegistradoPor().getNombreCompleto(),
                p.getRegistradoEn());
    }

    public static ComprobanteProveedorResponse comprobante(
            ComprobanteProveedor c, List<PagoProveedor> pagos, LocalDate hoy) {
        boolean vencido = c.vencido(hoy);
        return new ComprobanteProveedorResponse(
                c.getId(),
                c.getProveedor().getId(),
                c.getProveedor().getRazonSocial(),
                c.getProveedor().getRuc(),
                c.getOrden() == null ? null : c.getOrden().getId(),
                c.getOrden() == null ? null : c.getOrden().codigoVisible(),
                c.getTipo(),
                c.numeroCompleto(),
                c.getFechaEmision(),
                c.getFechaVencimiento(),
                c.getSubtotal(),
                c.getIgv(),
                c.getTotal(),
                c.getMontoPagado(),
                c.getMontoCredito(),
                c.saldo(),
                c.getEstado(),
                vencido,
                vencido ? ChronoUnit.DAYS.between(c.getFechaVencimiento(), hoy) : 0,
                c.getObservaciones(),
                c.getRegistradoPor().getNombreCompleto(),
                c.getRegistradoEn(),
                c.getMotivoAnulacion(),
                pagos.stream().map(ComprasMapper::pago).toList());
    }
}
