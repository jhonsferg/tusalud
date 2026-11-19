package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class ProveedorSunatSimulado implements ProveedorFacturacion {

    private static final int DIAS_MAXIMOS_EMISION = 7;

    private final XmlFirmador firmador;

    public ProveedorSunatSimulado(XmlFirmador firmador) {
        this.firmador = firmador;
    }

    @Override
    public boolean atiende(ProveedorFiscal proveedor) {
        return proveedor == ProveedorFiscal.SIMULACION;
    }

    @Override
    public RespuestaEnvio enviarComprobante(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion) {
        Map<String, byte[]> archivos;
        try {
            archivos = PaqueteSunat.descomprimir(zip);
        } catch (IllegalArgumentException ex) {
            return RespuestaEnvio.rechazo("0151", "El archivo zip no es válido");
        }
        if (archivos.size() != 1) {
            return RespuestaEnvio.rechazo("0152", "El zip debe contener exactamente un archivo XML");
        }
        String nombreXml = archivos.keySet().iterator().next();
        byte[] xml = archivos.values().iterator().next();
        String nombreBase = nombreZip.endsWith(".zip") ? nombreZip.substring(0, nombreZip.length() - 4) : nombreZip;
        if (!nombreXml.equals(nombreBase + ".xml")) {
            return RespuestaEnvio.rechazo("0154", "El nombre del XML no coincide con el del zip");
        }
        Document doc;
        try {
            doc = XmlFirmador.analizar(xml);
        } catch (Exception ex) {
            return RespuestaEnvio.rechazo("0100", "El XML no está bien formado");
        }
        if (!firmador.verificar(xml)) {
            return RespuestaEnvio.rechazo("0306", "No se pudo verificar la firma digital del comprobante");
        }
        String id = texto(doc, "ID");
        String rucEmisor = rucEmisor(doc);
        if (rucEmisor == null || !nombreBase.startsWith(rucEmisor + "-")) {
            return RespuestaEnvio.rechazo("0151", "El RUC del emisor no coincide con el nombre del archivo");
        }
        if (!rucEmisor.equals(configuracion.getRuc())) {
            return RespuestaEnvio.rechazo("0111", "El RUC del emisor no corresponde al contribuyente");
        }
        RespuestaEnvio rechazo = validarReglas(doc);
        if (rechazo != null) {
            return rechazo;
        }
        List<String> observaciones = observaciones(doc);
        String descripcion = "El comprobante numero " + id + ", ha sido aceptado";
        byte[] cdr = PaqueteSunat.construirCdr(id, rucEmisor, "0", descripcion, observaciones);
        return new RespuestaEnvio(
                true, false, "0", descripcion, PaqueteSunat.comprimir("R-" + nombreBase + ".xml", cdr), observaciones);
    }

    @Override
    public String enviarResumen(String nombreZip, byte[] zip, ConfiguracionFiscal configuracion) {
        Map<String, byte[]> archivos = PaqueteSunat.descomprimir(zip);
        byte[] xml = archivos.values().iterator().next();
        String nombreBase = nombreZip.substring(0, nombreZip.length() - 4);
        boolean valido = firmador.verificar(xml);
        return (valido ? "SIM-OK-" : "SIM-KO-") + nombreBase;
    }

    @Override
    public RespuestaTicket consultarTicket(String ticket, ConfiguracionFiscal configuracion) {
        if (ticket == null || !ticket.startsWith("SIM-")) {
            return new RespuestaTicket(RespuestaTicket.RECHAZADO, "0200", "El ticket no existe", null);
        }
        String nombreBase = ticket.substring(7);
        String identificador = nombreBase.substring(nombreBase.indexOf('-') + 1);
        if (ticket.startsWith("SIM-KO-")) {
            return new RespuestaTicket(
                    RespuestaTicket.RECHAZADO, "0306", "No se pudo verificar la firma digital del documento", null);
        }
        String descripcion = "El documento " + identificador + " ha sido aceptado";
        byte[] cdr = PaqueteSunat.construirCdr(identificador, configuracion.getRuc(), "0", descripcion, List.of());
        return new RespuestaTicket(
                RespuestaTicket.ACEPTADO, "0", descripcion, PaqueteSunat.comprimir("R-" + nombreBase + ".xml", cdr));
    }

    private RespuestaEnvio validarReglas(Document doc) {
        String raiz = doc.getDocumentElement().getLocalName();
        BigDecimal pagable = monto(doc, "PayableAmount");
        BigDecimal lineas = BigDecimal.ZERO;
        NodeList nodos = doc.getElementsByTagNameNS(UblGenerador.NS_CBC, "LineExtensionAmount");
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i).getParentNode() != null
                    && nodos.item(i).getParentNode().getLocalName().endsWith("Line")) {
                lineas =
                        lineas.add(new BigDecimal(nodos.item(i).getTextContent().trim()));
            }
        }
        BigDecimal igv = monto(doc, "TaxAmount");
        if (pagable == null || pagable.signum() <= 0) {
            return RespuestaEnvio.rechazo("2639", "El importe total del comprobante debe ser mayor a cero");
        }
        if (igv != null && lineas.add(igv).subtract(pagable).abs().compareTo(new BigDecimal("0.02")) > 0) {
            return RespuestaEnvio.rechazo("3103", "El importe total no coincide con la suma de las líneas más el IGV");
        }
        String fecha = texto(doc, "IssueDate");
        if (fecha != null) {
            long dias = ChronoUnit.DAYS.between(LocalDate.parse(fecha), LocalDate.now());
            if (dias > DIAS_MAXIMOS_EMISION) {
                return RespuestaEnvio.rechazo("2329", "La fecha de emisión está fuera del límite permitido");
            }
            if (dias < 0) {
                return RespuestaEnvio.rechazo("2329", "La fecha de emisión no puede ser futura");
            }
        }
        String tipo = texto(doc, "InvoiceTypeCode");
        Element receptor = (Element) doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "AccountingCustomerParty")
                .item(0);
        if (receptor != null) {
            Element id = (Element)
                    receptor.getElementsByTagNameNS(UblGenerador.NS_CBC, "ID").item(0);
            String esquema = id.getAttribute("schemeID");
            String numero = id.getTextContent().trim();
            if ("01".equals(tipo) && (!"6".equals(esquema) || !ValidadorDocumentos.rucValido(numero))) {
                return RespuestaEnvio.rechazo("2017", "El número de RUC del adquirente no es válido");
            }
            if ("1".equals(esquema) && !ValidadorDocumentos.dniValido(numero)) {
                return RespuestaEnvio.rechazo("2018", "El número de DNI del adquirente no es válido");
            }
        }
        if (raiz.equals("CreditNote") || raiz.equals("DebitNote")) {
            if (doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "BillingReference")
                            .getLength()
                    == 0) {
                return RespuestaEnvio.rechazo("2072", "La nota no referencia el comprobante que modifica");
            }
        }
        return null;
    }

    private List<String> observaciones(Document doc) {
        List<String> observaciones = new ArrayList<>();
        String tipo = texto(doc, "InvoiceTypeCode");
        Element receptor = (Element) doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "AccountingCustomerParty")
                .item(0);
        if ("01".equals(tipo)
                && receptor != null
                && receptor.getElementsByTagNameNS(UblGenerador.NS_CAC, "RegistrationAddress")
                                .getLength()
                        == 0) {
            observaciones.add("4093 - El adquirente no tiene dirección consignada");
        }
        return observaciones;
    }

    private static String rucEmisor(Document doc) {
        Element proveedor = (Element) doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "AccountingSupplierParty")
                .item(0);
        if (proveedor == null) {
            return null;
        }
        NodeList ids = proveedor.getElementsByTagNameNS(UblGenerador.NS_CBC, "ID");
        if (ids.getLength() > 0) {
            return ids.item(0).getTextContent().trim();
        }
        NodeList asignado = proveedor.getElementsByTagNameNS(UblGenerador.NS_CBC, "CustomerAssignedAccountID");
        return asignado.getLength() == 0
                ? null
                : asignado.item(0).getTextContent().trim();
    }

    private static String texto(Document doc, String nombre) {
        NodeList nodos = doc.getElementsByTagNameNS(UblGenerador.NS_CBC, nombre);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().trim();
    }

    private static BigDecimal monto(Document doc, String nombre) {
        String texto = texto(doc, nombre);
        return texto == null ? null : new BigDecimal(texto);
    }
}
