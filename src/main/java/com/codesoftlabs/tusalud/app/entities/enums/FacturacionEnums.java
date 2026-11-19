package com.codesoftlabs.tusalud.app.entities.enums;

public final class FacturacionEnums {

    private FacturacionEnums() {}

    public enum TipoCpe {
        FACTURA("01", "Factura", "F"),
        BOLETA("03", "Boleta de venta", "B"),
        NOTA_CREDITO("07", "Nota de crédito", "C"),
        NOTA_DEBITO("08", "Nota de débito", "D");

        private final String codigo;
        private final String etiqueta;
        private final String sufijo;

        TipoCpe(String codigo, String etiqueta, String sufijo) {
            this.codigo = codigo;
            this.etiqueta = etiqueta;
            this.sufijo = sufijo;
        }

        public String codigo() {
            return codigo;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public String sufijo() {
            return sufijo;
        }

        public boolean esNota() {
            return this == NOTA_CREDITO || this == NOTA_DEBITO;
        }
    }

    public enum EstadoCpe {
        PENDIENTE,
        ACEPTADO,
        ACEPTADO_OBSERVACIONES,
        RECHAZADO,
        ANULADO,
        ERROR;

        public boolean aceptado() {
            return this == ACEPTADO || this == ACEPTADO_OBSERVACIONES;
        }
    }

    public enum Afectacion {
        GRAVADO("10", "1000", "IGV", "VAT"),
        EXONERADO("20", "9997", "EXO", "VAT"),
        INAFECTO("30", "9998", "INA", "FRE");

        private final String codigo;
        private final String tributo;
        private final String nombre;
        private final String internacional;

        Afectacion(String codigo, String tributo, String nombre, String internacional) {
            this.codigo = codigo;
            this.tributo = tributo;
            this.nombre = nombre;
            this.internacional = internacional;
        }

        public String codigo() {
            return codigo;
        }

        public String tributo() {
            return tributo;
        }

        public String nombre() {
            return nombre;
        }

        public String internacional() {
            return internacional;
        }
    }

    public enum ProveedorFiscal {
        SIMULACION,
        SUNAT,
        OSE,
        PSE
    }

    public enum ModoFiscal {
        BETA,
        PRODUCCION
    }

    public enum TipoResumen {
        RESUMEN_DIARIO("RC"),
        COMUNICACION_BAJA("RA");

        private final String prefijo;

        TipoResumen(String prefijo) {
            this.prefijo = prefijo;
        }

        public String prefijo() {
            return prefijo;
        }
    }

    public enum EstadoResumen {
        PENDIENTE,
        ENVIADO,
        ACEPTADO,
        RECHAZADO,
        ERROR
    }

    public enum OperacionSunat {
        ENVIO_COMPROBANTE,
        ENVIO_RESUMEN,
        CONSULTA_TICKET
    }
}
