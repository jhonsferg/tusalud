package com.codesoftlabs.tusalud.app.storage;

public class ObjetoNoEncontradoException extends AlmacenamientoException {

    public ObjetoNoEncontradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
