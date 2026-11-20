package com.codesoftlabs.tusalud.app.entities.enums;

public enum MotivoMovimiento {
    COMPRA(TipoMovimiento.ENTRADA),
    CONSUMO_INTERNO(TipoMovimiento.SALIDA),
    MERMA(TipoMovimiento.SALIDA),
    VENCIMIENTO(TipoMovimiento.SALIDA),
    AJUSTE_NEGATIVO(TipoMovimiento.AJUSTE),
    AJUSTE_POSITIVO(TipoMovimiento.AJUSTE),
    DISPENSACION(TipoMovimiento.SALIDA),
    ANULACION_DISPENSACION(TipoMovimiento.ENTRADA),
    DEVOLUCION_PROVEEDOR(TipoMovimiento.SALIDA);

    private final TipoMovimiento tipo;

    MotivoMovimiento(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public TipoMovimiento tipo() {
        return tipo;
    }
}
