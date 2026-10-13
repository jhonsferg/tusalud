package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DisponibilidadResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaService;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
class CitaServiceImplTest {

    private static final BigDecimal PRECIO = new BigDecimal("150.00");

    @Autowired
    private CitaService citaService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal pacientePrincipal;
    private UsuarioPrincipal medicoPrincipal;
    private LocalDate proximoLunes;

    @BeforeEach
    void setUp() {
        medico = fixture.medicoConHorarioLunes(PRECIO);
        paciente = fixture.paciente();
        pacientePrincipal = fixture.principal(paciente.getUsuario());
        medicoPrincipal = fixture.principal(medico.getUsuario());
        proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private CitaCreateRequest solicitud(LocalDateTime fechaHora) {
        return new CitaCreateRequest(medico.getId(), fechaHora, "  Dolor de cabeza  ");
    }

    private long citasDelMedico() {
        return citaRepository
                .findAgendaMedicoPorFecha(medico.getId(), proximoLunes)
                .size();
    }

    @Test
    void crearCitaRegistraCitaPendienteConMontoDelMedico() {
        LocalDateTime fechaHora = proximoLunes.atTime(9, 0, 37);

        CitaResponse cita = citaService.crearCita(solicitud(fechaHora), pacientePrincipal);

        assertThat(cita.id()).isNotNull();
        assertThat(cita.estado()).isEqualTo(EstadoCita.PENDIENTE);
        assertThat(cita.monto()).isEqualByComparingTo(PRECIO);
        assertThat(cita.fechaHora()).isEqualTo(proximoLunes.atTime(9, 0));
        assertThat(cita.motivo()).isEqualTo("Dolor de cabeza");
        assertThat(cita.medicoId()).isEqualTo(medico.getId());
        assertThat(cita.pacienteId()).isEqualTo(paciente.getId());
        assertThat(citaRepository.findById(cita.id())).isPresent();
    }

    @Test
    void crearCitaEnDiaSinHorarioLanzaBusinessLogicException() {
        LocalDateTime martes = proximoLunes.plusDays(1).atTime(9, 0);

        assertThatThrownBy(() -> citaService.crearCita(solicitud(martes), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class)
                .isNotInstanceOf(ConflictException.class)
                .hasMessageContaining("no tiene horario de atención el día martes");
    }

    @Test
    void crearCitaFueraDelBloqueLanzaBusinessLogicException() {
        assertThatThrownBy(() -> citaService.crearCita(solicitud(proximoLunes.atTime(12, 0)), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("08:00-12:00");
        assertThatThrownBy(() -> citaService.crearCita(solicitud(proximoLunes.atTime(11, 45)), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void crearCitaNoAlineadaConLaDuracionLanzaBusinessLogicException() {
        assertThatThrownBy(() -> citaService.crearCita(solicitud(proximoLunes.atTime(9, 10)), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("09:10");
    }

    @Test
    void crearCitaEnFechaPasadaLanzaBusinessLogicException() {
        LocalDateTime lunesPasado = LocalDate.now()
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                .atTime(9, 0);

        assertThatThrownBy(() -> citaService.crearCita(solicitud(lunesPasado), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("futura");
    }

    @Test
    void crearCitaConMedicoInexistenteLanzaResourceNotFound() {
        CitaCreateRequest request = new CitaCreateRequest(-1L, proximoLunes.atTime(9, 0), "Control");

        assertThatThrownBy(() -> citaService.crearCita(request, pacientePrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crearCitaConSolapamientoDelMedicoLanzaConflictYNoPersiste() {
        Paciente otroPaciente = fixture.paciente();
        fixture.cita(medico, otroPaciente, proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);
        long antes = citasDelMedico();

        assertThatThrownBy(() -> citaService.crearCita(solicitud(proximoLunes.atTime(9, 0)), pacientePrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El médico ya tiene una cita registrada en ese horario");

        assertThat(citasDelMedico()).isEqualTo(antes);
    }

    @Test
    void crearCitaPermiteReservarHorarioDeCitaCancelada() {
        fixture.cita(medico, fixture.paciente(), proximoLunes.atTime(9, 0), EstadoCita.CANCELADA);

        CitaResponse cita = citaService.crearCita(solicitud(proximoLunes.atTime(9, 0)), pacientePrincipal);

        assertThat(cita.estado()).isEqualTo(EstadoCita.PENDIENTE);
    }

    @Test
    void crearCitaConSolapamientoDelPacienteLanzaConflict() {
        Medico otroMedico = fixture.medicoConHorarioLunes(new BigDecimal("80.00"));
        fixture.cita(otroMedico, paciente, proximoLunes.atTime(10, 0), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> citaService.crearCita(solicitud(proximoLunes.atTime(10, 0)), pacientePrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya tiene otra cita registrada que se cruza con ese horario");
    }

    @Test
    void cancelarCitaDeOtroPacienteLanzaForbidden() {
        Cita cita = fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);
        UsuarioPrincipal intruso = fixture.principal(fixture.paciente().getUsuario());

        assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), intruso))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> citaService.obtenerCita(cita.getId(), intruso))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void pacienteDuenoCancelaSuCita() {
        Cita cita = fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.CONFIRMADA);

        CitaResponse cancelada = citaService.cancelarCita(cita.getId(), pacientePrincipal);

        assertThat(cancelada.estado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void pacienteNoPuedeCancelarCitaPasadaPeroAdminSi() {
        LocalDateTime pasado = LocalDate.now()
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                .atTime(9, 0);
        Cita cita = fixture.cita(medico, paciente, pasado, EstadoCita.PENDIENTE);
        UsuarioPrincipal admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));

        assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class);
        assertThat(citaService.cancelarCita(cita.getId(), admin).estado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    void transicionesDeEstadoInvalidasLanzanBusinessLogicException() {
        Cita cancelada = fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.CANCELADA);
        Cita atendida = fixture.cita(medico, paciente, proximoLunes.atTime(10, 0), EstadoCita.ATENDIDA);
        Cita confirmada = fixture.cita(medico, paciente, proximoLunes.atTime(11, 0), EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> citaService.cancelarCita(cancelada.getId(), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("No se puede cancelar una cita en estado CANCELADA");
        assertThatThrownBy(() -> citaService.cancelarCita(atendida.getId(), pacientePrincipal))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> citaService.confirmarCita(confirmada.getId(), medicoPrincipal))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> citaService.atenderCita(cancelada.getId(), medicoPrincipal))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void flujoConfirmarYAtenderPorElMedico() {
        Cita cita = fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);

        assertThat(citaService.confirmarCita(cita.getId(), medicoPrincipal).estado())
                .isEqualTo(EstadoCita.CONFIRMADA);
        assertThat(citaService.atenderCita(cita.getId(), medicoPrincipal).estado())
                .isEqualTo(EstadoCita.ATENDIDA);
    }

    @Test
    void soloElMedicoDeLaCitaPuedeAtenderla() {
        Cita cita = fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);
        UsuarioPrincipal admin = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        UsuarioPrincipal otroMedico =
                fixture.principal(fixture.medicoConHorarioLunes(PRECIO).getUsuario());

        assertThatThrownBy(() -> citaService.atenderCita(cita.getId(), admin))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> citaService.confirmarCita(cita.getId(), otroMedico))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> citaService.atenderCita(cita.getId(), pacientePrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(citaService.obtenerCita(cita.getId(), admin).id()).isEqualTo(cita.getId());
        assertThat(citaService.obtenerCita(cita.getId(), medicoPrincipal).id()).isEqualTo(cita.getId());
    }

    @Test
    void disponibilidadExcluyeHorariosOcupados() {
        fixture.cita(medico, fixture.paciente(), proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, fixture.paciente(), proximoLunes.atTime(10, 0), EstadoCita.CANCELADA);

        DisponibilidadResponse disponibilidad = citaService.consultarDisponibilidad(medico.getId(), proximoLunes);

        assertThat(disponibilidad.medicoId()).isEqualTo(medico.getId());
        assertThat(disponibilidad.horariosDisponibles())
                .hasSize(7)
                .doesNotContain(LocalTime.of(9, 0))
                .contains(LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(11, 30))
                .doesNotContain(LocalTime.of(12, 0))
                .isSorted();
    }

    @Test
    void disponibilidadEnDiaSinHorarioEsVaciaYEnFechaPasadaFalla() {
        assertThat(citaService
                        .consultarDisponibilidad(medico.getId(), proximoLunes.plusDays(1))
                        .horariosDisponibles())
                .isEmpty();
        assertThatThrownBy(() -> citaService.consultarDisponibilidad(
                        medico.getId(), LocalDate.now().minusDays(1)))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void listarMisCitasYAgendaDelMedico() {
        citaService.crearCita(solicitud(proximoLunes.atTime(8, 30)), pacientePrincipal);
        citaService.crearCita(solicitud(proximoLunes.atTime(11, 0)), pacientePrincipal);

        List<CitaResponse> mias = citaService.listarMisCitas(pacientePrincipal, null, null);
        List<CitaResponse> agenda = citaService.agendaMedico(medicoPrincipal, proximoLunes);

        assertThat(mias)
                .extracting(CitaResponse::fechaHora)
                .containsExactly(proximoLunes.atTime(8, 30), proximoLunes.atTime(11, 0));
        assertThat(agenda).hasSize(2);
        assertThatThrownBy(() -> citaService.listarMisCitas(pacientePrincipal, proximoLunes, proximoLunes.minusDays(1)))
                .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void auditoriaCuentaPorEstadoYListaPorRango() {
        fixture.cita(medico, paciente, proximoLunes.atTime(9, 0), EstadoCita.PENDIENTE);
        fixture.cita(medico, paciente, proximoLunes.atTime(10, 0), EstadoCita.CANCELADA);

        List<CitasPorEstadoResponse> conteo = auditoriaService.contarCitasPorEstado();
        List<CitaResponse> citas = auditoriaService.listarCitas(proximoLunes, proximoLunes);

        assertThat(conteo)
                .extracting(CitasPorEstadoResponse::estado)
                .contains(EstadoCita.PENDIENTE, EstadoCita.CANCELADA);
        assertThat(citas).filteredOn(c -> c.medicoId().equals(medico.getId())).hasSize(2);
        assertThatThrownBy(() -> auditoriaService.listarCitas(proximoLunes, proximoLunes.minusDays(1)))
                .isInstanceOf(BusinessLogicException.class);
    }
}
