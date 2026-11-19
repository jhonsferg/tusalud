package com.codesoftlabs.tusalud.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.facturacion")
public record FacturacionProperties(
        Certificado certificado, Sol sol, Cola cola, String urlVerificacion, String sedePorDefecto) {

    public FacturacionProperties {
        certificado = certificado == null ? new Certificado(null, null, null, true) : certificado;
        sol = sol == null ? new Sol(null, null) : sol;
        cola = cola == null ? new Cola(10_000L, 6, 30) : cola;
        urlVerificacion = urlVerificacion == null || urlVerificacion.isBlank()
                ? "https://e-consulta.sunat.gob.pe/ol-ti-itconsvalicpe/ConsValiCpe.htm"
                : urlVerificacion;
        sedePorDefecto = sedePorDefecto == null || sedePorDefecto.isBlank() ? "PRINCIPAL" : sedePorDefecto;
    }

    public record Certificado(String ruta, String clave, String alias, boolean generarDePrueba) {}

    public record Sol(String usuario, String clave) {}

    public record Cola(long intervaloMs, int intentosMaximos, int retrocesoBaseSegundos) {}
}
