package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPago;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.MetodoPagoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import com.codesoftlabs.tusalud.app.services.interfaces.MetodoPagoService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
class PagoServiceImplTest {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private CuponService cuponService;

    @Autowired
    private MetodoPagoService metodoPagoService;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principal;
    private MetodoPagoResponse metodo;

    @BeforeEach
    void prepararDatos() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("150.00"));
        paciente = fixture.paciente();
        principal = fixture.principal(paciente.getUsuario());
        metodo = metodoPagoService.crear(
                principal,
                new MetodoPagoRequest(
                        TipoMetodoPago.TARJETA_CREDITO,
                        "JUAN PEREZ",
                        "4111111111111111",
                        12,
                        YearMonth.now().plusYears(2).getYear(),
                        "123",
                        null));
    }

    private Cita citaPendiente() {
        return fixture.cita(
                medico,
                paciente,
                LocalDateTime.now()
                        .plusDays(3)
                        .withHour(10)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0),
                EstadoCita.PENDIENTE);
    }

    private static String codigoUnico() {
        return "PAG" + ServiceTestFixture.siguiente();
    }

    @Test
    void pagarCitaRegistraElPagoConComprobanteYNotifica() {
        Cita cita = citaPendiente();

        PagoResponse pago = pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal);

        assertThat(pago.estado()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pago.monto()).isEqualByComparingTo("150.00");
        assertThat(pago.descuento()).isEqualByComparingTo("0.00");
        assertThat(pago.total()).isEqualByComparingTo("150.00");
        assertThat(pago.comprobante()).matches("B001-\\d{6}");
        assertThat(pago.metodo()).isEqualTo("VISA terminada en 1111");
        assertThat(pago.citaId()).isEqualTo(cita.getId());
        assertThat(pago.medicoNombre()).isEqualTo(medico.getUsuario().getNombreCompleto());
        assertThat(notificacionService.listar(principal, false, 10))
                .extracting(NotificacionResponse::tipo)
                .contains(TipoNotificacion.PAGO_REGISTRADO);
        CitaResponse citaConPago = citaService.obtenerCita(cita.getId(), principal);
        assertThat(citaConPago.pagado()).isTrue();
    }

    @Test
    void pagarConCuponAplicaElDescuentoYConsumeElCupon() {
        String codigo = codigoUnico();
        cuponService.crear(new CuponRequest(
                codigo,
                "Prueba",
                TipoCupon.PORCENTAJE,
                new BigDecimal("10"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30),
                null,
                true));
        Cita cita = citaPendiente();

        PagoResponse pago = pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), codigo), principal);

        assertThat(pago.descuento()).isEqualByComparingTo("15.00");
        assertThat(pago.total()).isEqualByComparingTo("135.00");
        assertThat(pago.codigoCupon()).isEqualTo(codigo);
        assertThat(cuponService.validar(codigo, BigDecimal.TEN).valido()).isTrue();
        assertThat(cuponService.listar())
                .filteredOn(c -> c.codigo().equals(codigo))
                .singleElement()
                .satisfies(c -> assertThat(c.usosActuales()).isEqualTo(1));
    }

    @Test
    void cuponNoValidoRechazaElPago() {
        Cita cita = citaPendiente();

        assertThatThrownBy(() -> pagoService.pagarCita(
                        cita.getId(), new PagoRequest(metodo.id(), "NOEXISTE" + codigoUnico()), principal))
                .isInstanceOf(BusinessLogicException.class);
        assertThat(citaService.obtenerCita(cita.getId(), principal).pagado()).isFalse();
    }

    @Test
    void pagarDosVecesLanzaConflict() {
        Cita cita = citaPendiente();
        pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal);

        assertThatThrownBy(() -> pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void pagarCitaDeOtroPacienteLanzaForbidden() {
        Cita cita = citaPendiente();
        UsuarioPrincipal otro = fixture.principal(fixture.paciente().getUsuario());

        assertThatThrownBy(() -> pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), otro))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void citaAtendidaNoSePuedePagar() {
        Cita cita = fixture.cita(medico, paciente, LocalDateTime.now().minusDays(2), EstadoCita.ATENDIDA);

        assertThatThrownBy(() -> pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("ATENDIDA");
    }

    @Test
    void cancelarCitaPagadaReembolsaElPagoYNotifica() {
        Cita cita = citaPendiente();
        PagoResponse pago = pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal);

        citaService.cancelarCita(cita.getId(), principal);

        assertThat(pagoService.obtenerMiPago(principal, pago.id()).estado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(citaService.obtenerCita(cita.getId(), principal).pagado()).isFalse();
        assertThat(notificacionService.listar(principal, false, 20))
                .extracting(NotificacionResponse::tipo)
                .contains(TipoNotificacion.PAGO_REEMBOLSADO);
    }

    @Test
    void listarMisPagosYObtenerSoloDelPropietario() {
        Cita cita = citaPendiente();
        PagoResponse pago = pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal);
        UsuarioPrincipal otro = fixture.principal(fixture.paciente().getUsuario());

        assertThat(pagoService.listarMisPagos(principal))
                .singleElement()
                .satisfies(p -> assertThat(p.id()).isEqualTo(pago.id()));
        assertThat(pagoService.listarMisPagos(otro)).isEmpty();
        assertThatThrownBy(() -> pagoService.obtenerMiPago(otro, pago.id()))
                .isInstanceOf(com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException.class);
    }

    @Test
    void listarTodosFiltraPorFechasDePago() {
        Cita cita = citaPendiente();
        PagoResponse pago = pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal);
        LocalDate hoy = LocalDate.now();

        assertThat(pagoService.listarTodos(hoy.minusDays(1), hoy.plusDays(1)))
                .extracting(PagoResponse::id)
                .contains(pago.id());
        assertThat(pagoService.listarTodos(hoy.minusYears(3), hoy.minusYears(2)))
                .isEmpty();
    }

    @Test
    void metodoVencidoNoPuedeUsarseParaPagar() {
        Cita cita = citaPendiente();
        MetodoPago guardado = metodoPagoRepository
                .findByIdAndUsuarioId(metodo.id(), principal.getId())
                .orElseThrow();
        guardado.setVencimientoAnio(2020);
        metodoPagoRepository.saveAndFlush(guardado);

        assertThatThrownBy(() -> pagoService.pagarCita(cita.getId(), new PagoRequest(metodo.id(), null), principal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("vencido");
    }
}
