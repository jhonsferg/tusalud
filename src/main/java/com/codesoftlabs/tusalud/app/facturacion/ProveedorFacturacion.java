package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import java.util.List;

public interface ProveedorFacturacion {

    record RespuestaEnvio(
            boolean aceptado,
            boolean transitorio,
            String codigo,
            String descripcion,
            byte[] cdrZip,
            List<String> observaciones) {

        public static RespuestaEnvio transitorio(String descripcion) {
            return new RespuestaEnvio(false, true, null, descripcion, null, List.of());
        }

        public static RespuestaEnvio rechazo(String codigo, String descripcion) {
            return new RespuestaEnvio(false, false, codigo, descripcion, null, List.of());
        }
    }

    record RespuestaTicket(String estado, String codigo, String descripcion, byte[] cdrZip) {

        public static final String PENDIENTE = "PENDIENTE";
        public static final String ACEPTADO = "ACEPTADO";
        public static final String RECHAZADO = "RECHAZADO";
    }

    boolean atiende(ProveedorFiscal proveedor);

    RespuestaEnvio enviarComprobante(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion);

    String enviarResumen(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion);

    RespuestaTicket consultarTicket(String ticket, ConfiguracionFiscal configuracion);
}
