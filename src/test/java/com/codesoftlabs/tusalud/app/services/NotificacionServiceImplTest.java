package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CitaCreateRequest;
import com.codesoftlabs.tusalud.app.dto.response.CitaResponse;
import com.codesoftlabs.tusalud.app.dto.response.NotificacionResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.services.interfaces.CitaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
class NotificacionServiceImplTest {

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico medico;
    private Paciente paciente;

    private void prepararMedicoYPaciente() {
        medico = fixture.medicoConHorarioLunes(new BigDecimal("100.00"));
        paciente = fixture.paciente();
    }

    private static LocalDate proximoLunes() {
        return LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private List<NotificacionResponse> notificacionesDe(Usuario usuario) {
        return notificacionService.listar(fixture.principal(usuario), false, 100);
    }

    @Test
    void crearCitaNotificaAlMedicoYAlPaciente() {
        prepararMedicoYPaciente();

        CitaResponse cita = citaService.crearCita(
                new CitaCreateRequest(
                        medico.getId(),
                        proximoLunes().atTime(9, 0),
                        "Control general",
                        medico.getEspecialidades().iterator().next().getId()),
                fixture.principal(paciente.getUsuario()));

        assertThat(notificacionesDe(medico.getUsuario())).singleElement().satisfies(n -> {
            assertThat(n.tipo()).isEqualTo(TipoNotificacion.CITA_CREADA);
            assertThat(n.enlace()).isEqualTo("/citas/" + cita.id());
            assertThat(n.leida()).isFalse();
        });
        assertThat(notificacionesDe(paciente.getUsuario()))
                .singleElement()
                .satisfies(n -> assertThat(n.tipo()).isEqualTo(TipoNotificacion.CITA_CREADA));
    }

    @Test
    void confirmarCitaNotificaAlPaciente() {
        prepararMedicoYPaciente();
        CitaResponse cita = citaService.crearCita(
                new CitaCreateRequest(
                        medico.getId(),
                        proximoLunes().atTime(10, 0),
                        "Dolor de cabeza",
                        medico.getEspecialidades().iterator().next().getId()),
                fixture.principal(paciente.getUsuario()));

        citaService.confirmarCita(cita.id(), fixture.principal(medico.getUsuario()));

        assertThat(notificacionesDe(paciente.getUsuario()))
                .extracting(NotificacionResponse::tipo)
                .containsExactly(TipoNotificacion.CITA_CONFIRMADA, TipoNotificacion.CITA_CREADA);
    }

    @Test
    void cancelarCitaNotificaAlMedico() {
        prepararMedicoYPaciente();
        CitaResponse cita = citaService.crearCita(
                new CitaCreateRequest(
                        medico.getId(),
                        proximoLunes().atTime(11, 0),
                        "Revisión",
                        medico.getEspecialidades().iterator().next().getId()),
                fixture.principal(paciente.getUsuario()));

        citaService.cancelarCita(cita.id(), fixture.principal(paciente.getUsuario()));

        assertThat(notificacionesDe(medico.getUsuario()))
                .extracting(NotificacionResponse::tipo)
                .containsExactly(TipoNotificacion.CITA_CANCELADA, TipoNotificacion.CITA_CREADA);
    }

    @Test
    void listarDevuelveLasMasNuevasPrimero() {
        Usuario usuario = fixture.usuario("notif", RolNombre.ROLE_PACIENTE);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Primera", "Mensaje 1", null);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Segunda", "Mensaje 2", null);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Tercera", "Mensaje 3", "/perfil");

        assertThat(notificacionesDe(usuario))
                .extracting(NotificacionResponse::titulo)
                .containsExactly("Tercera", "Segunda", "Primera");
        assertThat(notificacionService.listar(fixture.principal(usuario), false, 2))
                .hasSize(2);
    }

    @Test
    void soloNoLeidasExcluyeLasLeidas() {
        Usuario usuario = fixture.usuario("leida", RolNombre.ROLE_PACIENTE);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Uno", "Mensaje", null);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Dos", "Mensaje", null);
        NotificacionResponse primera = notificacionesDe(usuario).get(1);

        NotificacionResponse marcada = notificacionService.marcarLeida(fixture.principal(usuario), primera.id());

        assertThat(marcada.leida()).isTrue();
        assertThat(notificacionService.listar(fixture.principal(usuario), true, 50))
                .extracting(NotificacionResponse::titulo)
                .containsExactly("Dos");
        assertThat(notificacionService.resumen(fixture.principal(usuario)).noLeidas())
                .isEqualTo(1);
    }

    @Test
    void marcarLeidaDeOtroUsuarioLanzaNotFound() {
        Usuario dueno = fixture.usuario("dueno", RolNombre.ROLE_PACIENTE);
        Usuario otro = fixture.usuario("otro", RolNombre.ROLE_PACIENTE);
        notificacionService.crear(dueno, TipoNotificacion.SISTEMA, "Privada", "Mensaje", null);
        Long id = notificacionesDe(dueno).get(0).id();

        assertThatThrownBy(() -> notificacionService.marcarLeida(fixture.principal(otro), id))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> notificacionService.eliminar(fixture.principal(otro), id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarTodasLeidasYEliminarActualizanLaBandeja() {
        Usuario usuario = fixture.usuario("todas", RolNombre.ROLE_PACIENTE);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Uno", "Mensaje", null);
        notificacionService.crear(usuario, TipoNotificacion.SISTEMA, "Dos", "Mensaje", null);

        notificacionService.marcarTodasLeidas(fixture.principal(usuario));
        assertThat(notificacionService.resumen(fixture.principal(usuario)).noLeidas())
                .isZero();

        notificacionService.eliminar(
                fixture.principal(usuario), notificacionesDe(usuario).get(0).id());
        assertThat(notificacionesDe(usuario)).hasSize(1);
    }
}
