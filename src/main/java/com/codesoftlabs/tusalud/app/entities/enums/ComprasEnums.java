package com.codesoftlabs.tusalud.app.entities.enums;

public final class ComprasEnums {

    private ComprasEnums() {}

    public enum EstadoRequisicion {
        BORRADOR,
        ENVIADA,
        EN_COTIZACION,
        ATENDIDA,
        RECHAZADA,
        ANULADA
    }

    public enum PrioridadRequisicion {
        NORMAL,
        URGENTE
    }

    public enum AreaSolicitante {
        ALMACEN,
        FARMACIA
    }

    public enum EstadoCotizacion {
        VIGENTE,
        SELECCIONADA,
        DESCARTADA
    }

    public enum EstadoOrdenCompra {
        BORRADOR,
        PENDIENTE_APROBACION,
        APROBADA,
        PARCIAL,
        RECIBIDA,
        RECHAZADA,
        CERRADA,
        ANULADA;

        public boolean admiteRecepcion() {
            return this == APROBADA || this == PARCIAL;
        }

        public boolean aprobada() {
            return this == APROBADA || this == PARCIAL || this == RECIBIDA || this == CERRADA;
        }
    }

    public enum NivelAprobacion {
        FINANZAS,
        GERENCIA
    }

    public enum TipoComprobanteProveedor {
        FACTURA,
        BOLETA,
        RECIBO
    }

    public enum EstadoComprobanteProveedor {
        PENDIENTE,
        PARCIAL,
        PAGADO,
        ANULADO
    }

    public enum MetodoPagoProveedor {
        TRANSFERENCIA,
        EFECTIVO,
        CHEQUE,
        YAPE_PLIN
    }
}
