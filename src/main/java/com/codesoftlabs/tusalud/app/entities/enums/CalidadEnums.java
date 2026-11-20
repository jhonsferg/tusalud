package com.codesoftlabs.tusalud.app.entities.enums;

public final class CalidadEnums {

    private CalidadEnums() {}

    public enum TipoEvento {
        EVENTO_ADVERSO,
        CASI_INCIDENTE,
        INCIDENTE_SIN_DANO
    }

    public enum SeveridadEvento {
        LEVE,
        MODERADO,
        GRAVE,
        CENTINELA
    }

    public enum EstadoEvento {
        NOTIFICADO,
        EN_ANALISIS,
        EN_ACCION,
        CERRADO
    }

    public enum MetodoAnalisis {
        CINCO_PORQUES,
        ESPINA_PESCADO
    }

    public enum EstadoAccion {
        PENDIENTE,
        HECHA
    }

    public enum EstadoGuia {
        VIGENTE,
        OBSOLETA,
        BORRADOR
    }
}
