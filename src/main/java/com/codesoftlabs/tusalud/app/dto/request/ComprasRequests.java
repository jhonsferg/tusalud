package com.codesoftlabs.tusalud.app.dto.request;

import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.MetodoPagoProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.PrioridadRequisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.TipoComprobanteProveedor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ComprasRequests {

    private ComprasRequests() {}

    public record MotivoCompraRequest(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivo) {}

    public record RequisicionItemRequest(
            @NotNull(message = "El producto es obligatorio") UUID productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            @Max(value = 100000, message = "La cantidad no puede superar 100000")
            Integer cantidad,

            @Size(max = 200, message = "La observación no puede superar 200 caracteres")
            String observacion) {}

    public record RequisicionRequest(
            PrioridadRequisicion prioridad,

            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivo,

            Boolean sugerida,

            @NotEmpty(message = "Debe incluir al menos un producto") @Valid
            List<RequisicionItemRequest> items) {}

    public record CotizacionItemRequest(
            @NotNull(message = "El producto es obligatorio") UUID productoId,

            @NotNull(message = "El precio es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
            BigDecimal precioUnitario) {}

    public record CotizacionRequest(
            @NotNull(message = "El proveedor es obligatorio")
            UUID proveedorId,

            LocalDate vigenciaHasta,

            @Min(value = 0, message = "El plazo no puede ser negativo")
            @Max(value = 365, message = "El plazo no puede superar 365 días")
            Integer plazoEntregaDias,

            @Min(value = 0, message = "La condición de pago no puede ser negativa")
            @Max(value = 365, message = "La condición de pago no puede superar 365 días")
            Integer condicionPagoDias,

            @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
            String observaciones,

            @NotEmpty(message = "Debe cotizar al menos un producto") @Valid
            List<CotizacionItemRequest> items) {}

    public record OrdenItemRequest(
            @NotNull(message = "El producto es obligatorio") UUID productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            @Max(value = 100000, message = "La cantidad no puede superar 100000")
            Integer cantidad,

            @NotNull(message = "El precio es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
            BigDecimal precioUnitario) {}

    public record OrdenCompraRequest(
            @NotNull(message = "El proveedor es obligatorio")
            UUID proveedorId,

            UUID requisicionId,
            UUID cotizacionId,
            Boolean afectaIgv,

            @Min(value = 0, message = "La condición de pago no puede ser negativa")
            @Max(value = 365, message = "La condición de pago no puede superar 365 días")
            Integer condicionPagoDias,

            LocalDate fechaEntregaEstimada,

            @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
            String observaciones,

            @NotEmpty(message = "Debe incluir al menos un producto") @Valid
            List<OrdenItemRequest> items) {}

    public record RecepcionItemRequest(
            @NotNull(message = "El ítem de la orden es obligatorio")
            UUID ordenItemId,

            @NotNull(message = "La cantidad recibida es obligatoria")
            @Min(value = 0, message = "La cantidad recibida no puede ser negativa")
            Integer cantidadRecibida,

            @Min(value = 0, message = "La cantidad rechazada no puede ser negativa")
            Integer cantidadRechazada,

            @Size(max = 40, message = "El número de lote no puede superar 40 caracteres")
            String numeroLote,

            LocalDate fechaVencimiento,

            @Size(max = 60, message = "La ubicación no puede superar 60 caracteres")
            String ubicacion,

            @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
            String motivoDiscrepancia) {}

    public record RecepcionRequest(
            @Size(max = 40, message = "La guía no puede superar 40 caracteres")
            String guiaRemision,

            @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
            String observaciones,

            @NotEmpty(message = "Debe registrar al menos un ítem") @Valid
            List<RecepcionItemRequest> items) {}

    public record DevolucionItemRequest(
            @NotNull(message = "El ítem de la recepción es obligatorio")
            UUID recepcionItemId,

            @NotNull(message = "La cantidad es obligatoria")
            @Min(value = 1, message = "La cantidad debe ser mayor a cero")
            Integer cantidad) {}

    public record DevolucionRequest(
            @NotNull(message = "La recepción es obligatoria")
            UUID recepcionId,

            UUID comprobanteId,

            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
            String motivo,

            @NotEmpty(message = "Debe incluir al menos un ítem") @Valid
            List<DevolucionItemRequest> items) {}

    public record AplicarCreditoRequest(
            @NotNull(message = "El comprobante es obligatorio")
            UUID comprobanteId) {}

    public record ComprobanteProveedorRequest(
            @NotNull(message = "El proveedor es obligatorio")
            UUID proveedorId,

            UUID ordenId,
            @NotNull(message = "El tipo es obligatorio") TipoComprobanteProveedor tipo,

            @NotBlank(message = "La serie es obligatoria")
            @Pattern(regexp = "[A-Za-z0-9]{1,10}", message = "La serie solo admite letras y números")
            String serie,

            @NotBlank(message = "El número es obligatorio")
            @Pattern(regexp = "\\d{1,20}", message = "El número solo admite dígitos")
            String numero,

            @NotBlank(message = "El RUC del emisor es obligatorio")
            @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
            String rucEmisor,

            @NotNull(message = "La fecha de emisión es obligatoria")
            LocalDate fechaEmision,

            LocalDate fechaVencimiento,

            @NotNull(message = "El subtotal es obligatorio")
            @DecimalMin(value = "0.01", message = "El subtotal debe ser mayor a cero")
            BigDecimal subtotal,

            @DecimalMin(value = "0.00", message = "El IGV no puede ser negativo")
            BigDecimal igv,

            @Size(max = 300, message = "Las observaciones no pueden superar 300 caracteres")
            String observaciones) {}

    public record PagoProveedorRequest(
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
            BigDecimal monto,

            @NotNull(message = "El método de pago es obligatorio")
            MetodoPagoProveedor metodo,

            @Size(max = 60, message = "La referencia no puede superar 60 caracteres")
            String referencia,

            LocalDate fecha) {}
}
