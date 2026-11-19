package com.codesoftlabs.tusalud.app.entities.enums;

public final class DocumentosMedicosEnums {

    private DocumentosMedicosEnums() {}

    public enum TipoDocumentoMedico {
        CERTIFICADO("CM", "certificado-medico", "Certificado médico"),
        DESCANSO("DM", "descanso-medico", "Descanso médico"),
        INFORME("IM", "informe-medico", "Informe médico"),
        REFERENCIA("RF", "referencia-medica", "Referencia"),
        CONTRARREFERENCIA("CR", "contrarreferencia-medica", "Contrarreferencia"),
        EPICRISIS("EP", "epicrisis", "Epicrisis");

        private final String serie;
        private final String plantilla;
        private final String etiqueta;

        TipoDocumentoMedico(String serie, String plantilla, String etiqueta) {
            this.serie = serie;
            this.plantilla = plantilla;
            this.etiqueta = etiqueta;
        }

        public String serie() {
            return serie;
        }

        public String plantilla() {
            return plantilla;
        }

        public String etiqueta() {
            return etiqueta;
        }
    }

    public enum EstadoDocumentoMedico {
        BORRADOR,
        EMITIDO,
        ANULADO
    }

    public enum NivelEstablecimiento {
        PRIMER_NIVEL,
        SEGUNDO_NIVEL,
        TERCER_NIVEL,
        ESPECIALIZADO
    }

    public enum PrioridadReferencia {
        NORMAL,
        PREFERENTE,
        URGENTE
    }

    public enum EstadoReferencia {
        ENVIADA,
        RECIBIDA,
        ATENDIDA,
        RECHAZADA
    }

    public enum EstadoConsentimiento {
        PENDIENTE_FIRMA,
        FIRMADO,
        REVOCADO,
        VENCIDO
    }

    public enum FirmanteTipo {
        PACIENTE,
        REPRESENTANTE
    }

    public enum ViaFirma {
        APP_PACIENTE,
        CONSULTORIO
    }
}
