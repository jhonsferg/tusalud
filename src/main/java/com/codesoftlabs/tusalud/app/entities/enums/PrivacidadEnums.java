package com.codesoftlabs.tusalud.app.entities.enums;

public final class PrivacidadEnums {

    private PrivacidadEnums() {}

    public enum TipoArco {
        ACCESO("Acceso", 8),
        RECTIFICACION("Rectificación", 10),
        CANCELACION("Cancelación", 10),
        OPOSICION("Oposición", 10);

        private final String etiqueta;
        private final int diasHabiles;

        TipoArco(String etiqueta, int diasHabiles) {
            this.etiqueta = etiqueta;
            this.diasHabiles = diasHabiles;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public int diasHabiles() {
            return diasHabiles;
        }
    }

    public enum EstadoArco {
        RECIBIDA,
        EN_REVISION,
        ATENDIDA,
        PARCIAL,
        RECHAZADA;

        public boolean abierta() {
            return this == RECIBIDA || this == EN_REVISION;
        }
    }

    public enum CampoRectificable {
        NOMBRE("Nombre"),
        APELLIDO("Apellido"),
        TELEFONO("Teléfono"),
        EMAIL("Correo electrónico"),
        FECHA_NACIMIENTO("Fecha de nacimiento"),
        DNI("Documento de identidad");

        private final String etiqueta;

        CampoRectificable(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    public enum AccionConsentimiento {
        OTORGAR,
        REVOCAR
    }

    public enum OrigenPrivacidad {
        REGISTRO,
        APP,
        SOLICITUD_ARCO,
        ADMIN
    }

    public enum TipoIncidente {
        ACCESO_NO_AUTORIZADO,
        PERDIDA,
        DIVULGACION,
        ALTERACION,
        RANSOMWARE,
        OTRO
    }

    public enum SeveridadIncidente {
        BAJA,
        MEDIA,
        ALTA,
        CRITICA
    }

    public enum EstadoIncidente {
        ABIERTO,
        EN_CONTENCION,
        NOTIFICADO,
        CERRADO
    }

    public enum AccionRetencion {
        ANONIMIZAR,
        DEPURAR,
        MARCAR
    }
}
