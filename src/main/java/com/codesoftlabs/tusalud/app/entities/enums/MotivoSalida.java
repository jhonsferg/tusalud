package com.codesoftlabs.tusalud.app.entities.enums;

public enum MotivoSalida {
    CONSUMO_INTERNO,
    MERMA,
    VENCIMIENTO,
    AJUSTE_NEGATIVO;

    public MotivoMovimiento aMovimiento() {
        return MotivoMovimiento.valueOf(name());
    }
}
