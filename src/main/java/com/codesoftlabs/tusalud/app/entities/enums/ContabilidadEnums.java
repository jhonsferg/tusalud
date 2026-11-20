package com.codesoftlabs.tusalud.app.entities.enums;

public final class ContabilidadEnums {

    private ContabilidadEnums() {}

    public enum TipoCuentaContable {
        ACTIVO,
        PASIVO,
        PATRIMONIO,
        INGRESO,
        GASTO;

        public NaturalezaCuenta naturalezaPorDefecto() {
            return this == ACTIVO || this == GASTO ? NaturalezaCuenta.DEUDORA : NaturalezaCuenta.ACREEDORA;
        }
    }

    public enum NaturalezaCuenta {
        DEUDORA,
        ACREEDORA
    }

    public enum EstadoPeriodo {
        ABIERTO,
        CERRADO
    }

    public enum EstadoAsiento {
        BORRADOR,
        CONTABILIZADO,
        REVERTIDO
    }

    public enum OrigenAsiento {
        MANUAL,
        COBRO,
        COMPRA_PROVEEDOR,
        PAGO_PROVEEDOR,
        MOVIMIENTO_TESORERIA,
        REVERSA
    }

    public enum EventoContable {
        COBRO_EFECTIVO,
        COBRO_ELECTRONICO,
        COMPRA_PROVEEDOR,
        PAGO_PROVEEDOR_EFECTIVO,
        PAGO_PROVEEDOR_BANCO
    }

    public enum LadoAsiento {
        DEBE,
        HABER
    }

    public enum ConceptoMonto {
        TOTAL,
        SUBTOTAL,
        IGV,
        CUBIERTO,
        INGRESO_BRUTO,
        MONTO
    }

    public enum TipoCuentaTesoreria {
        CAJA,
        BANCO
    }

    public enum TipoMovimientoTesoreria {
        INGRESO,
        EGRESO
    }

    public enum OrigenMovimiento {
        MANUAL,
        COBRO,
        PAGO_PROVEEDOR
    }

    public enum EstadoLineaExtracto {
        PENDIENTE,
        CONCILIADA,
        IGNORADA
    }
}
