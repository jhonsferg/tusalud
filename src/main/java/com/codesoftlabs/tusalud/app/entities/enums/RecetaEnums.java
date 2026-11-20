package com.codesoftlabs.tusalud.app.entities.enums;

public final class RecetaEnums {

    private RecetaEnums() {}

    public enum CategoriaControl {
        NINGUNA("Sin control"),
        ANTIBIOTICO("Antibiótico"),
        PSICOTROPICO("Psicotrópico"),
        ESTUPEFACIENTE("Estupefaciente");

        private final String etiqueta;

        CategoriaControl(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public boolean especial() {
            return this == PSICOTROPICO || this == ESTUPEFACIENTE;
        }
    }

    public enum EstadoVenta {
        VIGENTE,
        SUSPENDIDO,
        RETIRADO
    }

    public enum SeveridadInteraccion {
        LEVE,
        MODERADA,
        GRAVE,
        CONTRAINDICADA
    }

    public enum CategoriaEmbarazo {
        A,
        B,
        C,
        D,
        X,
        SIN_CLASIFICAR
    }

    public enum CompatibilidadLactancia {
        COMPATIBLE,
        PRECAUCION,
        CONTRAINDICADO,
        SIN_CLASIFICAR
    }

    public enum EstadoRecetaElectronica {
        EMITIDA,
        DISPENSADA_PARCIAL,
        DISPENSADA,
        ANULADA
    }

    public enum TipoAlertaReceta {
        ALERGIA,
        INTERACCION,
        DUPLICIDAD,
        DOSIS_MAXIMA,
        EMBARAZO,
        LACTANCIA,
        RENAL
    }

    public enum SeveridadAlerta {
        INFORMATIVA,
        MODERADA,
        GRAVE,
        CONTRAINDICADA
    }
}
