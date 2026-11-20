package com.codesoftlabs.tusalud.app.entities.enums;

public final class PediatriaEnums {

    private PediatriaEnums() {}

    public enum EstadoRegistroVacunacion {
        VIGENTE,
        ANULADO
    }

    public enum EstadoDosis {
        APLICADA,
        ATRASADA,
        PROXIMA,
        PENDIENTE,
        FUERA_DE_EDAD
    }

    public enum IndicadorCrecimiento {
        PESO_EDAD("Peso para la edad", "kg"),
        TALLA_EDAD("Talla para la edad", "cm"),
        PC_EDAD("Perímetro cefálico para la edad", "cm"),
        IMC_EDAD("Índice de masa corporal para la edad", "kg/m²");

        private final String etiqueta;
        private final String unidad;

        IndicadorCrecimiento(String etiqueta, String unidad) {
            this.etiqueta = etiqueta;
            this.unidad = unidad;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public String unidad() {
            return unidad;
        }
    }

    public enum NivelAlerta {
        NORMAL,
        ATENCION,
        ALERTA
    }
}
