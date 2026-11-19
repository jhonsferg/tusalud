package com.codesoftlabs.tusalud.app.storage;

import java.util.Set;

public enum PropositoArchivo {
    AVATAR(2 * 1024 * 1024, Set.of(TipoContenido.JPEG, TipoContenido.PNG, TipoContenido.WEBP)),
    IMAGEN_DIAGNOSTICA(5 * 1024 * 1024, Set.of(TipoContenido.JPEG, TipoContenido.PNG, TipoContenido.PDF)),
    DOCUMENTO_PACIENTE(
            10 * 1024 * 1024,
            Set.of(
                    TipoContenido.PDF,
                    TipoContenido.JPEG,
                    TipoContenido.PNG,
                    TipoContenido.DOCX,
                    TipoContenido.XLSX,
                    TipoContenido.CSV)),
    REPORTE(25 * 1024 * 1024, Set.of(TipoContenido.PDF, TipoContenido.XLSX, TipoContenido.DOCX, TipoContenido.CSV)),
    PLANTILLA(2 * 1024 * 1024, Set.of(TipoContenido.JRXML, TipoContenido.XLSX, TipoContenido.DOCX)),
    FIRMA_CONSENTIMIENTO(512 * 1024, Set.of(TipoContenido.PNG)),
    DOCUMENTO_CLINICO(10 * 1024 * 1024, Set.of(TipoContenido.PDF));

    private final long tamanoMaximo;
    private final Set<TipoContenido> tiposPermitidos;

    PropositoArchivo(long tamanoMaximo, Set<TipoContenido> tiposPermitidos) {
        this.tamanoMaximo = tamanoMaximo;
        this.tiposPermitidos = tiposPermitidos;
    }

    public long tamanoMaximo() {
        return tamanoMaximo;
    }

    public Set<TipoContenido> tiposPermitidos() {
        return tiposPermitidos;
    }

    public String bucket(StorageProperties.Buckets buckets) {
        return switch (this) {
            case AVATAR -> buckets.avatares();
            case IMAGEN_DIAGNOSTICA -> buckets.imagenes();
            case DOCUMENTO_PACIENTE -> buckets.documentos();
            case REPORTE -> buckets.reportes();
            case PLANTILLA -> buckets.plantillas();
            case FIRMA_CONSENTIMIENTO, DOCUMENTO_CLINICO -> buckets.documentos();
        };
    }
}
