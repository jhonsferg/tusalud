package com.codesoftlabs.tusalud.app.facturacion;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronicoItem;
import com.codesoftlabs.tusalud.app.entities.ResumenSunat;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class UblGenerador {

    public static final String NS_CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    public static final String NS_CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    public static final String NS_EXT = XmlFirmador.NS_EXT;
    public static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String NS_SAC = "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1";
    public static final String NS_INVOICE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    public static final String NS_CREDIT = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    public static final String NS_DEBIT = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";
    public static final String NS_RESUMEN = "urn:sunat:names:specification:ubl:peru:schema:xsd:SummaryDocuments-1";
    public static final String NS_BAJA = "urn:sunat:names:specification:ubl:peru:schema:xsd:VoidedDocuments-1";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String idSunat(DocumentoElectronico d) {
        return d.getSerie() + "-" + d.getNumero();
    }

    public byte[] comprobante(DocumentoElectronico d, ConfiguracionFiscal c) {
        try {
            String raiz = switch (d.getTipo()) {
                case NOTA_CREDITO -> "CreditNote";
                case NOTA_DEBITO -> "DebitNote";
                default -> "Invoice";
            };
            String espacio = switch (d.getTipo()) {
                case NOTA_CREDITO -> NS_CREDIT;
                case NOTA_DEBITO -> NS_DEBIT;
                default -> NS_INVOICE;
            };
            Document doc = nuevo(espacio, raiz);
            Element r = doc.getDocumentElement();
            cabeceraComun(doc, r, c, idSunat(d));
            texto(doc, r, "cbc", "ID", idSunat(d));
            texto(doc, r, "cbc", "IssueDate", d.getEmitidoEn().toLocalDate().format(FECHA));
            texto(doc, r, "cbc", "IssueTime", d.getEmitidoEn().toLocalTime().format(HORA));
            if (!d.getTipo().esNota()) {
                Element tipo =
                        texto(doc, r, "cbc", "InvoiceTypeCode", d.getTipo().codigo());
                tipo.setAttribute("listID", "0101");
            }
            Element leyenda = texto(doc, r, "cbc", "Note", NumeroALetras.soles(d.getTotal()));
            leyenda.setAttribute("languageLocaleID", "1000");
            moneda(doc, r, "DocumentCurrencyCode", d.getMoneda());
            texto(doc, r, "cbc", "LineCountNumeric", String.valueOf(d.getItems().size()));
            if (d.getTipo().esNota()) {
                lineasNota(doc, r, d);
                referenciaNota(doc, r, d);
            }
            firmaReferencia(doc, r, c);
            emisor(doc, r, c);
            receptor(doc, r, d);
            totalesImpuestos(doc, r, d);
            totalMonetario(doc, r, d);
            int posicion = 1;
            for (DocumentoElectronicoItem item : d.getItems()) {
                linea(doc, r, d, item, posicion++);
            }
            return XmlFirmador.serializar(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el XML del comprobante", ex);
        }
    }

    public byte[] resumenDiario(ResumenSunat resumen, List<DocumentoElectronico> documentos, ConfiguracionFiscal c) {
        try {
            Document doc = nuevo(NS_RESUMEN, "SummaryDocuments");
            doc.getDocumentElement().setAttribute("xmlns:sac", NS_SAC);
            Element r = doc.getDocumentElement();
            extensiones(doc, r);
            texto(doc, r, "cbc", "UBLVersionID", "2.0");
            texto(doc, r, "cbc", "CustomizationID", "1.1");
            texto(doc, r, "cbc", "ID", resumen.getIdentificador());
            texto(doc, r, "cbc", "ReferenceDate", resumen.getFechaReferencia().format(FECHA));
            texto(doc, r, "cbc", "IssueDate", resumen.getFechaGeneracion().format(FECHA));
            firmaReferencia(doc, r, c);
            emisorResumen(doc, r, c);
            int linea = 1;
            for (DocumentoElectronico d : documentos) {
                Element l = elemento(doc, r, "sac", "SummaryDocumentsLine");
                texto(doc, l, "cbc", "LineID", String.valueOf(linea++));
                texto(doc, l, "cbc", "DocumentTypeCode", d.getTipo().codigo());
                texto(doc, l, "cbc", "ID", idSunat(d));
                Element cliente = elemento(doc, l, "cac", "AccountingCustomerParty");
                texto(
                        doc,
                        cliente,
                        "cbc",
                        "CustomerAssignedAccountID",
                        d.getClienteDoc() == null ? "-" : d.getClienteDoc());
                texto(doc, cliente, "cbc", "AdditionalAccountID", codigoDocumento(d.getClienteTipoDoc()));
                Element estado = elemento(doc, l, "cac", "Status");
                boolean baja =
                        d.getBajaResumen() != null && d.getBajaResumen().getId().equals(resumen.getId());
                texto(doc, estado, "cbc", "ConditionCode", baja ? "3" : "1");
                monto(doc, l, "sac", "TotalAmount", d.getTotal(), d.getMoneda());
                pagoResumen(doc, l, d);
                if (d.getIgv().signum() > 0) {
                    impuestoResumen(doc, l, "1000", "IGV", "VAT", d.getIgv(), d.getMoneda());
                }
            }
            return XmlFirmador.serializar(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el resumen diario", ex);
        }
    }

    public byte[] comunicacionBaja(ResumenSunat resumen, List<DocumentoElectronico> documentos, ConfiguracionFiscal c) {
        try {
            Document doc = nuevo(NS_BAJA, "VoidedDocuments");
            doc.getDocumentElement().setAttribute("xmlns:sac", NS_SAC);
            Element r = doc.getDocumentElement();
            extensiones(doc, r);
            texto(doc, r, "cbc", "UBLVersionID", "2.0");
            texto(doc, r, "cbc", "CustomizationID", "1.0");
            texto(doc, r, "cbc", "ID", resumen.getIdentificador());
            texto(doc, r, "cbc", "ReferenceDate", resumen.getFechaReferencia().format(FECHA));
            texto(doc, r, "cbc", "IssueDate", resumen.getFechaGeneracion().format(FECHA));
            firmaReferencia(doc, r, c);
            emisorResumen(doc, r, c);
            int linea = 1;
            for (DocumentoElectronico d : documentos) {
                Element l = elemento(doc, r, "sac", "VoidedDocumentsLine");
                texto(doc, l, "cbc", "LineID", String.valueOf(linea++));
                texto(doc, l, "cbc", "DocumentTypeCode", d.getTipo().codigo());
                texto(doc, l, "sac", "DocumentSerialID", d.getSerie());
                texto(doc, l, "sac", "DocumentNumberID", String.valueOf(d.getNumero()));
                texto(
                        doc,
                        l,
                        "sac",
                        "VoidReasonDescription",
                        d.getMotivoBaja() == null ? "ANULACION" : d.getMotivoBaja());
            }
            return XmlFirmador.serializar(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar la comunicación de baja", ex);
        }
    }

    private Document nuevo(String espacio, String raiz) throws Exception {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        Document doc = fabrica.newDocumentBuilder().newDocument();
        Element r = doc.createElementNS(espacio, raiz);
        r.setAttribute("xmlns:cac", NS_CAC);
        r.setAttribute("xmlns:cbc", NS_CBC);
        r.setAttribute("xmlns:ds", NS_DS);
        r.setAttribute("xmlns:ext", NS_EXT);
        doc.appendChild(r);
        return doc;
    }

    private void cabeceraComun(Document doc, Element r, ConfiguracionFiscal c, String id) {
        extensiones(doc, r);
        texto(doc, r, "cbc", "UBLVersionID", "2.1");
        texto(doc, r, "cbc", "CustomizationID", "2.0");
    }

    private void extensiones(Document doc, Element r) {
        Element extensiones = elemento(doc, r, "ext", "UBLExtensions");
        Element extension = elemento(doc, extensiones, "ext", "UBLExtension");
        elemento(doc, extension, "ext", "ExtensionContent");
    }

    private void moneda(Document doc, Element padre, String nombre, String moneda) {
        texto(doc, padre, "cbc", nombre, moneda);
    }

    private void firmaReferencia(Document doc, Element r, ConfiguracionFiscal c) {
        Element firma = elemento(doc, r, "cac", "Signature");
        texto(doc, firma, "cbc", "ID", c.getRuc());
        Element signataria = elemento(doc, firma, "cac", "SignatoryParty");
        Element identificacion = elemento(doc, signataria, "cac", "PartyIdentification");
        texto(doc, identificacion, "cbc", "ID", c.getRuc());
        Element nombre = elemento(doc, signataria, "cac", "PartyName");
        texto(doc, nombre, "cbc", "Name", c.getRazonSocial());
        Element adjunto = elemento(doc, firma, "cac", "DigitalSignatureAttachment");
        Element externa = elemento(doc, adjunto, "cac", "ExternalReference");
        texto(doc, externa, "cbc", "URI", "#" + XmlFirmador.ID_FIRMA);
    }

    private void emisor(Document doc, Element r, ConfiguracionFiscal c) {
        Element proveedor = elemento(doc, r, "cac", "AccountingSupplierParty");
        Element party = elemento(doc, proveedor, "cac", "Party");
        Element identificacion = elemento(doc, party, "cac", "PartyIdentification");
        Element id = texto(doc, identificacion, "cbc", "ID", c.getRuc());
        id.setAttribute("schemeID", "6");
        Element nombreComercial = elemento(doc, party, "cac", "PartyName");
        texto(
                doc,
                nombreComercial,
                "cbc",
                "Name",
                c.getNombreComercial() == null ? c.getRazonSocial() : c.getNombreComercial());
        Element legal = elemento(doc, party, "cac", "PartyLegalEntity");
        texto(doc, legal, "cbc", "RegistrationName", c.getRazonSocial());
        Element direccion = elemento(doc, legal, "cac", "RegistrationAddress");
        texto(doc, direccion, "cbc", "ID", c.getUbigeo());
        texto(doc, direccion, "cbc", "AddressTypeCode", c.getCodigoLocal());
        texto(doc, direccion, "cbc", "CityName", c.getProvincia());
        texto(doc, direccion, "cbc", "CountrySubentity", c.getDepartamento());
        texto(doc, direccion, "cbc", "District", c.getDistrito());
        Element linea = elemento(doc, direccion, "cac", "AddressLine");
        texto(doc, linea, "cbc", "Line", c.getDireccion());
        Element pais = elemento(doc, direccion, "cac", "Country");
        texto(doc, pais, "cbc", "IdentificationCode", "PE");
    }

    private void emisorResumen(Document doc, Element r, ConfiguracionFiscal c) {
        Element proveedor = elemento(doc, r, "cac", "AccountingSupplierParty");
        texto(doc, proveedor, "cbc", "CustomerAssignedAccountID", c.getRuc());
        texto(doc, proveedor, "cbc", "AdditionalAccountID", "6");
        Element party = elemento(doc, proveedor, "cac", "Party");
        Element legal = elemento(doc, party, "cac", "PartyLegalEntity");
        texto(doc, legal, "cbc", "RegistrationName", c.getRazonSocial());
    }

    private void receptor(Document doc, Element r, DocumentoElectronico d) {
        Element cliente = elemento(doc, r, "cac", "AccountingCustomerParty");
        Element party = elemento(doc, cliente, "cac", "Party");
        Element identificacion = elemento(doc, party, "cac", "PartyIdentification");
        Element id = texto(doc, identificacion, "cbc", "ID", d.getClienteDoc() == null ? "-" : d.getClienteDoc());
        id.setAttribute("schemeID", codigoDocumento(d.getClienteTipoDoc()));
        Element legal = elemento(doc, party, "cac", "PartyLegalEntity");
        texto(doc, legal, "cbc", "RegistrationName", d.getClienteNombre());
        if (d.getClienteDireccion() != null && !d.getClienteDireccion().isBlank()) {
            Element direccion = elemento(doc, legal, "cac", "RegistrationAddress");
            Element linea = elemento(doc, direccion, "cac", "AddressLine");
            texto(doc, linea, "cbc", "Line", d.getClienteDireccion());
        }
    }

    private static String codigoDocumento(TipoDocumentoCliente tipo) {
        return switch (tipo) {
            case RUC -> "6";
            case DNI -> "1";
            case SIN_DOC -> "0";
        };
    }

    private void lineasNota(Document doc, Element r, DocumentoElectronico d) {
        Element respuesta = elemento(doc, r, "cac", "DiscrepancyResponse");
        texto(doc, respuesta, "cbc", "ReferenceID", idSunat(d.getDocumentoReferencia()));
        texto(doc, respuesta, "cbc", "ResponseCode", d.getMotivoCodigo());
        texto(doc, respuesta, "cbc", "Description", d.getMotivoDescripcion());
    }

    private void referenciaNota(Document doc, Element r, DocumentoElectronico d) {
        Element referencia = elemento(doc, r, "cac", "BillingReference");
        Element factura = elemento(doc, referencia, "cac", "InvoiceDocumentReference");
        texto(doc, factura, "cbc", "ID", idSunat(d.getDocumentoReferencia()));
        texto(
                doc,
                factura,
                "cbc",
                "DocumentTypeCode",
                d.getDocumentoReferencia().getTipo().codigo());
    }

    private void totalesImpuestos(Document doc, Element r, DocumentoElectronico d) {
        Element total = elemento(doc, r, "cac", "TaxTotal");
        monto(doc, total, "cbc", "TaxAmount", d.getIgv(), d.getMoneda());
        Map<Afectacion, BigDecimal> bases = new EnumMap<>(Afectacion.class);
        if (d.getOpGravada().signum() > 0
                || (d.getOpExonerada().signum() == 0 && d.getOpInafecta().signum() == 0)) {
            bases.put(Afectacion.GRAVADO, d.getOpGravada());
        }
        if (d.getOpExonerada().signum() > 0) {
            bases.put(Afectacion.EXONERADO, d.getOpExonerada());
        }
        if (d.getOpInafecta().signum() > 0) {
            bases.put(Afectacion.INAFECTO, d.getOpInafecta());
        }
        bases.forEach((afectacion, base) -> {
            Element sub = elemento(doc, total, "cac", "TaxSubtotal");
            monto(doc, sub, "cbc", "TaxableAmount", base, d.getMoneda());
            monto(
                    doc,
                    sub,
                    "cbc",
                    "TaxAmount",
                    afectacion == Afectacion.GRAVADO ? d.getIgv() : BigDecimal.ZERO,
                    d.getMoneda());
            Element categoria = elemento(doc, sub, "cac", "TaxCategory");
            Element esquema = elemento(doc, categoria, "cac", "TaxScheme");
            texto(doc, esquema, "cbc", "ID", afectacion.tributo());
            texto(doc, esquema, "cbc", "Name", afectacion.nombre());
            texto(doc, esquema, "cbc", "TaxTypeCode", afectacion.internacional());
        });
    }

    private void totalMonetario(Document doc, Element r, DocumentoElectronico d) {
        String nombre = d.getTipo() == TipoCpe.NOTA_DEBITO ? "RequestedMonetaryTotal" : "LegalMonetaryTotal";
        Element total = elemento(doc, r, "cac", nombre);
        BigDecimal valorVenta = d.getOpGravada().add(d.getOpExonerada()).add(d.getOpInafecta());
        monto(doc, total, "cbc", "LineExtensionAmount", valorVenta, d.getMoneda());
        if (d.getTipo() != TipoCpe.NOTA_DEBITO) {
            monto(doc, total, "cbc", "TaxInclusiveAmount", d.getTotal(), d.getMoneda());
        }
        monto(doc, total, "cbc", "PayableAmount", d.getTotal(), d.getMoneda());
    }

    private void linea(Document doc, Element r, DocumentoElectronico d, DocumentoElectronicoItem item, int posicion) {
        String nombreLinea = switch (d.getTipo()) {
            case NOTA_CREDITO -> "CreditNoteLine";
            case NOTA_DEBITO -> "DebitNoteLine";
            default -> "InvoiceLine";
        };
        String nombreCantidad = switch (d.getTipo()) {
            case NOTA_CREDITO -> "CreditedQuantity";
            case NOTA_DEBITO -> "DebitedQuantity";
            default -> "InvoicedQuantity";
        };
        Element linea = elemento(doc, r, "cac", nombreLinea);
        texto(doc, linea, "cbc", "ID", String.valueOf(posicion));
        Element cantidad = texto(doc, linea, "cbc", nombreCantidad, decimal(item.getCantidad(), 2, 3));
        cantidad.setAttribute("unitCode", item.getUnidad());
        monto(doc, linea, "cbc", "LineExtensionAmount", item.getValorVenta(), d.getMoneda());
        Element referenciaPrecio = elemento(doc, linea, "cac", "PricingReference");
        Element condicion = elemento(doc, referenciaPrecio, "cac", "AlternativeConditionPrice");
        monto(doc, condicion, "cbc", "PriceAmount", item.getPrecioUnitario(), d.getMoneda(), 2, 6);
        texto(doc, condicion, "cbc", "PriceTypeCode", "01");
        Element impuestos = elemento(doc, linea, "cac", "TaxTotal");
        monto(doc, impuestos, "cbc", "TaxAmount", item.getIgv(), d.getMoneda());
        Element sub = elemento(doc, impuestos, "cac", "TaxSubtotal");
        monto(doc, sub, "cbc", "TaxableAmount", item.getValorVenta(), d.getMoneda());
        monto(doc, sub, "cbc", "TaxAmount", item.getIgv(), d.getMoneda());
        Element categoria = elemento(doc, sub, "cac", "TaxCategory");
        if (item.getAfectacion() == Afectacion.GRAVADO) {
            texto(doc, categoria, "cbc", "Percent", "18.00");
        }
        texto(
                doc,
                categoria,
                "cbc",
                "TaxExemptionReasonCode",
                item.getAfectacion().codigo());
        Element esquema = elemento(doc, categoria, "cac", "TaxScheme");
        texto(doc, esquema, "cbc", "ID", item.getAfectacion().tributo());
        texto(doc, esquema, "cbc", "Name", item.getAfectacion().nombre());
        texto(doc, esquema, "cbc", "TaxTypeCode", item.getAfectacion().internacional());
        Element producto = elemento(doc, linea, "cac", "Item");
        texto(doc, producto, "cbc", "Description", item.getDescripcion());
        if (item.getCodigo() != null && !item.getCodigo().isBlank()) {
            Element vendedor = elemento(doc, producto, "cac", "SellersItemIdentification");
            texto(doc, vendedor, "cbc", "ID", item.getCodigo());
        }
        Element precio = elemento(doc, linea, "cac", "Price");
        monto(doc, precio, "cbc", "PriceAmount", item.getValorUnitario(), d.getMoneda(), 2, 6);
    }

    private void pagoResumen(Document doc, Element linea, DocumentoElectronico d) {
        Element pago = elemento(doc, linea, "sac", "BillingPayment");
        monto(doc, pago, "cbc", "PaidAmount", pagable(d), d.getMoneda());
        texto(doc, pago, "cbc", "InstructionID", codigoPago(d));
    }

    private static BigDecimal pagable(DocumentoElectronico d) {
        return d.getOpGravada();
    }

    private static String codigoPago(DocumentoElectronico d) {
        if (d.getOpGravada().signum() > 0) {
            return "01";
        }
        return d.getOpExonerada().signum() > 0 ? "02" : "03";
    }

    private void impuestoResumen(
            Document doc, Element linea, String id, String nombre, String codigo, BigDecimal monto, String moneda) {
        Element total = elemento(doc, linea, "cac", "TaxTotal");
        monto(doc, total, "cbc", "TaxAmount", monto, moneda);
        Element sub = elemento(doc, total, "cac", "TaxSubtotal");
        monto(doc, sub, "cbc", "TaxAmount", monto, moneda);
        Element categoria = elemento(doc, sub, "cac", "TaxCategory");
        Element esquema = elemento(doc, categoria, "cac", "TaxScheme");
        texto(doc, esquema, "cbc", "ID", id);
        texto(doc, esquema, "cbc", "Name", nombre);
        texto(doc, esquema, "cbc", "TaxTypeCode", codigo);
    }

    private static Element elemento(Document doc, Element padre, String prefijo, String nombre) {
        Element e = doc.createElementNS(espacio(prefijo), prefijo + ":" + nombre);
        padre.appendChild(e);
        return e;
    }

    private static Element texto(Document doc, Element padre, String prefijo, String nombre, String valor) {
        Element e = elemento(doc, padre, prefijo, nombre);
        e.setTextContent(valor);
        return e;
    }

    private static Element monto(
            Document doc, Element padre, String prefijo, String nombre, BigDecimal valor, String moneda) {
        return monto(doc, padre, prefijo, nombre, valor, moneda, 2, 2);
    }

    private static Element monto(
            Document doc,
            Element padre,
            String prefijo,
            String nombre,
            BigDecimal valor,
            String moneda,
            int minimo,
            int maximo) {
        Element e = texto(doc, padre, prefijo, nombre, decimal(valor, minimo, maximo));
        e.setAttribute("currencyID", moneda);
        return e;
    }

    private static String decimal(BigDecimal valor, int minimo, int maximo) {
        BigDecimal redondeado = valor.setScale(maximo, RoundingMode.HALF_UP).stripTrailingZeros();
        if (redondeado.scale() < minimo) {
            redondeado = redondeado.setScale(minimo, RoundingMode.UNNECESSARY);
        }
        return redondeado.toPlainString();
    }

    private static String espacio(String prefijo) {
        return switch (prefijo) {
            case "cac" -> NS_CAC;
            case "cbc" -> NS_CBC;
            case "ext" -> NS_EXT;
            case "sac" -> NS_SAC;
            default -> throw new IllegalArgumentException("Prefijo desconocido " + prefijo);
        };
    }
}
