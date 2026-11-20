package com.codesoftlabs.tusalud.app.dto.response;

import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.AreaSolicitante;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoCotizacion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.NivelAprobacion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.PrioridadRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.TipoComprobanteProveedor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ComprasResponses {

    private ComprasResponses() {}

    public record ConfiguracionCompras(BigDecimal limiteFinanzas, BigDecimal tasaIgv) {}

    public record RequisicionItemResponse(
            UUID id,
            UUID productoId,
            String productoCodigo,
            String productoNombre,
            String unidad,
            int cantidad,
            String observacion,
            long stockActual,
            int stockMinimo) {}

    public record RequisicionResumen(
            UUID id,
            String codigo,
            EstadoRequisicion estado,
            PrioridadRequisicion prioridad,
            AreaSolicitante area,
            UUID solicitanteId,
            String solicitanteNombre,
            String motivo,
            boolean sugerida,
            int cantidadItems,
            LocalDateTime creadaEn,
            LocalDateTime enviadaEn) {}

    public record OrdenReferencia(UUID id, String codigo, EstadoOrdenCompra estado, BigDecimal total) {}

    public record RequisicionDetalle(
            RequisicionResumen resumen,
            List<RequisicionItemResponse> items,
            String motivoResolucion,
            String resueltaPorNombre,
            LocalDateTime resueltaEn,
            int cotizaciones,
            List<OrdenReferencia> ordenes) {}

    public record SugerenciaRequisicion(
            UUID productoId,
            String productoCodigo,
            String productoNombre,
            String unidad,
            long stockActual,
            int stockMinimo,
            int cantidadSugerida) {}

    public record CotizacionItemResponse(
            UUID id,
            UUID productoId,
            String productoNombre,
            String unidad,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {}

    public record CotizacionResponse(
            UUID id,
            UUID requisicionId,
            UUID proveedorId,
            String proveedorNombre,
            String proveedorRuc,
            EstadoCotizacion estado,
            LocalDate vigenciaHasta,
            boolean vencida,
            Integer plazoEntregaDias,
            int condicionPagoDias,
            String observaciones,
            BigDecimal total,
            String registradaPorNombre,
            LocalDateTime creadaEn,
            List<CotizacionItemResponse> items) {}

    public record ColumnaComparativa(
            UUID cotizacionId,
            String proveedorNombre,
            EstadoCotizacion estado,
            BigDecimal total,
            boolean cubreTodo,
            Integer plazoEntregaDias,
            int condicionPagoDias,
            boolean mejorTotal) {}

    public record FilaComparativa(
            UUID productoId,
            String productoNombre,
            String unidad,
            int cantidad,
            List<BigDecimal> precios,
            UUID mejorCotizacionId) {}

    public record ComparativaResponse(
            UUID requisicionId, List<ColumnaComparativa> columnas, List<FilaComparativa> filas) {}

    public record OrdenItemResponse(
            UUID id,
            UUID productoId,
            String productoCodigo,
            String productoNombre,
            String unidad,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            int recibida,
            int rechazada,
            int pendiente) {}

    public record OrdenCompraResumen(
            UUID id,
            String codigo,
            EstadoOrdenCompra estado,
            UUID proveedorId,
            String proveedorNombre,
            BigDecimal total,
            NivelAprobacion nivelAprobacion,
            String creadaPorNombre,
            String requisicionCodigo,
            LocalDateTime creadaEn,
            LocalDateTime aprobadaEn,
            int porcentajeRecibido) {}

    public record RecepcionResumen(
            UUID id,
            String codigo,
            UUID ordenId,
            String ordenCodigo,
            String proveedorNombre,
            String guiaRemision,
            String recibidaPorNombre,
            LocalDateTime recibidaEn,
            boolean conDiscrepancia,
            UUID ingresoId) {}

    public record OrdenCompraDetalle(
            OrdenCompraResumen resumen,
            UUID proveedorId,
            String proveedorRuc,
            UUID requisicionId,
            UUID cotizacionId,
            boolean afectaIgv,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total,
            int condicionPagoDias,
            LocalDate fechaEntregaEstimada,
            String observaciones,
            String hash,
            String aprobadaPorNombre,
            String motivoResolucion,
            LocalDateTime enviadaEn,
            LocalDateTime cerradaEn,
            LocalDateTime anuladaEn,
            BigDecimal facturado,
            List<OrdenItemResponse> items,
            List<RecepcionResumen> recepciones) {}

    public record RecepcionItemResponse(
            UUID id,
            UUID ordenItemId,
            UUID productoId,
            String productoNombre,
            int recibida,
            int rechazada,
            int devuelta,
            String numeroLote,
            LocalDate fechaVencimiento,
            String ubicacion,
            UUID loteId,
            BigDecimal costoUnitario,
            String motivoDiscrepancia) {}

    public record RecepcionDetalle(RecepcionResumen resumen, String observaciones, List<RecepcionItemResponse> items) {}

    public record DevolucionItemResponse(
            UUID id, String productoNombre, String numeroLote, int cantidad, BigDecimal costoUnitario) {}

    public record DevolucionResponse(
            UUID id,
            String codigo,
            UUID proveedorId,
            String proveedorNombre,
            UUID recepcionId,
            String recepcionCodigo,
            UUID comprobanteId,
            String motivo,
            BigDecimal monto,
            String registradaPorNombre,
            LocalDateTime registradaEn,
            List<DevolucionItemResponse> items) {}

    public record PagoProveedorResponse(
            UUID id,
            BigDecimal monto,
            MetodoPagoProveedor metodo,
            String referencia,
            LocalDate fecha,
            String registradoPorNombre,
            LocalDateTime registradoEn) {}

    public record ComprobanteProveedorResponse(
            UUID id,
            UUID proveedorId,
            String proveedorNombre,
            String proveedorRuc,
            UUID ordenId,
            String ordenCodigo,
            TipoComprobanteProveedor tipo,
            String numero,
            LocalDate fechaEmision,
            LocalDate fechaVencimiento,
            BigDecimal subtotal,
            BigDecimal igv,
            BigDecimal total,
            BigDecimal montoPagado,
            BigDecimal montoCredito,
            BigDecimal saldo,
            EstadoComprobanteProveedor estado,
            boolean vencido,
            long diasVencido,
            String observaciones,
            String registradoPorNombre,
            LocalDateTime registradoEn,
            String motivoAnulacion,
            List<PagoProveedorResponse> pagos) {}

    public record TramoVencimiento(String etiqueta, BigDecimal monto, long cantidad) {}

    public record SaldoProveedor(
            UUID proveedorId, String proveedorNombre, BigDecimal saldo, BigDecimal vencido, long comprobantes) {}

    public record ResumenCuentasPorPagar(
            BigDecimal totalPendiente,
            BigDecimal totalVencido,
            long cantidadPendientes,
            long cantidadVencidas,
            List<TramoVencimiento> tramos,
            List<SaldoProveedor> porProveedor) {}

    public record ReporteCompraProveedor(
            UUID proveedorId,
            String proveedorNombre,
            String proveedorRuc,
            long ordenes,
            BigDecimal montoOrdenado,
            BigDecimal montoRecibido,
            BigDecimal facturado,
            BigDecimal pagado,
            BigDecimal saldo) {}

    public record ResumenCompras(
            long requisicionesAbiertas,
            long ordenesPorAprobar,
            long ordenesEnTransito,
            BigDecimal porPagar,
            BigDecimal vencido,
            BigDecimal limiteFinanzas) {}

    public record ComprasAuditoriaResponse(
            UUID id,
            LocalDateTime fecha,
            String usuarioNombre,
            String entidad,
            UUID entidadId,
            String accion,
            String detalle) {}
}
