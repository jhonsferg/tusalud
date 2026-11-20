package com.codesoftlabs.tusalud.app.entities.enums;

public final class CronicosEnums {

    private CronicosEnums() {}

    public enum ProgramaCronico {
        HIPERTENSION,
        DIABETES,
        ASMA,
        OTRO
    }

    public enum EstadoPlan {
        ACTIVO,
        COMPLETADO,
        SUSPENDIDO
    }

    public enum ParametroSeguimiento {
        PRESION_SISTOLICA,
        PRESION_DIASTOLICA,
        GLUCOSA_AYUNO,
        HEMOGLOBINA_GLICOSILADA,
        PESO_KG,
        FLUJO_ESPIRATORIO
    }

    public enum EstadoControl {
        PENDIENTE,
        CUMPLIDO,
        OMITIDO
    }

    public enum OrigenMedicion {
        PACIENTE,
        PERSONAL
    }
}
