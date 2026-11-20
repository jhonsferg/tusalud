package com.codesoftlabs.tusalud.app.entities.enums;

import java.util.EnumSet;
import java.util.Set;

public final class OdontologiaEnums {

    private OdontologiaEnums() {}

    public enum Superficie {
        V("Vestibular"),
        L("Lingual o palatina"),
        M("Mesial"),
        D("Distal"),
        O("Oclusal o incisal");

        private final String etiqueta;

        Superficie(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    public enum EstadoPieza {
        SANO("Sano", true),
        CARIES("Caries", true),
        OBTURADO("Obturado", true),
        SELLANTE("Sellante", true),
        FRACTURA("Fractura", true),
        CORONA("Corona", false),
        ENDODONCIA("Endodoncia", false),
        IMPLANTE("Implante", false),
        PROTESIS("Prótesis", false),
        EXTRACCION_INDICADA("Extracción indicada", false),
        AUSENTE("Ausente", false);

        private final String etiqueta;
        private final boolean porSuperficie;

        EstadoPieza(String etiqueta, boolean porSuperficie) {
            this.etiqueta = etiqueta;
            this.porSuperficie = porSuperficie;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public boolean admiteSuperficie() {
            return porSuperficie;
        }
    }

    public enum EstadoPlan {
        BORRADOR,
        PRESENTADO,
        ACEPTADO,
        RECHAZADO,
        EN_CURSO,
        COMPLETADO,
        CANCELADO;

        public static Set<EstadoPlan> editables() {
            return EnumSet.of(BORRADOR);
        }

        public static Set<EstadoPlan> ejecutables() {
            return EnumSet.of(ACEPTADO, EN_CURSO);
        }

        public static Set<EstadoPlan> cobrables() {
            return EnumSet.of(ACEPTADO, EN_CURSO, COMPLETADO);
        }
    }

    public enum EstadoItemPlan {
        PENDIENTE,
        REALIZADO,
        CANCELADO
    }
}
