package com.codesoftlabs.tusalud.app.canales;

public class EnvioException extends RuntimeException {

    private final boolean permanente;

    public EnvioException(String mensaje, boolean permanente) {
        super(mensaje);
        this.permanente = permanente;
    }

    public EnvioException(String mensaje, boolean permanente, Throwable causa) {
        super(mensaje, causa);
        this.permanente = permanente;
    }

    public boolean permanente() {
        return permanente;
    }
}
