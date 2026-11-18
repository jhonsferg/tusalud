package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AprobarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.AseguradoraRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.ConvenioRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.DesgloseRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionCrearRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.LiquidacionPagoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PolizaRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.PreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.RechazarPreautorizacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.SegurosRequests.ReglaRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.AseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.ConvenioResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.DesgloseResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.LiquidacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PreautorizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.VerificacionResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLiquidacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.CuentaAseguradoraRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.seguros.LiquidacionService;
import com.codesoftlabs.tusalud.app.services.seguros.PolizaService;
import com.codesoftlabs.tusalud.app.services.seguros.PreautorizacionService;
import com.codesoftlabs.tusalud.app.services.seguros.SegurosCatalogoService;
import com.codesoftlabs.tusalud.app.services.seguros.SegurosReporteService;
import java.math.BigDecimal;
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
class SegurosServiceTest {

    @Autowired
    private SegurosCatalogoService catalogo;

    @Autowired
    private PolizaService polizas;

    @Autowired
    private PreautorizacionService preautorizaciones;

    @Autowired
    private LiquidacionService liquidaciones;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private CuentaAseguradoraRepository cuentaRepository;

    @Autowired
    private SegurosReporteService reportes;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal recepcion;
    private UsuarioPrincipal finanzas;
    private UsuarioPrincipal cajero;
    private UsuarioPrincipal medicoPrincipal;
    private Medico medico;
    private Paciente paciente;
    private ConvenioResponse convenio;
    private AseguradoraResponse aseguradora;

    @BeforeEach
    void setUp() {
        recepcion = fixture.principal(fixture.usuario("recep", RolNombre.ROLE_RECEPCION));
        finanzas = fixture.principal(fixture.usuario("fin", RolNombre.ROLE_FINANZAS));
        cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        medico = fixture.medicoConHorarioLunes(new BigDecimal("150.00"));
        medicoPrincipal = fixture.principal(medico.getUsuario());
        paciente = fixture.paciente();
        aseguradora = catalogo.crearAseguradora(new AseguradoraRequest(
                "AS" + ServiceTestFixture.siguiente(),
                "Aseguradora de prueba",
                TipoAseguradora.PRIVADO,
                "20123456789",
                null,
                true));
        convenio = catalogo.crearConvenio(convenioRequest("80", "1000.00", true));
    }

    private ConvenioRequest convenioRequest(String porcentaje, String topeAnual, boolean preautorizacionImagen) {
        return new ConvenioRequest(
                aseguradora.id(),
                "Plan de prueba",
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(300),
                topeAnual == null ? null : new BigDecimal(topeAnual),
                true,
                List.of(
                        new ReglaRequest(
                                CategoriaServicio.CONSULTA,
                                TipoCobertura.PORCENTAJE,
                                new BigDecimal(porcentaje),
                                null,
                                false),
                        new ReglaRequest(
                                CategoriaServicio.IMAGEN,
                                TipoCobertura.PORCENTAJE,
                                new BigDecimal("70"),
                                null,
                                preautorizacionImagen)),
                List.of("eco-mama"));
    }

    private PolizaResponse registrarPoliza() {
        return polizas.registrar(
                recepcion,
                new PolizaRequest(
                        paciente.getId(),
                        convenio.id(),
                        "pol-" + ServiceTestFixture.siguiente(),
                        "Titular de prueba",
                        "titular",
                        "Plan A",
                        LocalDate.now().minusDays(5),
                        LocalDate.now().plusDays(200)));
    }

    private Cita citaConfirmada() {
        return fixture.cita(medico, paciente, LocalDateTime.now().plusDays(1), EstadoCita.CONFIRMADA);
    }

    private ComprobanteResponse cobrarCita(Cita cita, Boolean particular, String motivo) {
        return cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.CITA,
                        cita.getId(),
                        null,
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.SIN_DOC,
                        null,
                        "Cliente de mostrador",
                        MetodoPagoCaja.EFECTIVO,
                        null,
                        particular,
                        motivo));
    }

    @Test
    void elCodigoDeAseguradoraSeNormalizaYNoSeRepite() {
        assertThat(aseguradora.codigo()).startsWith("AS");
        assertThatThrownBy(() -> catalogo.crearAseguradora(new AseguradoraRequest(
                        aseguradora.codigo().toLowerCase(), "Otra", TipoAseguradora.EPS, null, null, true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unConvenioValidaPorcentajesVigenciaYCategoriasRepetidas() {
        assertThatThrownBy(() -> catalogo.crearConvenio(convenioRequest("120", null, false)))
                .isInstanceOf(DatosInvalidosException.class);
        ConvenioRequest invertido = new ConvenioRequest(
                aseguradora.id(),
                "Invertido",
                LocalDate.now().plusDays(5),
                LocalDate.now(),
                null,
                true,
                List.of(new ReglaRequest(
                        CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, new BigDecimal("50"), null, false)),
                null);
        assertThatThrownBy(() -> catalogo.crearConvenio(invertido)).isInstanceOf(DatosInvalidosException.class);
        ConvenioRequest repetido = new ConvenioRequest(
                aseguradora.id(),
                "Repetido",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                null,
                true,
                List.of(
                        new ReglaRequest(
                                CategoriaServicio.CONSULTA,
                                TipoCobertura.PORCENTAJE,
                                new BigDecimal("50"),
                                null,
                                false),
                        new ReglaRequest(
                                CategoriaServicio.CONSULTA,
                                TipoCobertura.COPAGO_FIJO,
                                new BigDecimal("10"),
                                null,
                                false)),
                null);
        assertThatThrownBy(() -> catalogo.crearConvenio(repetido)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void actualizarUnConvenioReemplazaSusReglasSinDuplicarCategorias() {
        ConvenioRequest cambio = new ConvenioRequest(
                aseguradora.id(),
                "Plan de prueba",
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(300),
                new BigDecimal("2500.00"),
                true,
                List.of(
                        new ReglaRequest(
                                CategoriaServicio.CONSULTA,
                                TipoCobertura.COPAGO_FIJO,
                                new BigDecimal("25"),
                                null,
                                false),
                        new ReglaRequest(
                                CategoriaServicio.LABORATORIO,
                                TipoCobertura.PORCENTAJE,
                                new BigDecimal("60"),
                                null,
                                false)),
                List.of("rm-rodilla"));
        ConvenioResponse actualizado = catalogo.actualizarConvenio(convenio.id(), cambio);
        assertThat(actualizado.reglas()).hasSize(2);
        assertThat(actualizado.reglas().get(0).categoria()).isEqualTo(CategoriaServicio.CONSULTA);
        assertThat(actualizado.reglas().get(0).tipo()).isEqualTo(TipoCobertura.COPAGO_FIJO);
        assertThat(actualizado.reglas().get(1).categoria()).isEqualTo(CategoriaServicio.LABORATORIO);
        assertThat(actualizado.exclusiones()).containsExactly("RM-RODILLA");
        assertThat(catalogo.convenio(convenio.id()).reglas()).hasSize(2);
        assertThat(catalogo.convenio(convenio.id()).topeAnual()).isEqualByComparingTo("2500.00");
    }

    @Test
    void lasExclusionesSeGuardanEnMayusculas() {
        assertThat(convenio.exclusiones()).containsExactly("ECO-MAMA");
        assertThat(convenio.reglas()).hasSize(2);
        assertThat(convenio.vigente()).isTrue();
    }

    @Test
    void unaPolizaNoSeRepiteEnElMismoConvenioYSeNormaliza() {
        PolizaResponse poliza = registrarPoliza();
        assertThat(poliza.numero()).startsWith("POL-");
        assertThat(poliza.parentesco()).isEqualTo("TITULAR");
        assertThat(poliza.estado()).isEqualTo(EstadoPoliza.ACTIVA);
        assertThatThrownBy(() -> polizas.registrar(
                        recepcion,
                        new PolizaRequest(
                                paciente.getId(),
                                convenio.id(),
                                poliza.numero().toLowerCase(),
                                "Otro",
                                "HIJO",
                                null,
                                LocalDate.now(),
                                LocalDate.now().plusDays(10))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void suspenderUnaPolizaExigeMotivoYElPacienteNoVeDatosAjenos() {
        PolizaResponse poliza = registrarPoliza();
        assertThatThrownBy(() -> polizas.cambiarEstado(
                        recepcion, poliza.id(), new PolizaEstadoRequest(EstadoPoliza.SUSPENDIDA, "no")))
                .isInstanceOf(DatosInvalidosException.class);
        PolizaResponse suspendida = polizas.cambiarEstado(
                recepcion, poliza.id(), new PolizaEstadoRequest(EstadoPoliza.SUSPENDIDA, "Afiliación en revisión"));
        assertThat(suspendida.estado()).isEqualTo(EstadoPoliza.SUSPENDIDA);
        assertThat(polizas.mias(fixture.principal(paciente.getUsuario()))).hasSize(1);
        assertThat(polizas.mias(recepcion)).isEmpty();
    }

    @Test
    void laVerificacionEsDeterministaYReportaElSaldoDelTope() {
        PolizaResponse poliza = registrarPoliza();
        VerificacionResponse primera = polizas.verificar(recepcion, poliza.id());
        VerificacionResponse segunda = polizas.verificar(cajero, poliza.id());
        assertThat(primera.vigente()).isTrue();
        assertThat(primera.estado()).isEqualTo("VIGENTE");
        assertThat(primera.codigo()).isEqualTo(segunda.codigo()).startsWith("VC-");
        assertThat(primera.saldoTope()).isEqualByComparingTo("1000.00");
    }

    @Test
    void unaPolizaSuspendidaNoEsVigenteAlVerificar() {
        PolizaResponse poliza = registrarPoliza();
        polizas.cambiarEstado(
                recepcion, poliza.id(), new PolizaEstadoRequest(EstadoPoliza.SUSPENDIDA, "Motivo de prueba"));
        VerificacionResponse resultado = polizas.verificar(recepcion, poliza.id());
        assertThat(resultado.vigente()).isFalse();
        assertThat(resultado.estado()).isEqualTo("SUSPENDIDA");
    }

    @Test
    void elMedicoSoloVeLaCoberturaDePacientesConRelacion() {
        registrarPoliza();
        assertThatThrownBy(() -> polizas.coberturaParaMedico(medicoPrincipal, paciente.getId()))
                .isInstanceOf(ForbiddenOperationException.class);
        citaConfirmada();
        var cobertura = polizas.coberturaParaMedico(medicoPrincipal, paciente.getId());
        assertThat(cobertura.tieneCobertura()).isTrue();
        assertThat(cobertura.aseguradora()).isEqualTo("Aseguradora de prueba");
        assertThat(cobertura.requierenPreautorizacion()).containsExactly(CategoriaServicio.IMAGEN);
    }

    @Test
    void laPreautorizacionExigidaBloqueaHastaQueSeApruebaYLaUrgenciaLaOmite() {
        registrarPoliza();
        citaConfirmada();
        UUID referencia = UUID.randomUUID();
        assertThatThrownBy(() ->
                        preautorizaciones.exigirSiCorresponde(paciente, CategoriaServicio.IMAGEN, referencia, false))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("exige preautorización");
        preautorizaciones.exigirSiCorresponde(paciente, CategoriaServicio.IMAGEN, referencia, true);
        preautorizaciones.exigirSiCorresponde(paciente, CategoriaServicio.CONSULTA, referencia, false);

        PreautorizacionResponse solicitada = preautorizaciones.solicitar(
                medicoPrincipal,
                new PreautorizacionRequest(
                        paciente.getId(),
                        CategoriaServicio.IMAGEN,
                        referencia,
                        "Resonancia de rodilla",
                        new BigDecimal("650.00")));
        assertThat(solicitada.estado()).isEqualTo(EstadoPreautorizacion.SOLICITADA);
        assertThat(solicitada.codigo()).startsWith("PA-");
        assertThatThrownBy(() ->
                        preautorizaciones.exigirSiCorresponde(paciente, CategoriaServicio.IMAGEN, referencia, false))
                .isInstanceOf(BusinessLogicException.class);

        PreautorizacionResponse aprobada = preautorizaciones.aprobar(
                finanzas, solicitada.id(), new AprobarPreautorizacionRequest(new BigDecimal("600.00"), 15));
        assertThat(aprobada.estado()).isEqualTo(EstadoPreautorizacion.APROBADA);
        assertThat(aprobada.codigoAutorizacion()).startsWith("AUT-");
        assertThat(aprobada.vigenteHasta()).isEqualTo(LocalDate.now().plusDays(15));
        preautorizaciones.exigirSiCorresponde(paciente, CategoriaServicio.IMAGEN, referencia, false);
        assertThatThrownBy(() -> preautorizaciones.exigirSiCorresponde(
                        paciente, CategoriaServicio.IMAGEN, UUID.randomUUID(), false))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void unaPreautorizacionResueltaNoSeResuelveDeNuevoYSePuedeRechazarConMotivo() {
        registrarPoliza();
        citaConfirmada();
        PreautorizacionResponse solicitada = preautorizaciones.solicitar(
                medicoPrincipal,
                new PreautorizacionRequest(
                        paciente.getId(),
                        CategoriaServicio.IMAGEN,
                        null,
                        "Tomografía de abdomen",
                        new BigDecimal("900.00")));
        PreautorizacionResponse rechazada = preautorizaciones.rechazar(
                finanzas, solicitada.id(), new RechazarPreautorizacionRequest("La aseguradora no cubre este estudio"));
        assertThat(rechazada.estado()).isEqualTo(EstadoPreautorizacion.RECHAZADA);
        assertThat(rechazada.motivoRechazo()).contains("no cubre");
        assertThatThrownBy(() -> preautorizaciones.aprobar(
                        finanzas, solicitada.id(), new AprobarPreautorizacionRequest(new BigDecimal("10.00"), null)))
                .isInstanceOf(ConflictException.class);
        assertThat(preautorizaciones.delMedico(medicoPrincipal)).hasSize(1);
        assertThat(preautorizaciones.delPaciente(fixture.principal(paciente.getUsuario())))
                .hasSize(1);
    }

    @Test
    void soloSePuedePedirPreautorizacionSiElConvenioLaExigeYHayPolizaVigente() {
        citaConfirmada();
        assertThatThrownBy(() -> preautorizaciones.solicitar(
                        medicoPrincipal,
                        new PreautorizacionRequest(
                                paciente.getId(),
                                CategoriaServicio.IMAGEN,
                                null,
                                "Sin póliza",
                                new BigDecimal("100.00"))))
                .isInstanceOf(BusinessLogicException.class);
        registrarPoliza();
        assertThatThrownBy(() -> preautorizaciones.solicitar(
                        medicoPrincipal,
                        new PreautorizacionRequest(
                                paciente.getId(),
                                CategoriaServicio.CONSULTA,
                                null,
                                "No la exige",
                                new BigDecimal("100.00"))))
                .isInstanceOf(BusinessLogicException.class);
        UsuarioPrincipal otroMedico = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("100.00")).getUsuario());
        assertThatThrownBy(() -> preautorizaciones.solicitar(
                        otroMedico,
                        new PreautorizacionRequest(
                                paciente.getId(),
                                CategoriaServicio.IMAGEN,
                                null,
                                "Sin relación",
                                new BigDecimal("100.00"))))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void elCajeroCobraSoloElCopagoYSeGeneraLaCuentaPorCobrar() {
        registrarPoliza();
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        Cita cita = citaConfirmada();
        DesgloseResponse desglose =
                cajaService.desglosar(cajero, new DesgloseRequest(OrigenComprobante.CITA, cita.getId()));
        assertThat(desglose.tieneCobertura()).isTrue();
        assertThat(desglose.montoCubierto()).isEqualByComparingTo("120.00");
        assertThat(desglose.copago()).isEqualByComparingTo("30.00");
        assertThat(desglose.polizaNumero()).startsWith("****");

        ComprobanteResponse comprobante = cobrarCita(cita, null, null);
        assertThat(comprobante.total()).isEqualByComparingTo("30.00");
        assertThat(comprobante.montoCubierto()).isEqualByComparingTo("120.00");
        assertThat(comprobante.aseguradoraNombre()).isEqualTo("Aseguradora de prueba");
        var cuentas = cuentaRepository.porEstado(EstadoCuentaAseguradora.PENDIENTE);
        assertThat(cuentas).hasSize(1);
        assertThat(cuentas.get(0).getMontoCubierto()).isEqualByComparingTo("120.00");
        assertThat(cuentas.get(0).getCopago()).isEqualByComparingTo("30.00");
    }

    @Test
    void sePuedeForzarElCobroParticularConMotivoYSinPolizaSeCobraTodo() {
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        Cita sinPoliza = citaConfirmada();
        assertThat(cobrarCita(sinPoliza, null, null).total()).isEqualByComparingTo("150.00");

        registrarPoliza();
        Cita conPoliza = citaConfirmada();
        assertThatThrownBy(() -> cobrarCita(conPoliza, true, "no")).isInstanceOf(DatosInvalidosException.class);
        ComprobanteResponse particular = cobrarCita(conPoliza, true, "El paciente pidió pagar sin seguro");
        assertThat(particular.total()).isEqualByComparingTo("150.00");
        assertThat(particular.montoCubierto()).isNull();
        assertThat(cuentaRepository.porEstado(EstadoCuentaAseguradora.PENDIENTE))
                .isEmpty();
    }

    @Test
    void anularElComprobanteRevierteLaCuentaYUnaCuentaLiquidadaImpideAnular() {
        registrarPoliza();
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        ComprobanteResponse primero = cobrarCita(citaConfirmada(), null, null);
        cajaService.anular(cajero, primero.id(), "Error de digitación");
        assertThat(cuentaRepository.porEstado(EstadoCuentaAseguradora.REVERTIDA))
                .hasSize(1);
        assertThat(cuentaRepository.porEstado(EstadoCuentaAseguradora.PENDIENTE))
                .isEmpty();

        ComprobanteResponse segundo = cobrarCita(citaConfirmada(), null, null);
        liquidaciones.crear(
                finanzas,
                new LiquidacionCrearRequest(aseguradora.id(), LocalDate.now().minusDays(1), LocalDate.now()));
        assertThatThrownBy(() -> cajaService.anular(cajero, segundo.id(), "Ya liquidada"))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("liquidada");
    }

    @Test
    void elTopeAnualSeVaConsumiendoConCadaCobro() {
        ConvenioResponse conTope = catalogo.crearConvenio(new ConvenioRequest(
                aseguradora.id(),
                "Plan con tope",
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(300),
                new BigDecimal("200.00"),
                true,
                List.of(new ReglaRequest(
                        CategoriaServicio.CONSULTA, TipoCobertura.PORCENTAJE, new BigDecimal("80"), null, false)),
                null));
        polizas.registrar(
                recepcion,
                new PolizaRequest(
                        paciente.getId(),
                        conTope.id(),
                        "TOPE-" + ServiceTestFixture.siguiente(),
                        "Titular",
                        "TITULAR",
                        null,
                        LocalDate.now().minusDays(1),
                        LocalDate.now().plusDays(100)));
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        assertThat(cobrarCita(citaConfirmada(), null, null).montoCubierto()).isEqualByComparingTo("120.00");
        ComprobanteResponse segundo = cobrarCita(citaConfirmada(), null, null);
        assertThat(segundo.montoCubierto()).isEqualByComparingTo("80.00");
        ComprobanteResponse tercero = cobrarCita(citaConfirmada(), null, null);
        assertThat(tercero.montoCubierto()).isNull();
        assertThat(tercero.total()).isEqualByComparingTo("150.00");
    }

    @Test
    void laLiquidacionSigueSuCicloDeEstadosYExigeMotivoParaLaGlosa() {
        registrarPoliza();
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        cobrarCita(citaConfirmada(), null, null);
        cobrarCita(citaConfirmada(), null, null);
        assertThatThrownBy(() -> liquidaciones.crear(
                        finanzas,
                        new LiquidacionCrearRequest(
                                aseguradora.id(),
                                LocalDate.now().minusDays(60),
                                LocalDate.now().minusDays(40))))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(liquidaciones.cartera()).hasSize(1);
        assertThat(liquidaciones.cartera().get(0).pendiente()).isEqualByComparingTo("240.00");

        LiquidacionResponse borrador = liquidaciones.crear(
                finanzas,
                new LiquidacionCrearRequest(aseguradora.id(), LocalDate.now().minusDays(1), LocalDate.now()));
        assertThat(borrador.estado()).isEqualTo(EstadoLiquidacion.BORRADOR);
        assertThat(borrador.total()).isEqualByComparingTo("240.00");
        assertThat(borrador.cantidadCuentas()).isEqualTo(2);
        assertThat(borrador.codigo()).startsWith("LQ-");
        assertThat(liquidaciones.cartera()).isEmpty();
        assertThatThrownBy(() -> liquidaciones.registrarPago(
                        finanzas, borrador.id(), new LiquidacionPagoRequest(new BigDecimal("240.00"), null)))
                .isInstanceOf(ConflictException.class);

        assertThat(liquidaciones.enviar(finanzas, borrador.id()).estado()).isEqualTo(EstadoLiquidacion.ENVIADA);
        assertThatThrownBy(() -> liquidaciones.enviar(finanzas, borrador.id())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> liquidaciones.registrarPago(
                        finanzas, borrador.id(), new LiquidacionPagoRequest(new BigDecimal("200.00"), null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> liquidaciones.registrarPago(
                        finanzas, borrador.id(), new LiquidacionPagoRequest(new BigDecimal("300.00"), null)))
                .isInstanceOf(DatosInvalidosException.class);

        LiquidacionResponse observada = liquidaciones.registrarPago(
                finanzas,
                borrador.id(),
                new LiquidacionPagoRequest(new BigDecimal("200.00"), "Glosa por documentación incompleta"));
        assertThat(observada.estado()).isEqualTo(EstadoLiquidacion.OBSERVADA);
        assertThat(observada.montoGlosa()).isEqualByComparingTo("40.00");

        LiquidacionResponse pagada = liquidaciones.registrarPago(
                finanzas, borrador.id(), new LiquidacionPagoRequest(new BigDecimal("240.00"), null));
        assertThat(pagada.estado()).isEqualTo(EstadoLiquidacion.PAGADA);
        assertThat(cuentaRepository.porEstado(EstadoCuentaAseguradora.PAGADA)).hasSize(2);
        assertThat(liquidaciones.cuentasDe(borrador.id())).hasSize(2);
    }

    @Test
    void elResumenDependeDelRol() {
        registrarPoliza();
        var deFinanzas = reportes.resumen(finanzas);
        assertThat(deFinanzas.carteraPendiente()).isNotNull();
        assertThat(deFinanzas.polizasPorVencer()).isNotNull();
        var deRecepcion = reportes.resumen(recepcion);
        assertThat(deRecepcion.carteraPendiente()).isNull();
        assertThat(deRecepcion.cuentasPendientes()).isNull();
        assertThat(deRecepcion.polizasPorVencer()).isNotNull();
        assertThat(deRecepcion.preautorizacionesPendientes()).isNotNull();
    }
}
