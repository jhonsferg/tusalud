package com.codesoftlabs.tusalud.app.exceptions;

import lombok.Getter;

@Getter
public class RangoNoSatisfacibleException extends RuntimeException {

    private final long tamano;

    public RangoNoSatisfacibleException(long tamano) {
        super("El rango solicitado no es válido para este archivo");
        this.tamano = tamano;
    }
}
