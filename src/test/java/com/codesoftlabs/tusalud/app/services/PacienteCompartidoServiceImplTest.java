package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.dto.request.CompartirPacienteRequest;
import com.codesoftlabs.tusalud.app.dto.response.PacienteCompartidoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PacienteMedicoResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.NotificacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HistoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.PacienteCompartidoService;
import java.math.BigDecimal;
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
class PacienteCompartidoServiceImplTest {

    @Autowired
    private PacienteCompartidoService compartidoService;

    @Autowired
    private HistoriaClinicaService historiaService;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private ServiceTestFixture fixture;

    private Medico origen;
    private Medico destino;
    private Paciente paciente;
    private UsuarioPrincipal principalOrigen;
    private UsuarioPrincipal principalDestino;

    @BeforeEach
    void prepararDatos() {
        origen = fixture.medicoConHorarioLunes(BigDecimal.TEN);
        destino = fixture.medicoConHorarioLunes(BigDecimal.TEN);
        paciente = fixture.paciente();
        fixture.cita(origen, paciente, LocalDateTime.now().plusDays(2), EstadoCita.PENDIENTE);
        principalOrigen = fixture.principal(origen.getUsuario());
        principalDestino = fixture.principal(destino.getUsuario());
    }

    @Test
    void elMedicoDestinoVeAlPacienteYSuHistoriaTrasCompartir() {
        assertThatThrownBy(() -> historiaService.obtenerDePaciente(paciente.getId(), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);

        PacienteCompartidoResponse respuesta = compartidoService.compartir(
                paciente.getId(),
                new CompartirPacienteRequest(destino.getId(), "  Segunda opinión  "),
                principalOrigen);

        assertThat(respuesta.medicoId()).isEqualTo(destino.getId());
        assertThat(respuesta.nota()).isEqualTo("Segunda opinión");
        assertThat(respuesta.especialidades()).hasSize(1);
        assertThat(historiaService.obtenerDePaciente(paciente.getId(), principalDestino))
                .isNotNull();
        List<PacienteMedicoResponse> pacientes = historiaService.listarPacientesDeMedico(principalDestino);
        assertThat(pacientes).hasSize(1);
        assertThat(pacientes.get(0).pacienteId()).isEqualTo(paciente.getId());
        assertThat(pacientes.get(0).compartidoPor())
                .isEqualTo(origen.getUsuario().getNombreCompleto());
        assertThat(historiaService
                        .listarPacientesDeMedico(principalOrigen)
                        .get(0)
                        .compartidoPor())
                .isNull();
        assertThat(compartidoService.listar(paciente.getId(), principalDestino)).hasSize(1);
        assertThat(notificacionRepository.findAll())
                .anyMatch(
                        n -> n.getUsuario().getId().equals(destino.getUsuario().getId())
                                && n.getTipo() == TipoNotificacion.PACIENTE_COMPARTIDO);
    }

    @Test
    void validaQuienComparteConQuienYSinDuplicados() {
        Medico ajeno = fixture.medicoConHorarioLunes(BigDecimal.TEN);

        assertThatThrownBy(() -> compartidoService.compartir(
                        paciente.getId(), new CompartirPacienteRequest(destino.getId(), null), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> compartidoService.compartir(
                        paciente.getId(), new CompartirPacienteRequest(origen.getId(), null), principalOrigen))
                .isInstanceOf(BusinessLogicException.class);
        assertThatThrownBy(() -> compartidoService.compartir(
                        paciente.getId(), new CompartirPacienteRequest(-1L, null), principalOrigen))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> compartidoService.compartir(
                        -1L, new CompartirPacienteRequest(destino.getId(), null), principalOrigen))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> compartidoService.compartir(
                        paciente.getId(),
                        new CompartirPacienteRequest(ajeno.getId(), null),
                        fixture.principal(fixture.usuario("sinperfil", RolNombre.ROLE_MEDICO))))
                .isInstanceOf(ForbiddenOperationException.class);

        compartidoService.compartir(
                paciente.getId(), new CompartirPacienteRequest(destino.getId(), null), principalOrigen);
        assertThatThrownBy(() -> compartidoService.compartir(
                        paciente.getId(), new CompartirPacienteRequest(destino.getId(), null), principalOrigen))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void soloElOrigenOUnAdministradorRevocanElAcceso() {
        compartidoService.compartir(
                paciente.getId(), new CompartirPacienteRequest(destino.getId(), null), principalOrigen);

        assertThatThrownBy(() -> compartidoService.revocar(paciente.getId(), destino.getId(), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);
        Usuario admin = fixture.usuario("admin", RolNombre.ROLE_ADMIN);
        compartidoService.revocar(paciente.getId(), destino.getId(), fixture.principal(admin));

        assertThat(compartidoService.listar(paciente.getId(), principalOrigen)).isEmpty();
        assertThatThrownBy(() -> historiaService.obtenerDePaciente(paciente.getId(), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> compartidoService.revocar(paciente.getId(), destino.getId(), principalOrigen))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarExigeAccesoAlPaciente() {
        assertThatThrownBy(() -> compartidoService.listar(paciente.getId(), principalDestino))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> compartidoService.listar(-1L, principalOrigen))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(compartidoService.listar(
                        paciente.getId(), fixture.principal(fixture.usuario("admin", RolNombre.ROLE_ADMIN))))
                .isEmpty();
    }
}
