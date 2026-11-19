package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.Cie10Pruebas;
import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Dispensacion;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.DispensacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
class AtencionServiceImplTest {

    @Autowired
    private AtencionService atencionService;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private AtencionRepository atencionRepository;

    @Autowired
    private DispensacionRepository dispensacionRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;
    private UsuarioPrincipal principalMedico;
    private UsuarioPrincipal principalPaciente;
    private Cita cita;

    private void prepararCita(EstadoCita estado) {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("120.00"));
        paciente = fixture.paciente();
        principalMedico = fixture.principal(medico.getUsuario());
        principalPaciente = fixture.principal(paciente.getUsuario());
        cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2).withNano(0), estado);
    }

    private static AtencionRequest atencion(String diagnostico) {
        return new AtencionRequest(
                diagnostico,
                "Dieta baja en sal",
                "Control en 30 días",
                null,
                new BigDecimal("72.5"),
                new BigDecimal("170.0"),
                "120/80",
                new BigDecimal("36.5"),
                72,
                List.of(
                        new RecetaItemRequest("Losartán 50 mg", "1 tableta", "Cada 24 horas", "30 días"),
                        new RecetaItemRequest("Aspirina 100 mg", "1 tableta", "Cada 24 horas", null)),
                Cie10Pruebas.principal());
    }

    @Test
    void registrarAtencionMarcaLaCitaComoAtendidaYNotificaAlPaciente() {
        prepararCita(EstadoCita.CONFIRMADA);

        AtencionResponse respuesta = atencionService.registrar(cita.getId(), atencion("Hipertensión"), principalMedico);

        assertThat(respuesta.citaId()).isEqualTo(cita.getId());
        assertThat(respuesta.pacienteId()).isEqualTo(paciente.getId());
        assertThat(respuesta.medicoNombre()).isEqualTo(medico.getUsuario().getNombreCompleto());
        assertThat(respuesta.receta()).hasSize(2);
        assertThat(respuesta.receta().get(0).medicamento()).isEqualTo("Losartán 50 mg");
        assertThat(citaRepository.findById(cita.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoCita.ATENDIDA);
        assertThat(notificacionService.listar(principalPaciente, false, 10))
                .extracting(NotificacionResponse::tipo)
                .contains(TipoNotificacion.CITA_ATENDIDA);
    }

    @Test
    void registrarDosVecesLanzaConflict() {
        prepararCita(EstadoCita.PENDIENTE);
        atencionService.registrar(cita.getId(), atencion("Primera"), principalMedico);

        assertThatThrownBy(() -> atencionService.registrar(cita.getId(), atencion("Segunda"), principalMedico))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void medicoAjenoNoPuedeRegistrarLaAtencion() {
        prepararCita(EstadoCita.CONFIRMADA);
        UsuarioPrincipal otroMedico = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("90.00")).getUsuario());

        assertThatThrownBy(() -> atencionService.registrar(cita.getId(), atencion("Ajena"), otroMedico))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void citaCanceladaNoAceptaAtencion() {
        prepararCita(EstadoCita.CANCELADA);

        assertThatThrownBy(() -> atencionService.registrar(cita.getId(), atencion("Cancelada"), principalMedico))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("CANCELADA");
    }

    @Test
    void citaAtendidaSinAtencionPuedeRegistrarla() {
        prepararCita(EstadoCita.ATENDIDA);

        AtencionResponse respuesta = atencionService.registrar(cita.getId(), atencion("Tardía"), principalMedico);

        assertThat(respuesta.diagnostico()).isEqualTo("Tardía");
    }

    @Test
    void obtenerPermiteAlPacienteDuenoYNiegaAOtrosPacientes() {
        prepararCita(EstadoCita.CONFIRMADA);
        atencionService.registrar(cita.getId(), atencion("Visible"), principalMedico);
        UsuarioPrincipal otroPaciente = fixture.principal(fixture.paciente().getUsuario());

        assertThat(atencionService.obtener(cita.getId(), principalPaciente).diagnostico())
                .isEqualTo("Visible");
        assertThat(atencionService.obtener(cita.getId(), principalMedico).diagnostico())
                .isEqualTo("Visible");
        assertThatThrownBy(() -> atencionService.obtener(cita.getId(), otroPaciente))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void obtenerSinAtencionLanzaNotFound() {
        prepararCita(EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> atencionService.obtener(cita.getId(), principalMedico))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarEditaLaAtencionSoloParaElMedicoDeLaCita() {
        prepararCita(EstadoCita.CONFIRMADA);
        atencionService.registrar(cita.getId(), atencion("Inicial"), principalMedico);

        AtencionResponse editada = atencionService.actualizar(cita.getId(), atencion("Corregida"), principalMedico);

        assertThat(editada.diagnostico()).isEqualTo("Corregida");
        assertThat(atencionService.obtener(cita.getId(), principalMedico).diagnostico())
                .isEqualTo("Corregida");
        assertThatThrownBy(() -> atencionService.actualizar(
                        cita.getId(), atencion("Ajena"), fixture.principal(paciente.getUsuario())))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void citaInexistenteLanzaNotFound() {
        prepararCita(EstadoCita.CONFIRMADA);

        assertThatThrownBy(() -> atencionService.obtener(UUID.randomUUID(), principalMedico))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void laAtencionYaNoSeModificaPasadasCuarentaYOchoHoras() {
        prepararCita(EstadoCita.CONFIRMADA);
        AtencionResponse creada = atencionService.registrar(cita.getId(), atencion("Hipertensión"), principalMedico);
        Atencion guardada = atencionRepository.findById(creada.id()).orElseThrow();
        guardada.setFecha(LocalDateTime.now().minusHours(49));
        atencionRepository.saveAndFlush(guardada);

        assertThatThrownBy(() -> atencionService.actualizar(cita.getId(), atencion("Falsificado"), principalMedico))
                .isInstanceOf(ConflictException.class);
        assertThat(atencionService.obtener(cita.getId(), principalPaciente).diagnostico())
                .isEqualTo("Hipertensión");
    }

    @Test
    void laAtencionConRecetaDispensadaNoSePuedeModificar() {
        prepararCita(EstadoCita.CONFIRMADA);
        AtencionResponse creada = atencionService.registrar(cita.getId(), atencion("Hipertensión"), principalMedico);
        Atencion guardada = atencionRepository.findById(creada.id()).orElseThrow();
        dispensacionRepository.save(Dispensacion.builder()
                .fecha(LocalDateTime.now())
                .comprobante("F001-" + ServiceTestFixture.siguiente())
                .estado(EstadoDispensacion.EMITIDA)
                .paciente(paciente)
                .atencion(guardada)
                .usuario(medico.getUsuario())
                .metodoPago(MetodoPagoFarmacia.EFECTIVO)
                .total(BigDecimal.TEN)
                .build());

        assertThatThrownBy(() -> atencionService.actualizar(cita.getId(), atencion("Cambiada"), principalMedico))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void elAdministradorYUnMedicoAjenoNoLeenLaAtencion() {
        prepararCita(EstadoCita.CONFIRMADA);
        atencionService.registrar(cita.getId(), atencion("Hipertensión"), principalMedico);
        UsuarioPrincipal administrador = fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN));
        UsuarioPrincipal ajeno = fixture.principal(
                fixture.medicoConHorarioLunes(new BigDecimal("100.00")).getUsuario());

        assertThatThrownBy(() -> atencionService.obtener(cita.getId(), administrador))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> atencionService.obtener(cita.getId(), ajeno))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
