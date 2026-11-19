package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.CorreccionClienteRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.NotaItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.NotaRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.DocumentoElectronicoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.RegistroVentasResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ResumenSunatResponse;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.SerieComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.facturacion.PaqueteSunat;
import com.codesoftlabs.tusalud.app.facturacion.XmlFirmador;
import com.codesoftlabs.tusalud.app.repositories.ConfiguracionFiscalRepository;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.SerieComprobanteRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.facturacion.ColaFacturacion;
import com.codesoftlabs.tusalud.app.services.facturacion.FacturacionDocumentos;
import com.codesoftlabs.tusalud.app.services.facturacion.FacturacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class FacturacionServiceTest {

    private static final String RUC_VALIDO = "20100070970";
    private static final String RUC_INVALIDO = "20123456789";

    @Autowired
    private FacturacionService facturacion;

    @Autowired
    private FacturacionDocumentos documentos;

    @Autowired
    private ColaFacturacion cola;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private ConfiguracionFiscalRepository configuraciones;

    @Autowired
    private SerieComprobanteRepository series;

    @Autowired
    private NotificacionRepository notificaciones;

    @Autowired
    private XmlFirmador firmador;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal cajero;
    private UsuarioPrincipal finanzas;
    private Paciente paciente;
    private ConfiguracionFiscal configuracion;

    @BeforeEach
    void preparar() {
        cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        paciente = fixture.paciente();
        configuracion = configuraciones.save(ConfiguracionFiscal.builder()
                .sede("PRINCIPAL")
                .habilitada(true)
                .ruc("20131312955")
                .razonSocial("Clínica TuSalud SAC")
                .direccion("Av. Salud 123")
                .ubigeo("150101")
                .departamento("Lima")
                .provincia("Lima")
                .distrito("Lima")
                .proveedor(ProveedorFiscal.SIMULACION)
                .modo(ModoFiscal.BETA)
                .actualizadoEn(LocalDateTime.now())
                .build());
        for (String[] s :
                new String[][] {{"FC01", "NOTA_CREDITO"}, {"BC01", "NOTA_CREDITO"}, {"FD01", "NOTA_DEBITO"}}) {
            series.save(SerieComprobante.builder()
                    .sede("PRINCIPAL")
                    .tipo(TipoCpe.valueOf(s[1]))
                    .serie(s[0])
                    .build());
        }
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100.00")));
    }

    private ComprobanteResponse boleta(String precio) {
        return cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.SERVICIO,
                        null,
                        paciente.getId(),
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.SIN_DOC,
                        null,
                        "Cliente de mostrador",
                        MetodoPagoCaja.EFECTIVO,
                        List.of(new CobroItemRequest("Curación", 1, new BigDecimal(precio)))));
    }

    private ComprobanteResponse factura(String ruc, String precio) {
        return cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.SERVICIO,
                        null,
                        null,
                        TipoComprobante.FACTURA,
                        TipoDocumentoCliente.RUC,
                        ruc,
                        "Distribuidora Andina SAC",
                        MetodoPagoCaja.TRANSFERENCIA,
                        List.of(new CobroItemRequest("Chequeo ocupacional", 1, new BigDecimal(precio)))));
    }

    private DocumentoElectronicoDetalle detalle(ComprobanteResponse c) {
        return facturacion.deComprobante(c.id()).orElseThrow();
    }

    private DocumentoElectronicoDetalle enviada(ComprobanteResponse c) {
        cola.procesarAhora();
        return detalle(c);
    }

    @Test
    void elCobroGeneraElComprobanteElectronicoFirmadoConLaNumeracionDelCobro() {
        ComprobanteResponse cobro = boleta("59.00");

        DocumentoElectronicoDetalle d = detalle(cobro);

        assertThat(d.estado()).isEqualTo(EstadoCpe.PENDIENTE);
        assertThat(d.tipo()).isEqualTo(TipoCpe.BOLETA);
        assertThat(d.serie()).isEqualTo(cobro.serie());
        assertThat(d.numero()).isEqualTo(cobro.numero());
        assertThat(d.total()).isEqualByComparingTo("59.00");
        assertThat(d.opGravada().add(d.igv())).isEqualByComparingTo("59.00");
        assertThat(d.digestValue()).isNotBlank();
        assertThat(firmador.verificar(facturacion.xml(d.id()))).isTrue();
    }

    @Test
    void sinConfiguracionHabilitadaElCobroNoGeneraComprobanteElectronico() {
        configuracion.setHabilitada(false);
        configuraciones.save(configuracion);

        ComprobanteResponse cobro = boleta("20.00");

        assertThat(facturacion.deComprobante(cobro.id())).isEmpty();
    }

    @Test
    void laColaEnviaYGuardaLaConstanciaDeRecepcionYLaBitacora() {
        ComprobanteResponse cobro = factura(RUC_VALIDO, "236.00");

        DocumentoElectronicoDetalle d = enviada(cobro);

        assertThat(d.estado()).isEqualTo(EstadoCpe.ACEPTADO_OBSERVACIONES);
        assertThat(d.cdrCodigo()).isEqualTo("0");
        assertThat(d.cdrDescripcion()).contains("aceptado");
        assertThat(d.intentos()).isEqualTo(1);
        assertThat(d.intercambios()).hasSize(1);
        assertThat(d.intercambios().get(0).exitoso()).isTrue();
        PaqueteSunat.Cdr cdr = PaqueteSunat.leerCdr(facturacion.cdr(d.id()));
        assertThat(cdr.aceptado()).isTrue();
    }

    @Test
    void unaFacturaConDireccionSeAceptaSinObservaciones() {
        ComprobanteResponse cobro = factura(RUC_VALIDO, "118.00");
        DocumentoElectronicoDetalle original = detalle(cobro);

        facturacion.corregirCliente(
                finanzas,
                original.id(),
                new CorreccionClienteRequest(
                        TipoDocumentoCliente.RUC, RUC_VALIDO, "Distribuidora Andina SAC", "Jr. Comercio 45"));

        assertThat(detalle(cobro).estado()).isEqualTo(EstadoCpe.ACEPTADO);
    }

    @Test
    void unRucInvalidoSeRechazaYSeCorrigeSinCambiarElNumero() {
        ComprobanteResponse cobro = factura(RUC_INVALIDO, "118.00");
        DocumentoElectronicoDetalle antes = detalle(cobro);

        DocumentoElectronicoDetalle rechazado = enviada(cobro);

        assertThat(rechazado.estado()).isEqualTo(EstadoCpe.RECHAZADO);
        assertThat(rechazado.cdrCodigo()).isEqualTo("2017");
        assertThat(notificaciones.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        finanzas.getId(),
                        TipoNotificacion.COMPROBANTE_RECHAZADO,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
        assertThatThrownBy(() -> facturacion.corregirCliente(
                        finanzas,
                        antes.id(),
                        new CorreccionClienteRequest(TipoDocumentoCliente.RUC, RUC_INVALIDO, "Cliente", null)))
                .isInstanceOf(DatosInvalidosException.class);

        DocumentoElectronicoDetalle corregido = facturacion.corregirCliente(
                finanzas,
                antes.id(),
                new CorreccionClienteRequest(
                        TipoDocumentoCliente.RUC, RUC_VALIDO, "Distribuidora Andina SAC", "Av. 1"));

        assertThat(corregido.estado()).isEqualTo(EstadoCpe.ACEPTADO);
        assertThat(corregido.numero()).isEqualTo(antes.numero());
        assertThat(corregido.clienteDoc()).isEqualTo(RUC_VALIDO);
        assertThat(cajaService.obtener(cobro.id()).clienteDoc()).isEqualTo(RUC_VALIDO);
    }

    @Test
    void unComprobanteAceptadoNoSeModificaSeCorrigeConUnaNota() {
        ComprobanteResponse cobro = factura(RUC_VALIDO, "118.00");
        DocumentoElectronicoDetalle aceptado = enviada(cobro);

        assertThatThrownBy(() -> facturacion.corregirCliente(
                        finanzas,
                        aceptado.id(),
                        new CorreccionClienteRequest(TipoDocumentoCliente.RUC, RUC_VALIDO, "Otro nombre", null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> facturacion.reenviar(finanzas, aceptado.id())).isInstanceOf(ConflictException.class);
    }

    @Test
    void laNotaDeCreditoTotalSeEmiteConSuSerieYAcreditaElImporte() {
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));

        DocumentoElectronicoDetalle nota = facturacion.crearNota(
                finanzas, new NotaRequest(factura.id(), TipoCpe.NOTA_CREDITO, "01", "Anulación de la operación", null));

        assertThat(nota.tipo()).isEqualTo(TipoCpe.NOTA_CREDITO);
        assertThat(nota.serie()).isEqualTo("FC01");
        assertThat(nota.numero()).isEqualTo(1);
        assertThat(nota.estado().aceptado()).isTrue();
        assertThat(nota.total()).isEqualByComparingTo("118.00");
        assertThat(nota.documentoReferenciaNumero()).isEqualTo(factura.numeroCompleto());
        assertThat(facturacion.detalle(factura.id()).acreditado()).isEqualByComparingTo("118.00");
        assertThatThrownBy(() -> facturacion.crearNota(
                        finanzas,
                        new NotaRequest(
                                factura.id(),
                                TipoCpe.NOTA_CREDITO,
                                "09",
                                "Disminución en el valor",
                                List.of(new NotaItemRequest("Ajuste", BigDecimal.ONE, new BigDecimal("10.00"), null)))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("superan");
    }

    @Test
    void lasNotasParcialesSumanHastaElLimiteYElCorrelativoAvanza() {
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));

        DocumentoElectronicoDetalle primera = facturacion.crearNota(
                finanzas,
                new NotaRequest(
                        factura.id(),
                        TipoCpe.NOTA_CREDITO,
                        "09",
                        "Disminución en el valor",
                        List.of(new NotaItemRequest("Descuento", BigDecimal.ONE, new BigDecimal("59.00"), null))));
        DocumentoElectronicoDetalle segunda = facturacion.crearNota(
                finanzas,
                new NotaRequest(
                        factura.id(),
                        TipoCpe.NOTA_CREDITO,
                        "09",
                        "Disminución en el valor",
                        List.of(new NotaItemRequest("Descuento", BigDecimal.ONE, new BigDecimal("59.00"), null))));

        assertThat(primera.numero()).isEqualTo(1);
        assertThat(segunda.numero()).isEqualTo(2);
        assertThat(primera.opGravada()).isEqualByComparingTo("50.00");
        assertThat(primera.igv()).isEqualByComparingTo("9.00");
        assertThatThrownBy(() -> facturacion.crearNota(
                        finanzas,
                        new NotaRequest(
                                factura.id(),
                                TipoCpe.NOTA_CREDITO,
                                "09",
                                "Disminución en el valor",
                                List.of(new NotaItemRequest(
                                        "Descuento", BigDecimal.ONE, new BigDecimal("0.01"), null)))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laNotaDeDebitoRequiereItemsYUsaSuPropiaSerie() {
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));

        assertThatThrownBy(() -> facturacion.crearNota(
                        finanzas, new NotaRequest(factura.id(), TipoCpe.NOTA_DEBITO, "01", "Intereses por mora", null)))
                .isInstanceOf(DatosInvalidosException.class);

        DocumentoElectronicoDetalle nota = facturacion.crearNota(
                finanzas,
                new NotaRequest(
                        factura.id(),
                        TipoCpe.NOTA_DEBITO,
                        "01",
                        "Intereses por mora",
                        List.of(new NotaItemRequest("Interés", BigDecimal.ONE, new BigDecimal("11.80"), null))));

        assertThat(nota.serie()).isEqualTo("FD01");
        assertThat(nota.tipo()).isEqualTo(TipoCpe.NOTA_DEBITO);
        assertThat(nota.estado().aceptado()).isTrue();
    }

    @Test
    void noSeEmiteNotaSobreUnComprobanteNoAceptadoNiSobreOtraNota() {
        DocumentoElectronicoDetalle pendiente = detalle(factura(RUC_VALIDO, "118.00"));

        assertThatThrownBy(() -> facturacion.crearNota(
                        finanzas,
                        new NotaRequest(pendiente.id(), TipoCpe.NOTA_CREDITO, "01", "Anulación de la operación", null)))
                .isInstanceOf(ConflictException.class);

        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "50.00"));
        DocumentoElectronicoDetalle nota = facturacion.crearNota(
                finanzas, new NotaRequest(factura.id(), TipoCpe.NOTA_CREDITO, "01", "Anulación de la operación", null));
        assertThatThrownBy(() -> facturacion.crearNota(
                        finanzas,
                        new NotaRequest(nota.id(), TipoCpe.NOTA_CREDITO, "01", "Anulación de la operación", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void laBajaDeUnaFacturaGeneraUnaComunicacionQueLaAnulaAlSerAceptada() {
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));

        facturacion.solicitarBaja(finanzas, factura.id(), "Error en la emisión");
        ResumenSunatResponse baja = facturacion.listarResumenes().get(0);
        assertThat(baja.identificador()).startsWith("RA-");
        assertThat(baja.estado()).isEqualTo(EstadoResumen.PENDIENTE);
        assertThatThrownBy(() -> facturacion.solicitarBaja(finanzas, factura.id(), "Otra vez"))
                .isInstanceOf(ConflictException.class);

        facturacion.procesarResumen(baja.id());
        assertThat(facturacion.listarResumenes().get(0).estado()).isEqualTo(EstadoResumen.ENVIADO);
        facturacion.procesarResumen(baja.id());

        assertThat(facturacion.listarResumenes().get(0).estado()).isEqualTo(EstadoResumen.ACEPTADO);
        assertThat(facturacion.detalle(factura.id()).estado()).isEqualTo(EstadoCpe.ANULADO);
    }

    @Test
    void noSePuedeDarDeBajaUnComprobanteConNotasDeCredito() {
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));
        facturacion.crearNota(
                finanzas,
                new NotaRequest(
                        factura.id(),
                        TipoCpe.NOTA_CREDITO,
                        "09",
                        "Disminución en el valor",
                        List.of(new NotaItemRequest("Descuento", BigDecimal.ONE, new BigDecimal("10.00"), null))));

        assertThatThrownBy(() -> facturacion.solicitarBaja(finanzas, factura.id(), "Error en la emisión"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("notas de crédito");
    }

    @Test
    void lasBoletasPorResumenEsperanAlResumenDiarioYSeAceptanConElTicket() {
        configuracion.setBoletasPorResumen(true);
        configuraciones.save(configuracion);
        ComprobanteResponse cobro = boleta("59.00");

        cola.procesarAhora();
        assertThat(detalle(cobro).estado()).isEqualTo(EstadoCpe.PENDIENTE);
        assertThat(detalle(cobro).intentos()).isZero();

        ResumenSunatResponse resumen = facturacion.generarResumenDiario(finanzas, LocalDate.now());
        assertThat(resumen.identificador()).startsWith("RC-");
        assertThat(resumen.documentos()).isEqualTo(1);
        assertThatThrownBy(() -> facturacion.generarResumenDiario(finanzas, LocalDate.now()))
                .isInstanceOf(ConflictException.class);

        facturacion.procesarResumen(resumen.id());
        facturacion.procesarResumen(resumen.id());

        assertThat(detalle(cobro).estado()).isEqualTo(EstadoCpe.ACEPTADO);
        assertThat(facturacion.listarResumenes().get(0).estado()).isEqualTo(EstadoResumen.ACEPTADO);
    }

    @Test
    void alAnularElCobroDeUnComprobanteAceptadoSeComunicaLaBaja() {
        ComprobanteResponse cobro = factura(RUC_VALIDO, "118.00");
        enviada(cobro);

        ComprobanteResponse anulado = cajaService.anular(cajero, cobro.id(), "Error de digitación");

        assertThat(anulado.estado()).isEqualTo(EstadoComprobante.ANULADO);
        ResumenSunatResponse baja = facturacion.listarResumenes().get(0);
        assertThat(baja.motivo()).isEqualTo("Error de digitación");
        facturacion.procesarResumen(baja.id());
        facturacion.procesarResumen(baja.id());
        assertThat(detalle(cobro).estado()).isEqualTo(EstadoCpe.ANULADO);
    }

    @Test
    void alAnularUnCobroNoEnviadoElComprobanteElectronicoQuedaAnuladoLocalmente() {
        ComprobanteResponse cobro = boleta("40.00");

        cajaService.anular(cajero, cobro.id(), "Cliente se arrepintió");

        DocumentoElectronicoDetalle d = detalle(cobro);
        assertThat(d.estado()).isEqualTo(EstadoCpe.ANULADO);
        assertThat(d.proximoIntento()).isNull();
        assertThat(cola.procesarAhora()).isZero();
    }

    @Test
    void conAfectacionExoneradaElComprobanteNoLlevaIgv() {
        configuracion.setAfectacionServicios(Afectacion.EXONERADO);
        configuraciones.save(configuracion);

        ComprobanteResponse cobro = boleta("100.00");

        assertThat(cobro.igv()).isEqualByComparingTo("0.00");
        assertThat(cobro.subtotal()).isEqualByComparingTo("100.00");
        DocumentoElectronicoDetalle d = detalle(cobro);
        assertThat(d.opExonerada()).isEqualByComparingTo("100.00");
        assertThat(d.opGravada()).isEqualByComparingTo("0.00");
        assertThat(d.igv()).isEqualByComparingTo("0.00");
        String xml = new String(facturacion.xml(d.id()), StandardCharsets.UTF_8);
        assertThat(xml).contains("<cbc:TaxExemptionReasonCode>20</cbc:TaxExemptionReasonCode>");
        assertThat(enviada(cobro).estado().aceptado()).isTrue();
    }

    @Test
    void lasLineasCuadranConLosTotalesDelCobroAunConRedondeos() {
        ComprobanteResponse cobro = cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.SERVICIO,
                        null,
                        null,
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.SIN_DOC,
                        null,
                        "Cliente de mostrador",
                        MetodoPagoCaja.EFECTIVO,
                        List.of(
                                new CobroItemRequest("A", 3, new BigDecimal("0.35")),
                                new CobroItemRequest("B", 7, new BigDecimal("1.15")),
                                new CobroItemRequest("C", 1, new BigDecimal("9.99")))));

        DocumentoElectronicoDetalle d = detalle(cobro);

        assertThat(d.total()).isEqualByComparingTo(cobro.total());
        assertThat(d.opGravada()).isEqualByComparingTo(cobro.subtotal());
        assertThat(d.igv()).isEqualByComparingTo(cobro.igv());
        BigDecimal lineas = d.items().stream().map(i -> i.valorVenta()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(lineas).isEqualByComparingTo(d.opGravada());
        assertThat(enviada(cobro).estado().aceptado()).isTrue();
    }

    @Test
    void elRegistroDeVentasRestaLasNotasDeCreditoYMuestraLosAnuladosEnCero() {
        enviada(boleta("59.00"));
        DocumentoElectronicoDetalle factura = enviada(factura(RUC_VALIDO, "118.00"));
        facturacion.crearNota(
                finanzas, new NotaRequest(factura.id(), TipoCpe.NOTA_CREDITO, "01", "Anulación de la operación", null));

        RegistroVentasResponse registro = documentos.registroVentas(LocalDate.now(), LocalDate.now());

        assertThat(registro.filas()).hasSize(3);
        assertThat(registro.total()).isEqualByComparingTo("59.00");
        assertThat(registro.totalIgv()).isEqualByComparingTo("9.00");
        assertThat(documentos
                        .excelRegistroVentas(finanzas, LocalDate.now(), LocalDate.now())
                        .contenido())
                .startsWith((byte) 'P', (byte) 'K');
        assertThatThrownBy(() -> documentos.registroVentas(
                        LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void laRepresentacionImpresaLaVeElPacienteTitularYNoOtro() {
        ComprobanteResponse cobro = boleta("59.00");
        UUID id = detalle(cobro).id();
        UsuarioPrincipal titular = fixture.principal(paciente.getUsuario());
        UsuarioPrincipal otro = fixture.principal(fixture.paciente().getUsuario());

        byte[] pdf = documentos.pdf(titular, id).contenido();

        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(documentos.pdf(finanzas, id).contenido()).isNotEmpty();
        assertThatThrownBy(() -> documentos.pdf(otro, id)).isInstanceOf(ForbiddenOperationException.class);
        assertThat(facturacion.misComprobantes(titular)).extracting(m -> m.id()).contains(id);
        assertThat(facturacion.misComprobantes(otro)).isEmpty();
    }

    @Test
    void sinDestinoAlcanzableElEnvioSeReintentaConRetrocesoYTerminaEnError() {
        configuracion.setProveedor(ProveedorFiscal.OSE);
        configuracion.setUrlServicio("http://127.0.0.1:9/billService");
        configuraciones.save(configuracion);
        ComprobanteResponse cobro = boleta("59.00");
        UUID id = detalle(cobro).id();

        facturacion.enviar(id);
        DocumentoElectronicoDetalle primero = facturacion.detalle(id);
        assertThat(primero.estado()).isEqualTo(EstadoCpe.PENDIENTE);
        assertThat(primero.intentos()).isEqualTo(1);
        assertThat(primero.proximoIntento()).isAfter(LocalDateTime.now());
        assertThat(primero.ultimoError()).isNotBlank();

        for (int i = 0; i < 5; i++) {
            facturacion.enviar(id);
        }

        DocumentoElectronicoDetalle ultimo = facturacion.detalle(id);
        assertThat(ultimo.estado()).isEqualTo(EstadoCpe.ERROR);
        assertThat(ultimo.proximoIntento()).isNull();
        assertThat(ultimo.intercambios()).hasSize(6);
        assertThat(ultimo.intercambios()).noneMatch(x -> x.exitoso());
    }
}
