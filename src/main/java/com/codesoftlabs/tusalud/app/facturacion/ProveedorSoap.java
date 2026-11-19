package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Component
public class ProveedorSoap implements ProveedorFacturacion {

    private static final String URL_BETA = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";
    private static final String URL_PRODUCCION = "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService";

    private final FacturacionProperties propiedades;
    private final HttpClient cliente =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public ProveedorSoap(FacturacionProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    public boolean atiende(ProveedorFiscal proveedor) {
        return proveedor == ProveedorFiscal.SUNAT
                || proveedor == ProveedorFiscal.OSE
                || proveedor == ProveedorFiscal.PSE;
    }

    @Override
    public RespuestaEnvio enviarComprobante(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion) {
        String cuerpo = "<ser:sendBill><fileName>" + nombreZip + "</fileName><contentFile>"
                + Base64.getEncoder().encodeToString(zip) + "</contentFile></ser:sendBill>";
        Respuesta respuesta;
        try {
            respuesta = invocar(cuerpo, configuracion);
        } catch (IOException ex) {
            return RespuestaEnvio.transitorio("No se pudo contactar el servicio: " + ex.getMessage());
        }
        if (respuesta.falla() != null) {
            return respuesta.transitoria()
                    ? RespuestaEnvio.transitorio(respuesta.falla())
                    : RespuestaEnvio.rechazo(respuesta.codigoFalla(), respuesta.falla());
        }
        String contenido = respuesta.valor("applicationResponse");
        if (contenido == null) {
            return RespuestaEnvio.transitorio("La respuesta no incluye la constancia de recepción");
        }
        byte[] cdr = Base64.getDecoder().decode(contenido);
        PaqueteSunat.Cdr leido = PaqueteSunat.leerCdr(cdr);
        return new RespuestaEnvio(
                leido.aceptado(), false, leido.codigo(), leido.descripcion(), cdr, leido.observaciones());
    }

    @Override
    public String enviarResumen(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion) {
        String cuerpo = "<ser:sendSummary><fileName>" + nombreZip + "</fileName><contentFile>"
                + Base64.getEncoder().encodeToString(zip) + "</contentFile></ser:sendSummary>";
        try {
            Respuesta respuesta = invocar(cuerpo, configuracion);
            if (respuesta.falla() != null) {
                throw new IllegalStateException(respuesta.falla());
            }
            return respuesta.valor("ticket");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo contactar el servicio: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RespuestaTicket consultarTicket(String ticket, ConfiguracionFiscal configuracion) {
        String cuerpo = "<ser:getStatus><ticket>" + ticket + "</ticket></ser:getStatus>";
        try {
            Respuesta respuesta = invocar(cuerpo, configuracion);
            if (respuesta.falla() != null) {
                return new RespuestaTicket(RespuestaTicket.RECHAZADO, respuesta.codigoFalla(), respuesta.falla(), null);
            }
            String estado = respuesta.valor("statusCode");
            if ("98".equals(estado)) {
                return new RespuestaTicket(RespuestaTicket.PENDIENTE, estado, "En proceso", null);
            }
            String contenido = respuesta.valor("content");
            byte[] cdr = contenido == null ? null : Base64.getDecoder().decode(contenido);
            if (cdr == null) {
                return new RespuestaTicket(RespuestaTicket.RECHAZADO, estado, "Sin constancia de recepción", null);
            }
            PaqueteSunat.Cdr leido = PaqueteSunat.leerCdr(cdr);
            return new RespuestaTicket(
                    "0".equals(estado) && leido.aceptado() ? RespuestaTicket.ACEPTADO : RespuestaTicket.RECHAZADO,
                    leido.codigo(),
                    leido.descripcion(),
                    cdr);
        } catch (IOException ex) {
            return new RespuestaTicket(RespuestaTicket.PENDIENTE, null, "No se pudo contactar el servicio", null);
        }
    }

    private Respuesta invocar(String cuerpo, ConfiguracionFiscal configuracion) throws IOException {
        FacturacionProperties.Sol sol = propiedades.sol();
        if (sol.usuario() == null || sol.usuario().isBlank() || sol.clave() == null) {
            throw new IOException("Faltan las credenciales SOL en la configuración del servidor");
        }
        String url = configuracion.getUrlServicio() != null
                        && !configuracion.getUrlServicio().isBlank()
                ? configuracion.getUrlServicio()
                : configuracion.getModo() == ModoFiscal.PRODUCCION ? URL_PRODUCCION : URL_BETA;
        String sobre = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:ser=\"http://service.sunat.gob.pe\""
                + " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">"
                + "<soapenv:Header><wsse:Security><wsse:UsernameToken><wsse:Username>"
                + escapar(configuracion.getRuc() + sol.usuario())
                + "</wsse:Username><wsse:Password>" + escapar(sol.clave())
                + "</wsse:Password></wsse:UsernameToken></wsse:Security></soapenv:Header><soapenv:Body>"
                + cuerpo + "</soapenv:Body></soapenv:Envelope>";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<byte[]> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.ofByteArray());
            return interpretar(respuesta.statusCode(), respuesta.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Operación interrumpida", ex);
        }
    }

    private Respuesta interpretar(int estado, byte[] cuerpo) {
        try {
            Document doc = XmlFirmador.analizar(cuerpo);
            NodeList falla = doc.getElementsByTagName("faultstring");
            if (falla.getLength() > 0) {
                String codigo = doc.getElementsByTagName("faultcode").item(0).getTextContent();
                String limpio = codigo.contains(".") ? codigo.substring(codigo.lastIndexOf('.') + 1) : codigo;
                boolean transitoria = estado >= 500 && !limpio.matches("\\d{4}");
                return new Respuesta(doc, falla.item(0).getTextContent(), limpio, transitoria);
            }
            return new Respuesta(doc, null, null, false);
        } catch (Exception ex) {
            return new Respuesta(
                    null, "La respuesta del servicio no se pudo interpretar (HTTP " + estado + ")", null, true);
        }
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record Respuesta(Document documento, String falla, String codigoFalla, boolean transitoria) {

        String valor(String nombre) {
            if (documento == null) {
                return null;
            }
            NodeList nodos = documento.getElementsByTagName(nombre);
            return nodos.getLength() == 0
                    ? null
                    : nodos.item(0).getTextContent().trim();
        }
    }
}
