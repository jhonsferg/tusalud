package com.codesoftlabs.tusalud.app.storage;

import java.util.Set;

public enum TipoContenido {
    JPEG("image/jpeg", true, Set.of("jpg", "jpeg")),
    PNG("image/png", true, Set.of("png")),
    WEBP("image/webp", true, Set.of("webp")),
    PDF("application/pdf", true, Set.of("pdf")),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", false, Set.of("xlsx")),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", false, Set.of("docx")),
    CSV("text/csv", false, Set.of("csv")),
    JRXML("application/xml", false, Set.of("jrxml", "xml"));

    private final String mime;
    private final boolean visualizable;
    private final Set<String> extensiones;

    TipoContenido(String mime, boolean visualizable, Set<String> extensiones) {
        this.mime = mime;
        this.visualizable = visualizable;
        this.extensiones = extensiones;
    }

    public String mime() {
        return mime;
    }

    public boolean visualizable() {
        return visualizable;
    }

    public boolean admiteExtension(String extension) {
        return extension != null && extensiones.contains(extension.toLowerCase(java.util.Locale.ROOT));
    }

    public String extensionPrincipal() {
        return extensiones.stream().sorted().findFirst().orElse("bin");
    }

    public static TipoContenido desdeMime(String mime) {
        for (TipoContenido tipo : values()) {
            if (tipo.mime.equalsIgnoreCase(mime)) {
                return tipo;
            }
        }
        return null;
    }
}
