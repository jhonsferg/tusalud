package com.codesoftlabs.tusalud.app.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.DocumentoElectronicoItem;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.facturacion.ProveedorFacturacion.RespuestaEnvio;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class FacturacionUnidadTest {

    private static final String RUC_EMISOR = "20131312955";

    private CertificadoDigital certificado;
    private XmlFirmador firmador;
    private UblGenerador generador;
    private ProveedorSunatSimulado proveedor;
    private ConfiguracionFiscal configuracion;

    @BeforeEach
    void preparar() {
        FacturacionProperties propiedades = new FacturacionProperties(null, null, null, null, null);
        certificado = new CertificadoDigital(propiedades);
        firmador = new XmlFirmador();
        generador = new UblGenerador();
        proveedor = new ProveedorSunatSimulado(firmador);
        configuracion = ConfiguracionFiscal.builder()
                .sede("PRINCIPAL")
                .habilitada(true)
                .ruc(RUC_EMISOR)
                .razonSocial("Clínica TuSalud SAC")
                .direccion("Av. Salud 123")
                .ubigeo("150101")
                .departamento("Lima")
                .provincia("Lima")
                .distrito("Lima")
                .proveedor(ProveedorFiscal.SIMULACION)
                .modo(ModoFiscal.BETA)
                .build();
    }

    @Test
    void conviertePesosEnLetras() {
        assertThat(NumeroALetras.soles(new BigDecimal("118.00"))).isEqualTo("CIENTO DIECIOCHO CON 00/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("1000.50"))).isEqualTo("MIL CON 50/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("21.05"))).isEqualTo("VEINTIUNO CON 05/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("2000000.00"))).isEqualTo("DOS MILLONES CON 00/100 SOLES");
        assertThat(NumeroALetras.soles(new BigDecimal("0.99"))).isEqualTo("CERO CON 99/100 SOLES");
    }

    @Test
    void validaRucYDniPorDigitoVerificador() {
        assertThat(ValidadorDocumentos.rucValido("20131312955")).isTrue();
        assertThat(ValidadorDocumentos.rucValido("20100070970")).isTrue();
        assertThat(ValidadorDocumentos.rucValido("20123456789")).isFalse();
        assertThat(ValidadorDocumentos.rucValido("30131312955")).isFalse();
        assertThat(ValidadorDocumentos.rucValido("2013131295")).isFalse();
        assertThat(ValidadorDocumentos.rucValido(null)).isFalse();
        assertThat(ValidadorDocumentos.dniValido("45678912")).isTrue();
        assertThat(ValidadorDocumentos.dniValido("00000000")).isFalse();
        assertThat(ValidadorDocumentos.dniValido("1234567")).isFalse();
    }

    @Test
    void laFirmaSeVerificaYDetectaCualquierAlteracion() {
        DocumentoElectronico factura = factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45");
        byte[] xml = generador.comprobante(factura, configuracion);
        XmlFirmador.XmlFirmado firmado = firmador.firmar(xml, certificado.credencial());

        assertThat(firmado.digest()).isNotBlank();
        assertThat(firmador.verificar(firmado.contenido())).isTrue();
        assertThat(firmador.certificadoDe(firmado.contenido())).isNotNull();

        String alterado = new String(firmado.contenido(), StandardCharsets.UTF_8)
                .replace("<cbc:PayableAmount currencyID=\"PEN\">118.00", "<cbc:PayableAmount currencyID=\"PEN\">1.00");
        assertThat(firmador.verificar(alterado.getBytes(StandardCharsets.UTF_8)))
                .isFalse();
    }

    @Test
    void elXmlDeLaFacturaTieneLaEstructuraUblDeSunat() throws Exception {
        DocumentoElectronico factura = factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45");
        Document doc = XmlFirmador.analizar(generador.comprobante(factura, configuracion));

        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("Invoice");
        assertThat(texto(doc, "ID")).isEqualTo("F001-15");
        assertThat(texto(doc, "UBLVersionID")).isEqualTo("2.1");
        assertThat(texto(doc, "CustomizationID")).isEqualTo("2.0");
        assertThat(texto(doc, "InvoiceTypeCode")).isEqualTo("01");
        assertThat(texto(doc, "DocumentCurrencyCode")).isEqualTo("PEN");
        assertThat(texto(doc, "PayableAmount")).isEqualTo("118.00");
        assertThat(doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "InvoiceLine")
                        .getLength())
                .isEqualTo(1);
        assertThat(doc.getElementsByTagNameNS(UblGenerador.NS_EXT, "ExtensionContent")
                        .getLength())
                .isEqualTo(1);
        NodeList notas = doc.getElementsByTagNameNS(UblGenerador.NS_CBC, "Note");
        assertThat(notas.item(0).getTextContent()).isEqualTo("CIENTO DIECIOCHO CON 00/100 SOLES");
        assertThat(doc.getElementsByTagNameNS(UblGenerador.NS_CBC, "TaxExemptionReasonCode")
                        .item(0)
                        .getTextContent())
                .isEqualTo("10");
    }

    @Test
    void laNotaDeCreditoReferenciaAlComprobanteYUsaSuPropiaEstructura() throws Exception {
        DocumentoElectronico factura = factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45");
        DocumentoElectronico nota = DocumentoElectronico.builder()
                .tipo(TipoCpe.NOTA_CREDITO)
                .serie("FC01")
                .numero(3)
                .sede("PRINCIPAL")
                .emitidoEn(LocalDateTime.now())
                .documentoReferencia(factura)
                .motivoCodigo("01")
                .motivoDescripcion("Anulación de la operación")
                .clienteTipoDoc(TipoDocumentoCliente.RUC)
                .clienteDoc("20100070970")
                .clienteNombre("Distribuidora Andina SAC")
                .estado(EstadoCpe.PENDIENTE)
                .creadoEn(LocalDateTime.now())
                .opGravada(new BigDecimal("100.00"))
                .igv(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .build();
        nota.agregarItem(item("Consulta", "118.00"));
        Document doc = XmlFirmador.analizar(generador.comprobante(nota, configuracion));

        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("CreditNote");
        assertThat(texto(doc, "ResponseCode")).isEqualTo("01");
        assertThat(doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "BillingReference")
                        .getLength())
                .isEqualTo(1);
        assertThat(doc.getElementsByTagNameNS(UblGenerador.NS_CAC, "CreditNoteLine")
                        .getLength())
                .isEqualTo(1);
        assertThat(texto(doc, "CreditedQuantity")).isEqualTo("1.00");
    }

    @Test
    void elProveedorSimuladoAceptaUnComprobanteValidoYDevuelveLaConstanciaDeRecepcion() {
        byte[] zip = paquete(factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45"), 15);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-15.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isTrue();
        assertThat(respuesta.observaciones()).isEmpty();
        PaqueteSunat.Cdr cdr = PaqueteSunat.leerCdr(respuesta.cdrZip());
        assertThat(cdr.aceptado()).isTrue();
        assertThat(cdr.descripcion()).contains("F001-15");
    }

    @Test
    void elProveedorSimuladoRechazaUnRucDeClienteInvalido() {
        byte[] zip = paquete(factura("20123456789", "Cliente Dudoso SAC", "Jr. Falso 1"), 15);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-15.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isFalse();
        assertThat(respuesta.transitorio()).isFalse();
        assertThat(respuesta.codigo()).isEqualTo("2017");
    }

    @Test
    void elProveedorSimuladoRechazaUnXmlAlteradoDespuesDeFirmar() {
        DocumentoElectronico factura = factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45");
        byte[] xml = firmador.firmar(generador.comprobante(factura, configuracion), certificado.credencial())
                .contenido();
        byte[] alterado = new String(xml, StandardCharsets.UTF_8)
                .replace("Distribuidora Andina SAC", "Otro Cliente SAC")
                .getBytes(StandardCharsets.UTF_8);
        byte[] zip = PaqueteSunat.comprimir(RUC_EMISOR + "-01-F001-15.xml", alterado);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-15.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isFalse();
        assertThat(respuesta.codigo()).isEqualTo("0306");
    }

    @Test
    void laFacturaSinDireccionSeAceptaConObservaciones() {
        byte[] zip = paquete(factura("20100070970", "Distribuidora Andina SAC", null), 15);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-15.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isTrue();
        assertThat(respuesta.observaciones()).hasSize(1);
        assertThat(respuesta.observaciones().get(0)).startsWith("4093");
    }

    @Test
    void elProveedorSimuladoRechazaUnaFechaDeEmisionVencida() {
        DocumentoElectronico factura = factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45");
        factura.setEmitidoEn(LocalDateTime.now().minusDays(20));
        byte[] zip = paquete(factura, 15);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-15.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isFalse();
        assertThat(respuesta.codigo()).isEqualTo("2329");
    }

    @Test
    void elNombreDelPaqueteDebeCoincidirConElXml() {
        byte[] zip = paquete(factura("20100070970", "Distribuidora Andina SAC", "Jr. Comercio 45"), 15);

        RespuestaEnvio respuesta = proveedor.enviarComprobante(RUC_EMISOR + "-01-F001-99.zip", zip, configuracion);

        assertThat(respuesta.aceptado()).isFalse();
        assertThat(respuesta.codigo()).isEqualTo("0154");
    }

    @Test
    void unPaqueteConRutasPeligrosasSeRechaza() {
        byte[] zip =
                PaqueteSunat.comprimir("../" + RUC_EMISOR + "-01-F001-15.xml", "<a/>".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> PaqueteSunat.descomprimir(zip)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elResumenDiarioYLaBajaGeneranXmlFirmadoVerificable() throws Exception {
        DocumentoElectronico boleta = factura("20100070970", "Cliente", "Av. 1");
        boleta.setTipo(TipoCpe.BOLETA);
        boleta.setSerie("B001");
        boleta.setClienteTipoDoc(TipoDocumentoCliente.DNI);
        boleta.setClienteDoc("45678912");
        com.codesoftlabs.tusalud.app.entities.ResumenSunat resumen =
                com.codesoftlabs.tusalud.app.entities.ResumenSunat.builder()
                        .id(UUID.randomUUID())
                        .tipo(com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen.RESUMEN_DIARIO)
                        .identificador("RC-20261119-1")
                        .fechaReferencia(java.time.LocalDate.now())
                        .fechaGeneracion(java.time.LocalDate.now())
                        .build();
        byte[] resumenXml = firmador.firmar(
                        generador.resumenDiario(resumen, java.util.List.of(boleta), configuracion),
                        certificado.credencial())
                .contenido();
        Document doc = XmlFirmador.analizar(resumenXml);
        assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("SummaryDocuments");
        assertThat(firmador.verificar(resumenXml)).isTrue();
        assertThat(texto(doc, "ConditionCode")).isEqualTo("1");

        com.codesoftlabs.tusalud.app.entities.ResumenSunat baja =
                com.codesoftlabs.tusalud.app.entities.ResumenSunat.builder()
                        .id(UUID.randomUUID())
                        .tipo(
                                com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen
                                        .COMUNICACION_BAJA)
                        .identificador("RA-20261119-1")
                        .fechaReferencia(java.time.LocalDate.now())
                        .fechaGeneracion(java.time.LocalDate.now())
                        .build();
        DocumentoElectronico factura = factura("20100070970", "Cliente", "Av. 1");
        factura.setMotivoBaja("Error en la emisión");
        byte[] bajaXml = firmador.firmar(
                        generador.comunicacionBaja(baja, java.util.List.of(factura), configuracion),
                        certificado.credencial())
                .contenido();
        Document docBaja = XmlFirmador.analizar(bajaXml);
        assertThat(docBaja.getDocumentElement().getLocalName()).isEqualTo("VoidedDocuments");
        assertThat(firmador.verificar(bajaXml)).isTrue();
    }

    private byte[] paquete(DocumentoElectronico d, int numero) {
        byte[] xml = firmador.firmar(generador.comprobante(d, configuracion), certificado.credencial())
                .contenido();
        return PaqueteSunat.comprimir(RUC_EMISOR + "-01-F001-" + numero + ".xml", xml);
    }

    private DocumentoElectronico factura(String ruc, String nombre, String direccion) {
        DocumentoElectronico d = DocumentoElectronico.builder()
                .id(UUID.randomUUID())
                .tipo(TipoCpe.FACTURA)
                .serie("F001")
                .numero(15)
                .sede("PRINCIPAL")
                .emitidoEn(LocalDateTime.now())
                .clienteTipoDoc(TipoDocumentoCliente.RUC)
                .clienteDoc(ruc)
                .clienteNombre(nombre)
                .clienteDireccion(direccion)
                .estado(EstadoCpe.PENDIENTE)
                .creadoEn(LocalDateTime.now())
                .opGravada(new BigDecimal("100.00"))
                .igv(new BigDecimal("18.00"))
                .total(new BigDecimal("118.00"))
                .build();
        d.agregarItem(item("Consulta de medicina general", "118.00"));
        return d;
    }

    private DocumentoElectronicoItem item(String descripcion, String total) {
        return DocumentoElectronicoItem.builder()
                .descripcion(descripcion)
                .unidad("ZZ")
                .cantidad(new BigDecimal("1.000"))
                .valorUnitario(new BigDecimal("100.000000"))
                .precioUnitario(new BigDecimal(total).setScale(6))
                .afectacion(Afectacion.GRAVADO)
                .valorVenta(new BigDecimal("100.00"))
                .igv(new BigDecimal("18.00"))
                .total(new BigDecimal(total))
                .build();
    }

    private static String texto(Document doc, String nombre) {
        NodeList nodos = doc.getElementsByTagNameNS("*", nombre);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().trim();
    }
}
