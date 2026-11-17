package com.codesoftlabs.tusalud.app.entities.enums;

public enum PrioridadTurno {
    NORMAL("A", 2),
    PREFERENTE("P", 1),
    URGENTE("U", 0);

    private final String prefijo;
    private final int orden;

    PrioridadTurno(String prefijo, int orden) {
        this.prefijo = prefijo;
        this.orden = orden;
    }

    public String prefijo() {
        return prefijo;
    }

    public int orden() {
        return orden;
    }
}
