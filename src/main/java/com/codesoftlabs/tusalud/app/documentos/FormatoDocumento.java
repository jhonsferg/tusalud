package com.codesoftlabs.tusalud.app.documentos;

import java.util.Locale;

public enum FormatoDocumento {
    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String mime;
    private final String extension;

    FormatoDocumento(String mime, String extension) {
        this.mime = mime;
        this.extension = extension;
    }

    public String mime() {
        return mime;
    }

    public String extension() {
        return extension;
    }

    public static FormatoDocumento desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return PDF;
        }
        String normalizado = texto.trim().toUpperCase(Locale.ROOT);
        for (FormatoDocumento formato : values()) {
            if (formato.name().equals(normalizado)
                    || formato.extension.toUpperCase(Locale.ROOT).equals(normalizado)) {
                return formato;
            }
        }
        return null;
    }
}
