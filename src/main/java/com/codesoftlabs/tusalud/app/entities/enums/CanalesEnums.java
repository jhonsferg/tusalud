package com.codesoftlabs.tusalud.app.entities.enums;

public final class CanalesEnums {

    private CanalesEnums() {}

    public enum Canal {
        CORREO("Correo electrónico"),
        SMS("Mensaje de texto"),
        WHATSAPP("WhatsApp");

        private final String etiqueta;

        Canal(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    public enum EstadoEntrega {
        PENDIENTE,
        ENVIADO,
        ENTREGADO,
        FALLIDO,
        REBOTADO
    }

    public enum EstadoContacto {
        SIN_CONFIGURAR,
        PENDIENTE_VERIFICACION,
        ACTIVO,
        BAJA
    }

    public enum TipoEjecucionRespaldo {
        RESPALDO,
        PRUEBA_RESTAURACION
    }

    public enum EstadoEjecucionRespaldo {
        EXITOSO,
        FALLIDO
    }

    public enum AlcanceRespaldo {
        BASE,
        ALMACEN,
        COMPLETO
    }

    public enum CategoriaRetencion {
        DIARIO,
        SEMANAL,
        MENSUAL
    }
}
