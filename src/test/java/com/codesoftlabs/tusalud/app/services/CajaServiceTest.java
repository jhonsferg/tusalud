package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CierreCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.response.ComprobanteResponse;
import com.codesoftlabs.tusalud.app.dto.response.CuentaPorCobrarResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteIngresosResponse;
import com.codesoftlabs.tusalud.app.dto.response.ResumenCajaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SesionCajaResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.repositories.OrdenLabCobroRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
class CajaServiceTest {

    @Autowired
    private CajaService cajaService;

    @Autowired
    private OrdenLabCobroRepository ordenRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private UsuarioPrincipal cajero;
    private UsuarioPrincipal otroCajero;
    private UsuarioPrincipal admin;
    private Medico medico;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        otroCajero = fixture.principal(fixture.usuario("caja2", RolNombre.ROLE_CAJA));
        admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        medico = fixture.medicoConHorarioLunes(new BigDecimal("150.00"));
        paciente = fixture.paciente();
    }

    private void abrir(UsuarioPrincipal actor, String monto) {
        cajaService.abrir(actor, new AperturaCajaRequest(new BigDecimal(monto)));
    }

    private CobroRequest servicio(String precio, int cantidad, MetodoPagoCaja metodo) {
        return new CobroRequest(
                OrigenComprobante.SERVICIO,
                null,
                null,
                TipoComprobante.BOLETA,
                TipoDocumentoCliente.SIN_DOC,
                null,
                "Cliente de mostrador",
                metodo,
                List.of(new CobroItemRequest("Curación", cantidad, new BigDecimal(precio))));
    }

    private CobroRequest deCita(Long citaId, MetodoPagoCaja metodo) {
        return new CobroRequest(
                OrigenComprobante.CITA,
                citaId,
                null,
                TipoComprobante.BOLETA,
                TipoDocumentoCliente.DNI,
                paciente.getDni().length() == 8 ? paciente.getDni() : "12345678",
                "Paciente de prueba",
                metodo,
                null);
    }

    private Cita cita(EstadoCita estado) {
        return fixture.cita(medico, paciente, LocalDateTime.now().plusDays(1), estado);
    }

    private OrdenLaboratorio orden(String total) {
        return ordenRepository.save(OrdenLaboratorio.builder()
                .codigo("LAB-T" + ServiceTestFixture.siguiente())
                .fecha(LocalDateTime.now())
                .paciente(paciente)
                .medico(medico)
                .total(new BigDecimal(total))
                .build());
    }

    @Test
    void aperturaYCierreCalculanLaDiferenciaYAvisanAlAdministrador() {
        abrir(cajero, "100.00");
        cajaService.cobrar(cajero, servicio("50.00", 1, MetodoPagoCaja.EFECTIVO));
        cajaService.cobrar(cajero, servicio("30.00", 1, MetodoPagoCaja.TARJETA));

        SesionCajaResponse cierre =
                cajaService.cerrar(cajero, new CierreCajaRequest(new BigDecimal("140.00"), "Faltan 10"));

        assertThat(cierre.estado()).isEqualTo(EstadoCaja.CERRADA);
        assertThat(cierre.montoEsperado()).isEqualByComparingTo("150.00");
        assertThat(cierre.diferencia()).isEqualByComparingTo("-10.00");
        assertThat(notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        admin.getId(),
                        TipoNotificacion.CAJA_DIFERENCIA,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
        assertThatThrownBy(() -> cajaService.actual(cajero)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unCierreExactoNoNotificaYSoloSeAbreUnaCajaALaVez() {
        abrir(cajero, "50.00");
        assertThatThrownBy(() -> abrir(cajero, "10.00")).isInstanceOf(BusinessLogicException.class);
        cajaService.cobrar(cajero, servicio("20.00", 1, MetodoPagoCaja.EFECTIVO));

        SesionCajaResponse cierre = cajaService.cerrar(cajero, new CierreCajaRequest(new BigDecimal("70.00"), null));

        assertThat(cierre.diferencia()).isEqualByComparingTo("0.00");
        assertThat(notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        admin.getId(),
                        TipoNotificacion.CAJA_DIFERENCIA,
                        LocalDate.now().atStartOfDay()))
                .isFalse();
        assertThatThrownBy(() -> cajaService.cerrar(cajero, new CierreCajaRequest(BigDecimal.ONE, null)))
                .isInstanceOf(BusinessLogicException.class);
        abrir(cajero, "0.00");
        assertThat(cajaService.actual(cajero).estado()).isEqualTo(EstadoCaja.ABIERTA);
    }

    @Test
    void cobrarExigeUnaCajaAbierta() {
        assertThatThrownBy(() -> cajaService.cobrar(cajero, servicio("10.00", 1, MetodoPagoCaja.EFECTIVO)))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("caja abierta");
    }

    @Test
    void elIgvSeSacaDelTotalConRedondeoAlCentimo() {
        abrir(cajero, "0.00");

        ComprobanteResponse exacto = cajaService.cobrar(cajero, servicio("118.00", 1, MetodoPagoCaja.EFECTIVO));
        ComprobanteResponse redondeo = cajaService.cobrar(cajero, servicio("3.33", 3, MetodoPagoCaja.EFECTIVO));

        assertThat(exacto.total()).isEqualByComparingTo("118.00");
        assertThat(exacto.subtotal()).isEqualByComparingTo("100.00");
        assertThat(exacto.igv()).isEqualByComparingTo("18.00");
        assertThat(redondeo.total()).isEqualByComparingTo("9.99");
        assertThat(redondeo.subtotal()).isEqualByComparingTo("8.47");
        assertThat(redondeo.igv()).isEqualByComparingTo("1.52");
        assertThat(redondeo.subtotal().add(redondeo.igv())).isEqualByComparingTo(redondeo.total());
        assertThat(exacto.codigo()).matches("B001-\\d{6}");
        assertThat(redondeo.numero()).isEqualTo(exacto.numero() + 1);
        assertThat(redondeo.items())
                .singleElement()
                .satisfies(i -> assertThat(i.subtotal()).isEqualByComparingTo("9.99"));
    }

    @Test
    void validaElDocumentoDelClienteSegunElTipoDeComprobante() {
        abrir(cajero, "0.00");
        CobroRequest base = servicio("100.00", 1, MetodoPagoCaja.EFECTIVO);

        assertThatThrownBy(() -> cajaService.cobrar(
                        cajero, conDocumento(base, TipoComprobante.FACTURA, TipoDocumentoCliente.SIN_DOC, null)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cajaService.cobrar(
                        cajero, conDocumento(base, TipoComprobante.FACTURA, TipoDocumentoCliente.RUC, "123")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cajaService.cobrar(
                        cajero, conDocumento(base, TipoComprobante.BOLETA, TipoDocumentoCliente.RUC, "20123456789")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cajaService.cobrar(
                        cajero, conDocumento(base, TipoComprobante.BOLETA, TipoDocumentoCliente.DNI, "1234")))
                .isInstanceOf(DatosInvalidosException.class);

        ComprobanteResponse factura = cajaService.cobrar(
                cajero, conDocumento(base, TipoComprobante.FACTURA, TipoDocumentoCliente.RUC, "20123456789"));
        assertThat(factura.serie()).isEqualTo("F001");
        assertThat(factura.clienteDoc()).isEqualTo("20123456789");
    }

    @Test
    void laBoletaSinDocumentoTieneUnLimiteDe700Soles() {
        abrir(cajero, "0.00");

        assertThat(cajaService
                        .cobrar(cajero, servicio("700.00", 1, MetodoPagoCaja.TARJETA))
                        .clienteDoc())
                .isNull();
        assertThatThrownBy(() -> cajaService.cobrar(cajero, servicio("700.01", 1, MetodoPagoCaja.TARJETA)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("700");
        CobroRequest conDni = conDocumento(
                servicio("800.00", 1, MetodoPagoCaja.TARJETA),
                TipoComprobante.BOLETA,
                TipoDocumentoCliente.DNI,
                "12345678");
        assertThat(cajaService.cobrar(cajero, conDni).total()).isEqualByComparingTo("800.00");
    }

    @Test
    void cobrarUnaCitaRegistraSuPagoYElPacienteLoVe() {
        abrir(cajero, "0.00");
        Cita cita = cita(EstadoCita.CONFIRMADA);

        ComprobanteResponse comprobante = cajaService.cobrar(cajero, deCita(cita.getId(), MetodoPagoCaja.YAPE));

        assertThat(comprobante.total()).isEqualByComparingTo("150.00");
        assertThat(comprobante.pacienteId()).isEqualTo(paciente.getId());
        assertThat(cita.getPago()).isNotNull();
        assertThat(cita.getPago().getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(cita.getPago().getMetodoDescripcion()).contains("Yape");
        assertThat(cita.getPago().getComprobante()).isEqualTo("CJ-" + comprobante.codigo());
        assertThat(notificacionRepository.existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
                        paciente.getUsuario().getId(),
                        TipoNotificacion.COMPROBANTE_EMITIDO,
                        LocalDate.now().atStartOfDay()))
                .isTrue();
        assertThatThrownBy(() -> cajaService.cobrar(cajero, deCita(cita.getId(), MetodoPagoCaja.EFECTIVO)))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void noSeCobraUnaCitaCanceladaNiInexistente() {
        abrir(cajero, "0.00");
        Cita cancelada = cita(EstadoCita.CANCELADA);

        assertThatThrownBy(() -> cajaService.cobrar(cajero, deCita(cancelada.getId(), MetodoPagoCaja.EFECTIVO)))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> cajaService.cobrar(cajero, deCita(999_999L, MetodoPagoCaja.EFECTIVO)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> cajaService.cobrar(cajero, deCita(null, MetodoPagoCaja.EFECTIVO)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void anularRevierteElPagoDeLaCitaYPermiteCobrarDeNuevo() {
        abrir(cajero, "0.00");
        Cita cita = cita(EstadoCita.PENDIENTE);
        ComprobanteResponse comprobante = cajaService.cobrar(cajero, deCita(cita.getId(), MetodoPagoCaja.EFECTIVO));

        ComprobanteResponse anulado = cajaService.anular(cajero, comprobante.id(), "Error de digitación");

        assertThat(anulado.estado()).isEqualTo(EstadoComprobante.ANULADO);
        assertThat(anulado.motivoAnulacion()).isEqualTo("Error de digitación");
        assertThat(cita.getPago().getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(cajaService.resumen(cajero).total()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> cajaService.anular(cajero, comprobante.id(), "Otra vez"))
                .isInstanceOf(BusinessLogicException.class);

        ComprobanteResponse nuevo = cajaService.cobrar(cajero, deCita(cita.getId(), MetodoPagoCaja.TARJETA));
        assertThat(cita.getPago().getEstado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(cita.getPago().getComprobante()).isEqualTo("CJ-" + nuevo.codigo());
    }

    @Test
    void cobrarUnaOrdenDeLaboratorioLaMarcaComoCobradaYAnularLaLibera() {
        abrir(cajero, "0.00");
        OrdenLaboratorio orden = orden("85.50");
        CobroRequest request = new CobroRequest(
                OrigenComprobante.LABORATORIO,
                orden.getId(),
                null,
                TipoComprobante.BOLETA,
                TipoDocumentoCliente.SIN_DOC,
                null,
                "Paciente de prueba",
                MetodoPagoCaja.EFECTIVO,
                null);

        ComprobanteResponse comprobante = cajaService.cobrar(cajero, request);

        assertThat(comprobante.total()).isEqualByComparingTo("85.50");
        assertThat(orden.isCobrado()).isTrue();
        assertThatThrownBy(() -> cajaService.cobrar(cajero, request)).isInstanceOf(BusinessLogicException.class);

        cajaService.anular(cajero, comprobante.id(), "Cobro duplicado");
        assertThat(orden.isCobrado()).isFalse();
        assertThat(cajaService.cobrar(cajero, request).estado()).isEqualTo(EstadoComprobante.EMITIDO);
    }

    @Test
    void unaCajaCerradaSoloLaAnulaElAdministradorYUnCajeroAjenoNo() {
        abrir(cajero, "0.00");
        ComprobanteResponse comprobante = cajaService.cobrar(cajero, servicio("40.00", 1, MetodoPagoCaja.EFECTIVO));
        abrir(otroCajero, "0.00");

        assertThatThrownBy(() -> cajaService.anular(otroCajero, comprobante.id(), "No es mío"))
                .isInstanceOf(ForbiddenOperationException.class);
        cajaService.cerrar(cajero, new CierreCajaRequest(new BigDecimal("40.00"), null));
        assertThatThrownBy(() -> cajaService.anular(cajero, comprobante.id(), "Tarde"))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("cerrada");

        assertThat(cajaService
                        .anular(admin, comprobante.id(), "Corrección contable")
                        .estado())
                .isEqualTo(EstadoComprobante.ANULADO);
    }

    @Test
    void laCuentaPorCobrarListaCitasYOrdenesYSeVaciaAlCobrar() {
        abrir(cajero, "0.00");
        Cita cita = cita(EstadoCita.ATENDIDA);
        Cita cancelada = cita(EstadoCita.CANCELADA);
        OrdenLaboratorio orden = orden("60.00");

        List<CuentaPorCobrarResponse> antes = cajaService.pendientes(null);
        assertThat(antes).extracting(CuentaPorCobrarResponse::referenciaId).contains(cita.getId(), orden.getId());
        assertThat(antes).extracting(CuentaPorCobrarResponse::referenciaId).doesNotContain(cancelada.getId());
        assertThat(cajaService.pendientes(paciente.getDni())).hasSize(2);

        cajaService.cobrar(cajero, deCita(cita.getId(), MetodoPagoCaja.EFECTIVO));

        assertThat(cajaService.pendientes(paciente.getDni()))
                .singleElement()
                .satisfies(c -> assertThat(c.origen()).isEqualTo(OrigenComprobante.LABORATORIO));
    }

    @Test
    void elResumenAgrupaPorMetodoDePago() {
        abrir(cajero, "20.00");
        cajaService.cobrar(cajero, servicio("10.00", 1, MetodoPagoCaja.EFECTIVO));
        cajaService.cobrar(cajero, servicio("25.00", 1, MetodoPagoCaja.YAPE));
        cajaService.cobrar(cajero, servicio("5.00", 2, MetodoPagoCaja.EFECTIVO));

        ResumenCajaResponse resumen = cajaService.resumen(cajero);

        assertThat(resumen.comprobantes()).isEqualTo(3);
        assertThat(resumen.total()).isEqualByComparingTo("45.00");
        assertThat(resumen.efectivoEsperado()).isEqualByComparingTo("40.00");
        assertThat(resumen.porMetodo())
                .filteredOn(m -> m.metodo() == MetodoPagoCaja.EFECTIVO)
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.total()).isEqualByComparingTo("20.00");
                    assertThat(m.comprobantes()).isEqualTo(2);
                });
    }

    @Test
    void listaLosComprobantesConFiltrosYElReporteDeIngresos() {
        abrir(cajero, "0.00");
        ComprobanteResponse uno = cajaService.cobrar(cajero, servicio("30.00", 1, MetodoPagoCaja.EFECTIVO));
        ComprobanteResponse dos = cajaService.cobrar(cajero, servicio("70.00", 1, MetodoPagoCaja.TARJETA));
        cajaService.anular(cajero, dos.id(), "Duplicado");
        LocalDate hoy = LocalDate.now();

        assertThat(cajaService.comprobantes(hoy, hoy, null, null))
                .extracting(ComprobanteResponse::id)
                .contains(uno.id(), dos.id());
        assertThat(cajaService.comprobantes(hoy, hoy, null, EstadoComprobante.ANULADO))
                .extracting(ComprobanteResponse::id)
                .contains(dos.id())
                .doesNotContain(uno.id());
        assertThat(cajaService.comprobantes(hoy, hoy, uno.codigo().toLowerCase(), null))
                .extracting(ComprobanteResponse::id)
                .containsExactly(uno.id());
        assertThat(cajaService.obtener(uno.id()).items()).hasSize(1);

        ReporteIngresosResponse reporte = cajaService.reporteIngresos(hoy, hoy);
        assertThat(reporte.total()).isEqualByComparingTo("30.00");
        assertThat(reporte.porDia())
                .singleElement()
                .satisfies(d -> assertThat(d.comprobantes()).isEqualTo(1));
        assertThat(reporte.porMetodo())
                .singleElement()
                .satisfies(m -> assertThat(m.metodo()).isEqualTo(MetodoPagoCaja.EFECTIVO));
        assertThat(reporte.porOrigen())
                .singleElement()
                .satisfies(o -> assertThat(o.origen()).isEqualTo(OrigenComprobante.SERVICIO));
        assertThatThrownBy(() -> cajaService.reporteIngresos(hoy.minusDays(400), hoy))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> cajaService.reporteIngresos(hoy, hoy.minusDays(1)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void elHistorialMuestraAlCajeroSoloSusSesionesYAlAdministradorTodas() {
        abrir(cajero, "10.00");
        abrir(otroCajero, "20.00");

        assertThat(cajaService.historial(cajero))
                .hasSize(1)
                .allSatisfy(s -> assertThat(s.cajeroId()).isEqualTo(cajero.getId()));
        assertThat(cajaService.historial(admin))
                .extracting(SesionCajaResponse::cajeroId)
                .contains(cajero.getId(), otroCajero.getId());
    }

    private CobroRequest conDocumento(
            CobroRequest base, TipoComprobante tipo, TipoDocumentoCliente tipoDoc, String doc) {
        return new CobroRequest(
                base.origen(),
                base.referenciaId(),
                base.pacienteId(),
                tipo,
                tipoDoc,
                doc,
                base.clienteNombre(),
                base.metodoPago(),
                base.items());
    }
}
