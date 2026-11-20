package com.codesoftlabs.tusalud.app.entities.enums;

public final class HceEnums {

    private HceEnums() {}

    public enum TipoVersion {
        ADENDA,
        CORRECCION
    }

    public enum CampoCorregible {
        DIAGNOSTICO,
        TRATAMIENTO,
        INDICACIONES,
        OBSERVACIONES
    }
}
