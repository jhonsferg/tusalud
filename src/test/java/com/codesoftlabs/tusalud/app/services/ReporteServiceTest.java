package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.AperturaCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CierreCajaRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.CobroRequest;
import com.codesoftlabs.tusalud.app.dto.response.ReporteCalidadResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteEjecutivoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteFinancieroResponse;
import com.codesoftlabs.tusalud.app.dto.response.ReporteOperativoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Pago;
import com.codesoftlabs.tusalud.app.entities.Valoracion;
import com.codesoftlabs.tusalud.app.entities.enums.AgrupacionReporte;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoCaja;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.repositories.ValoracionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CajaService;
import com.codesoftlabs.tusalud.app.services.interfaces.ReporteService;
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
class ReporteServiceTest {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ValoracionRepository valoracionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal cajero;
    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("150.00"));
        paciente = fixture.paciente();
        cajero = fixture.principal(fixture.usuario("caja", RolNombre.ROLE_CAJA));
        hoy = LocalDate.now();
    }

    private Cita citaAyer(int hora, EstadoCita estado) {
        return fixture.cita(medico, paciente, hoy.minusDays(1).atTime(hora, 0), estado);
    }

    private void pagoEnLinea(Cita cita, String metodo, String total) {
        pagoRepository.save(Pago.builder()
                .cita(cita)
                .metodoDescripcion(metodo)
                .monto(new BigDecimal(total))
                .descuento(BigDecimal.ZERO)
                .total(new BigDecimal(total))
                .estado(EstadoPago.PAGADO)
                .comprobante("B001-9" + ServiceTestFixture.siguiente())
                .fechaPago(LocalDateTime.now().minusHours(1))
                .build());
    }

    private void cobroLibre(String precio, int cantidad, MetodoPagoCaja metodo) {
        cajaService.cobrar(
                cajero,
                new CobroRequest(
                        OrigenComprobante.SERVICIO,
                        null,
                        null,
                        TipoComprobante.BOLETA,
                        TipoDocumentoCliente.SIN_DOC,
                        null,
                        "Cliente de mostrador",
                        metodo,
                        List.of(new CobroItemRequest("Curación", cantidad, new BigDecimal(precio)))));
    }

    @Test
    void elReporteEjecutivoCuentaCitasEIngresosYComparaConElPeriodoAnterior() {
        Cita atendida = citaAyer(9, EstadoCita.ATENDIDA);
        citaAyer(10, EstadoCita.CANCELADA);
        citaAyer(11, EstadoCita.CONFIRMADA);
        pagoEnLinea(atendida, "Yape", "150.00");
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        cobroLibre("59.00", 2, MetodoPagoCaja.EFECTIVO);

        ReporteEjecutivoResponse r = reporteService.ejecutivo(hoy.minusDays(6), hoy, AgrupacionReporte.DIA);

        assertThat(r.periodo().dias()).isEqualTo(7);
        assertThat(r.periodoAnterior().hasta()).isEqualTo(hoy.minusDays(7));
        assertThat(r.citas().actual()).isEqualByComparingTo("3");
        assertThat(r.atendidas().actual()).isEqualByComparingTo("1");
        assertThat(r.canceladas().actual()).isEqualByComparingTo("1");
        assertThat(r.ingresos().actual()).isEqualByComparingTo("268.00");
        assertThat(r.ingresos().anterior()).isEqualByComparingTo("0");
        assertThat(r.ingresos().variacionPct()).isNull();
        assertThat(r.ticketPromedio().actual()).isEqualByComparingTo("134.00");
        assertThat(r.tasaAusenciaPct().actual()).isEqualByComparingTo("50.00");
        assertThat(r.serie()).hasSize(7);
        assertThat(r.serie().stream()
                        .mapToLong(ReporteEjecutivoResponse.Punto::citas)
                        .sum())
                .isEqualTo(3);
    }

    @Test
    void elReporteFinancieroDesglosaPorMetodoOrigenEspecialidadYMedico() {
        Cita atendida = citaAyer(9, EstadoCita.ATENDIDA);
        pagoEnLinea(atendida, "Yape", "150.00");
        cajaService.abrir(cajero, new AperturaCajaRequest(new BigDecimal("100")));
        cobroLibre("59.00", 2, MetodoPagoCaja.EFECTIVO);
        cajaService.cerrar(cajero, new CierreCajaRequest(new BigDecimal("200.00"), null));

        ReporteFinancieroResponse r = reporteService.financiero(hoy.minusDays(6), hoy, AgrupacionReporte.DIA);

        assertThat(r.totalCobrado()).isEqualByComparingTo("268.00");
        assertThat(r.transacciones()).isEqualTo(2);
        assertThat(r.porMetodo())
                .extracting(
                        ReporteFinancieroResponse.Metodo::metodo, m -> m.total().intValue())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("YAPE", 150),
                        org.assertj.core.groups.Tuple.tuple("EFECTIVO", 118));
        assertThat(r.porOrigen())
                .filteredOn(o -> o.origen().equals("CONSULTA"))
                .singleElement()
                .satisfies(o -> assertThat(o.total()).isEqualByComparingTo("150.00"));
        assertThat(r.porOrigen())
                .filteredOn(o -> o.origen().equals("SERVICIOS"))
                .singleElement()
                .satisfies(o -> assertThat(o.total()).isEqualByComparingTo("118.00"));
        assertThat(r.porOrigen())
                .filteredOn(ReporteFinancieroResponse.Origen::devengado)
                .extracting(ReporteFinancieroResponse.Origen::origen)
                .containsExactly("HOSPITALIZACION");
        assertThat(r.porEspecialidad()).hasSize(1);
        assertThat(r.porEspecialidad().get(0).total()).isEqualByComparingTo("150.00");
        assertThat(r.porMedico()).hasSize(1);
        assertThat(r.comprobantes().emitidos()).isEqualTo(1);
        assertThat(r.comprobantes().anulados()).isZero();
        assertThat(r.cajas().sesionesCerradas()).isEqualTo(1);
        assertThat(r.cajas().neto()).isNotNull();
        assertThat(r.porCobrar().total()).isNotNull();
    }

    @Test
    void elReporteFinancieroAgrupaPorMes() {
        Cita atendida = citaAyer(9, EstadoCita.ATENDIDA);
        pagoEnLinea(atendida, "Visa", "150.00");

        ReporteFinancieroResponse r = reporteService.financiero(hoy.minusDays(40), hoy, AgrupacionReporte.MES);

        assertThat(r.porDia())
                .allSatisfy(d -> assertThat(d.fecha().getDayOfMonth()).isEqualTo(1));
        assertThat(r.porDia().stream()
                        .map(ReporteFinancieroResponse.Dia::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("150.00");
        assertThat(r.porMetodo())
                .extracting(ReporteFinancieroResponse.Metodo::metodo)
                .containsExactly("TARJETA");
    }

    @Test
    void elReporteOperativoCalculaDemandaAgendaEInventario() {
        citaAyer(9, EstadoCita.ATENDIDA);
        citaAyer(9, EstadoCita.CONFIRMADA);
        citaAyer(14, EstadoCita.PENDIENTE);

        ReporteOperativoResponse r = reporteService.operativo(hoy.minusDays(6), hoy, AgrupacionReporte.DIA);

        int dia = hoy.minusDays(1).getDayOfWeek().getValue();
        assertThat(r.demanda())
                .filteredOn(d -> d.diaSemana() == dia && d.hora() == 9)
                .singleElement()
                .satisfies(d -> assertThat(d.citas()).isEqualTo(2));
        assertThat(r.agenda()).hasSize(1);
        assertThat(r.agenda().get(0).citas()).isEqualTo(3);
        assertThat(r.agenda().get(0).capacidad()).isGreaterThan(0);
        assertThat(r.ocupacionSerie()).hasSize(7);
        assertThat(r.esperas().turnos()).isZero();
        assertThat(r.inventario()).isNotNull();
    }

    @Test
    void elReporteDeCalidadResumeValoracionesYCancelaciones() {
        Cita atendida = citaAyer(9, EstadoCita.ATENDIDA);
        citaAyer(10, EstadoCita.CANCELADA);
        valoracionRepository.save(Valoracion.builder()
                .cita(atendida)
                .medico(medico)
                .paciente(paciente)
                .puntuacion(4)
                .comentario("Buena atención")
                .build());

        ReporteCalidadResponse r = reporteService.calidad(hoy.minusDays(6), hoy, AgrupacionReporte.DIA);

        assertThat(r.valoraciones().total()).isEqualTo(1);
        assertThat(r.valoraciones().promedio()).isEqualByComparingTo("4.00");
        assertThat(r.valoraciones().distribucion()).containsExactly(0L, 0L, 0L, 1L, 0L);
        assertThat(r.porMedico()).hasSize(1);
        assertThat(r.porEspecialidad()).hasSize(1);
        assertThat(r.citasPorEspecialidad()).hasSize(1);
        assertThat(r.citasPorEspecialidad().get(0).canceladas()).isEqualTo(1);
        assertThat(r.citasPorEspecialidad().get(0).tasaCancelacionPct()).isEqualByComparingTo("50.00");
        assertThat(r.laboratorio().completadas()).isZero();
    }

    @Test
    void losReportesNoExponenDatosDePacientes() {
        citaAyer(9, EstadoCita.ATENDIDA);
        String texto = reporteService
                        .calidad(hoy.minusDays(6), hoy, AgrupacionReporte.DIA)
                        .toString()
                + reporteService.operativo(hoy.minusDays(6), hoy, AgrupacionReporte.DIA)
                + reporteService.financiero(hoy.minusDays(6), hoy, AgrupacionReporte.DIA)
                + reporteService.ejecutivo(hoy.minusDays(6), hoy, AgrupacionReporte.DIA);
        assertThat(texto)
                .doesNotContain(paciente.getDni())
                .doesNotContain(paciente.getUsuario().getEmail());
        assertThat(texto).doesNotContain(paciente.getUsuario().getNombre());
    }

    @Test
    void validaElRangoYUsaValoresPorDefecto() {
        assertThatThrownBy(() -> reporteService.ejecutivo(hoy, hoy.minusDays(1), null))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> reporteService.financiero(hoy.minusDays(366), hoy, null))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("366");
        assertThat(reporteService
                        .operativo(hoy.minusDays(365), hoy, null)
                        .periodo()
                        .dias())
                .isEqualTo(366);
        ReporteEjecutivoResponse porDefecto = reporteService.ejecutivo(null, null, null);
        assertThat(porDefecto.periodo().dias()).isEqualTo(30);
        assertThat(porDefecto.periodo().agrupacion()).isEqualTo(AgrupacionReporte.DIA);
    }
}
