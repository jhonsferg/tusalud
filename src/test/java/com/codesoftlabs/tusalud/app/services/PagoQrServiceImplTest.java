package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.config.PagosProperties;
import com.codesoftlabs.tusalud.app.dto.request.CuponRequest;
import com.codesoftlabs.tusalud.app.dto.request.PagoQrRequest;
import com.codesoftlabs.tusalud.app.dto.response.PagoQrResponse;
import com.codesoftlabs.tusalud.app.dto.response.PagoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.Billetera;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.IntencionPagoRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.impl.PagoQrServiceImpl;
import com.codesoftlabs.tusalud.app.services.interfaces.CuponService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoQrService;
import com.codesoftlabs.tusalud.app.services.interfaces.PagoService;
import com.codesoftlabs.tusalud.app.services.pagos.ProveedorBilletera;
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
class PagoQrServiceImplTest {

    @Autowired
    private PagoQrService pagoQrService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private CuponService cuponService;

    @Autowired
    private IntencionPagoRepository intencionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private List<ProveedorBilletera> proveedores;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principal;

    @BeforeEach
    void prepararDatos() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("150.00"));
        paciente = fixture.paciente();
        principal = fixture.principal(paciente.getUsuario());
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

    @Test
    void generaElQrDeYapeConElMontoDeLaEspecialidad() {
        Cita cita = citaPendiente();

        PagoQrResponse qr = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal);

        assertThat(qr.estado()).isEqualTo(EstadoIntencionPago.PENDIENTE);
        assertThat(qr.total()).isEqualByComparingTo("150.00");
        assertThat(qr.descuento()).isEqualByComparingTo("0.00");
        assertThat(qr.qrPayload())
                .startsWith("tusalud-pay://yape?codigo=" + qr.codigo())
                .contains("monto=150.00")
                .contains("ref=CITA-" + cita.getId());
        assertThat(qr.expiraEn()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(qr.pagoId()).isNull();
    }

    @Test
    void confirmaElPagoConPlinYAplicaElCupon() {
        String codigo = "QR" + ServiceTestFixture.siguiente();
        cuponService.crear(new CuponRequest(
                codigo,
                "Prueba",
                TipoCupon.PORCENTAJE,
                new BigDecimal("20"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30),
                null,
                true));
        Cita cita = citaPendiente();
        PagoQrResponse qr = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.PLIN, codigo), principal);
        assertThat(qr.descuento()).isEqualByComparingTo("30.00");
        assertThat(qr.total()).isEqualByComparingTo("120.00");

        PagoQrResponse confirmado = pagoQrService.simularConfirmacion(qr.codigo(), principal);

        assertThat(confirmado.estado()).isEqualTo(EstadoIntencionPago.CONFIRMADO);
        assertThat(confirmado.pagoId()).isNotNull();
        PagoResponse pago = pagoService.obtenerMiPago(principal, confirmado.pagoId());
        assertThat(pago.metodo()).isEqualTo("Plin");
        assertThat(pago.total()).isEqualByComparingTo("120.00");
        assertThat(pago.codigoCupon()).isEqualTo(codigo);
        assertThat(pagoQrService.simularConfirmacion(qr.codigo(), principal).estado())
                .isEqualTo(EstadoIntencionPago.CONFIRMADO);
        assertThatThrownBy(() -> pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rechazaCuponInvalidoYCitasAjenas() {
        Cita cita = citaPendiente();

        assertThatThrownBy(() ->
                        pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, "NOEXISTE"), principal))
                .isInstanceOf(BusinessLogicException.class);
        UsuarioPrincipal otro = fixture.principal(fixture.paciente().getUsuario());
        assertThatThrownBy(() -> pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), otro))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> pagoQrService.crear(new PagoQrRequest(-1L, Billetera.YAPE, null), principal))
                .isInstanceOf(ResourceNotFoundException.class);
        Cita cancelada = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(10), EstadoCita.CANCELADA);
        assertThatThrownBy(() ->
                        pagoQrService.crear(new PagoQrRequest(cancelada.getId(), Billetera.PLIN, null), principal))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void marcaComoExpiradoYNoPermiteConfirmarVencidos() {
        Cita cita = citaPendiente();
        PagoQrResponse qr = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal);
        IntencionPago intencion = intencionRepository
                .findByCodigoAndUsuarioId(qr.codigo(), principal.getId())
                .orElseThrow();
        intencion.setExpiraEn(LocalDateTime.now().minusSeconds(1));

        assertThat(pagoQrService.obtener(qr.codigo(), principal).estado()).isEqualTo(EstadoIntencionPago.EXPIRADO);
        assertThatThrownBy(() -> pagoQrService.simularConfirmacion(qr.codigo(), principal))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void cancelaYUnNuevoQrReemplazaAlPendiente() {
        Cita cita = citaPendiente();
        PagoQrResponse primero = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal);
        PagoQrResponse segundo = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.PLIN, null), principal);

        assertThat(pagoQrService.obtener(primero.codigo(), principal).estado())
                .isEqualTo(EstadoIntencionPago.CANCELADO);
        assertThat(pagoQrService.cancelar(segundo.codigo(), principal).estado())
                .isEqualTo(EstadoIntencionPago.CANCELADO);
        assertThatThrownBy(() -> pagoQrService.simularConfirmacion(segundo.codigo(), principal))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void soloElDuenoPuedeConsultarYCancelarElPagoConfirmadoNoSePuedeCancelar() {
        Cita cita = citaPendiente();
        PagoQrResponse qr = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal);
        UsuarioPrincipal otro = fixture.principal(fixture.paciente().getUsuario());

        assertThatThrownBy(() -> pagoQrService.obtener(qr.codigo(), otro))
                .isInstanceOf(ResourceNotFoundException.class);
        pagoQrService.simularConfirmacion(qr.codigo(), principal);
        assertThatThrownBy(() -> pagoQrService.cancelar(qr.codigo(), principal)).isInstanceOf(ConflictException.class);
    }

    @Test
    void sinSimulacionHabilitadaNoSeConfirma() {
        Cita cita = citaPendiente();
        PagoQrResponse qr = pagoQrService.crear(new PagoQrRequest(cita.getId(), Billetera.YAPE, null), principal);
        PagoQrServiceImpl sinSimulacion = new PagoQrServiceImpl(
                null, intencionRepository, null, cuponService, pagoService, new PagosProperties(false, 5), proveedores);

        assertThatThrownBy(() -> sinSimulacion.simularConfirmacion(qr.codigo(), principal))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
