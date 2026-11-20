package com.codesoftlabs.tusalud.app.entities.enums;

import java.util.List;

public final class QuirofanoEnums {

    private QuirofanoEnums() {}

    public enum EstadoQuirofano {
        DISPONIBLE,
        MANTENIMIENTO,
        FUERA_DE_SERVICIO
    }

    public enum TipoCirugia {
        ELECTIVA,
        URGENCIA
    }

    public enum EstadoCirugia {
        SOLICITADA,
        PROGRAMADA,
        EN_PREPARACION,
        EN_CURSO,
        FINALIZADA,
        SUSPENDIDA,
        CANCELADA;

        public boolean ocupaQuirofano() {
            return this == PROGRAMADA || this == EN_PREPARACION || this == EN_CURSO;
        }

        public boolean abierta() {
            return this != FINALIZADA && this != CANCELADA;
        }
    }

    public enum TipoAnestesia {
        GENERAL,
        REGIONAL,
        LOCAL,
        SEDACION,
        NINGUNA;

        public boolean requiereConsentimientoAnestesico() {
            return this != NINGUNA;
        }
    }

    public enum RolQuirurgico {
        CIRUJANO,
        AYUDANTE,
        ANESTESIOLOGO,
        INSTRUMENTISTA,
        CIRCULANTE;

        public boolean esMedico() {
            return this == CIRUJANO || this == AYUDANTE || this == ANESTESIOLOGO;
        }
    }

    public enum MomentoChecklist {
        ANTES_INDUCCION(List.of(
                "IDENTIDAD_CONFIRMADA",
                "SITIO_MARCADO",
                "CONSENTIMIENTOS_VERIFICADOS",
                "EQUIPO_ANESTESIA_REVISADO",
                "ALERGIAS_REVISADAS")),
        ANTES_INCISION(List.of(
                "EQUIPO_SE_PRESENTO",
                "PROCEDIMIENTO_CONFIRMADO",
                "ANTIBIOTICO_ADMINISTRADO",
                "ESTERILIDAD_CONFIRMADA",
                "IMAGENES_DISPONIBLES")),
        ANTES_SALIDA(List.of(
                "PROCEDIMIENTO_REGISTRADO",
                "CONTEO_INSTRUMENTAL_COMPLETO",
                "MUESTRAS_ETIQUETADAS",
                "PROBLEMAS_DEL_EQUIPO_REVISADOS",
                "PLAN_POSTOPERATORIO_DEFINIDO"));

        private final List<String> items;

        MomentoChecklist(List<String> items) {
            this.items = items;
        }

        public List<String> items() {
            return items;
        }
    }

    public enum TipoEsterilizacion {
        VAPOR,
        PLASMA,
        OXIDO_ETILENO
    }

    public enum ResultadoIndicador {
        PENDIENTE,
        CONFORME,
        NO_CONFORME
    }

    public enum ResultadoCiclo {
        EN_PROCESO,
        CONFORME,
        NO_CONFORME
    }

    public enum EstadoLoteEsteril {
        ESTERIL,
        EN_USO,
        USADO
    }

    public enum EstadoEfectivoLote {
        ESTERIL,
        EN_ESPERA_DE_INDICADOR,
        EN_USO,
        USADO,
        BLOQUEADO,
        VENCIDO
    }
}
