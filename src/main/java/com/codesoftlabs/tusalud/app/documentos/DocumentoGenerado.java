package com.codesoftlabs.tusalud.app.documentos;

public record DocumentoGenerado(byte[] contenido, String nombre, FormatoDocumento formato) {

    public String mime() {
        return formato.mime();
    }
}
