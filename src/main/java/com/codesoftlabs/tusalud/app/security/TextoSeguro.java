package com.codesoftlabs.tusalud.app.security;

public final class TextoSeguro {

    private TextoSeguro() {}

    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String sinControl = texto.replaceAll("\\p{Cntrl}", " ");
        String compacto = sinControl.replaceAll("\\s+", " ").trim();
        return compacto.isEmpty() ? null : compacto;
    }
}
