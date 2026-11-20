package com.codesoftlabs.tusalud.app.entities.enums;

public final class PlanillaEnums {

    private PlanillaEnums() {}

    public enum RegimenLaboral {
        GENERAL,
        PEQUENA_EMPRESA,
        MICROEMPRESA,
        PRACTICAS
    }

    public enum SistemaPensionario {
        ONP,
        AFP
    }

    public enum EstadoContrato {
        VIGENTE,
        FINALIZADO
    }

    public enum TipoConcepto {
        INGRESO,
        DESCUENTO,
        APORTE_EMPLEADOR
    }

    public enum ModoConcepto {
        MONTO_FIJO,
        PORCENTAJE_BASICO
    }

    public enum TipoMarcacion {
        ENTRADA,
        SALIDA
    }

    public enum OrigenMarcacion {
        MANUAL,
        TERMINAL
    }

    public enum EstadoAsistencia {
        PRESENTE,
        TARDANZA,
        FALTA,
        AUSENCIA_APROBADA,
        SIN_SALIDA,
        SIN_TURNO
    }

    public enum DestinoExtra {
        PAGO,
        COMPENSACION
    }

    public enum EstadoPlanilla {
        BORRADOR,
        CALCULADA,
        APROBADA,
        PAGADA,
        CERRADA
    }
}
